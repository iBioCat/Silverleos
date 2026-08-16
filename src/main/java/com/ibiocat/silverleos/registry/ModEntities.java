package com.ibiocat.silverleos.registry;

import com.ibiocat.silverleos.Silverleos;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/// Holder and registration point for this mod's [EntityType]s.
public final class ModEntities {
	public static final ResourceKey<EntityType<?>> SILVERLEOS_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Silverleos.id("silverleos"));

	public static final EntityType<SilverleosEntity> SILVERLEOS = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			SILVERLEOS_KEY,
			// Bipedal build: the model stands ~31px tall (~1.9 blocks) with eyes near the top of the skull.
			EntityType.Builder.of(SilverleosEntity::new, MobCategory.CREATURE)
					.sized(0.7f, 1.9f)
					.eyeHeight(1.72f)
					.clientTrackingRange(10)
					.build(SILVERLEOS_KEY)
	);

	private ModEntities() {}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(SILVERLEOS, SilverleosEntity.createAttributes());
	}
}
