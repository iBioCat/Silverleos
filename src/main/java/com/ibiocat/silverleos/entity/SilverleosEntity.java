package com.ibiocat.silverleos.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/// The Silverleos (Чешуйник): a slow, ancient cave dweller that wanders the dark.
///
/// Camouflage is computed on the server and synced as a 0..1 visibility value so
/// every client sees the same blend. The client maps that to an opaque skin made
/// of nearby block textures — not translucency.
public class SilverleosEntity extends PathfinderMob implements GeoEntity {
	private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");

	private static final EntityDataAccessor<Float> DATA_VISIBILITY =
			SynchedEntityData.defineId(SilverleosEntity.class, EntityDataSerializers.FLOAT);

	/// Floor of the synced visibility range. 0 means fully camouflaged.
	private static final float HIDDEN_VISIBILITY = 0.0F;
	private static final float VISIBILITY_STEP = 1.0F / 30.0F;
	private static final float PLAYER_REVEAL_RANGE = 5.0F;
	private static final int BLOCK_LIGHT_REVEAL = 8;
	private static final int REVEAL_AFTER_HIT_TICKS = 40;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	/// Previous tick's visibility, used only on the client to lerp with partial ticks.
	private float visibilityO = 1.0F;
	private int forceRevealTicks;

	public SilverleosEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 24.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.18D)
				.add(Attributes.FOLLOW_RANGE, 16.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VISIBILITY, 1.0F);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		this.visibilityO = getVisibility();
		super.tick();

		if (!this.level().isClientSide()) {
			tickCamouflage();
		}
	}

	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float damageAmount) {
		super.actuallyHurt(level, damageSource, damageAmount);
		this.forceRevealTicks = REVEAL_AFTER_HIT_TICKS;
	}

	private void tickCamouflage() {
		if (this.forceRevealTicks > 0) {
			this.forceRevealTicks--;
		}

		float target = shouldReveal() ? 1.0F : HIDDEN_VISIBILITY;
		float current = getVisibility();
		if (current < target) {
			current = Math.min(target, current + VISIBILITY_STEP);
		} else if (current > target) {
			current = Math.max(target, current - VISIBILITY_STEP);
		}

		setVisibility(current);
	}

	private boolean shouldReveal() {
		if (this.forceRevealTicks > 0 || this.hurtTime > 0) {
			return true;
		}
		if (this.walkAnimation.speed() > 0.02F) {
			return true;
		}
		if (this.level().getBrightness(LightLayer.BLOCK, this.blockPosition()) >= BLOCK_LIGHT_REVEAL) {
			return true;
		}
		Player nearest = this.level().getNearestPlayer(this, PLAYER_REVEAL_RANGE);
		return nearest != null && !nearest.isSpectator();
	}

	public float getVisibility() {
		return this.entityData.get(DATA_VISIBILITY);
	}

	private void setVisibility(float visibility) {
		this.entityData.set(DATA_VISIBILITY, Mth.clamp(visibility, HIDDEN_VISIBILITY, 1.0F));
	}

	/// 0 = original skin, 1 = fully replaced by nearby block textures.
	public float getCamouflageAmount() {
		return 1.0F - Mth.clamp(getVisibility(), 0.0F, 1.0F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("CamouflageVisibility", getVisibility());
		output.putInt("ForceRevealTicks", this.forceRevealTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setVisibility(input.getFloatOr("CamouflageVisibility", 1.0F));
		this.forceRevealTicks = input.getIntOr("ForceRevealTicks", 0);
		this.visibilityO = getVisibility();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("movement", 5, state ->
				state.isMoving() ? state.setAndContinue(WALK_ANIM) : state.setAndContinue(IDLE_ANIM)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
