package com.ibiocat.silverleos.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.google.common.reflect.TypeToken;
import com.ibiocat.silverleos.Silverleos;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Builds an opaque per-entity skin from nearby block textures.
///
/// Transparent UV holes stay empty. Opaque pixels are replaced with a collage
/// of the blocks around the mob, then blended back toward the original skin
/// as the mob reveals.
final class CamouflageTextures {
	static final DataTicket<Identifier> TEXTURE_TICKET =
			DataTicket.create("silverleos_camo_texture", new TypeToken<Identifier>() {});

	static final Identifier ORIGINAL_TEXTURE = Silverleos.id("textures/entity/silverleos.png");
	private static final int SIZE = 128;
	private static final int TILE = 16;
	private static final int[][] SAMPLE_OFFSETS = {
			{0, -1, 0},
			{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
			{0, 1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
			{0, 2, 0}, {1, 2, 0}, {-1, 2, 0}
	};

	private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final Map<Identifier, NativeImage> BLOCK_IMAGES = new ConcurrentHashMap<>();
	private static @Nullable NativeImage originalPixels;

	private CamouflageTextures() {}

	static Identifier textureFor(SilverleosEntity entity) {
		float camo = entity.getCamouflageAmount();
		if (camo < 0.02F || entity.isRemoved()) {
			release(entity.getId());
			return ORIGINAL_TEXTURE;
		}

		Entry entry = ENTRIES.computeIfAbsent(entity.getId(), CamouflageTextures::createEntry);
		BlockPos floor = entity.blockPosition().below();
		boolean moving = Math.abs(entry.lastCamo - camo) > 0.02F;
		boolean relocated = !floor.equals(entry.lastFloor);
		if (!moving && !relocated && entity.tickCount % 5 != 0) {
			return entry.id;
		}

		rebuild(entity, entry, camo, floor);
		return entry.id;
	}

	private static Entry createEntry(int entityId) {
		Identifier id = Silverleos.id("dynamic/camo/" + entityId);
		DynamicTexture texture = new DynamicTexture(() -> "silverleos camo " + entityId, SIZE, SIZE, true);
		Minecraft.getInstance().getTextureManager().register(id, texture);
		return new Entry(id, texture);
	}

	private static void rebuild(SilverleosEntity entity, Entry entry, float camo, BlockPos floor) {
		NativeImage dest = entry.texture.getPixels();
		NativeImage source = original();
		if (dest == null || source == null) {
			return;
		}

		List<Sample> samples = collectSamples(entity);
		if (samples.isEmpty()) {
			copyImage(source, dest);
			entry.texture.upload();
			entry.lastCamo = 0.0F;
			entry.lastFloor = floor;
			return;
		}

		int tiles = SIZE / TILE;
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int orig = source.getPixel(x, y);
				if (ARGB.alpha(orig) == 0) {
					dest.setPixel(x, y, 0);
					continue;
				}

				Sample sample = samples.get(((x / TILE) + (y / TILE) * tiles) % samples.size());
				dest.setPixel(x, y, lerpOpaque(orig, sample.pixel(x, y), camo));
			}
		}

		entry.texture.upload();
		entry.lastCamo = camo;
		entry.lastFloor = floor;
	}

	private static List<Sample> collectSamples(SilverleosEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockStateModelSet models = minecraft.getModelManager().getBlockStateModelSet();
		List<Sample> samples = new ArrayList<>();
		BlockPos origin = entity.blockPosition();

		for (int[] offset : SAMPLE_OFFSETS) {
			BlockPos pos = origin.offset(offset[0], offset[1], offset[2]);
			BlockState state = entity.level().getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) {
				continue;
			}

			Material.Baked material = models.getParticleMaterial(state);
			TextureAtlasSprite sprite = material.sprite();
			NativeImage image = loadBlockImage(sprite.contents().name());
			if (image == null) {
				continue;
			}

			samples.add(new Sample(image, tintFor(minecraft, entity, state, pos)));
		}

		return samples;
	}

	private static int tintFor(Minecraft minecraft, SilverleosEntity entity, BlockState state, BlockPos pos) {
		BlockTintSource source = minecraft.getBlockColors().getTintSource(state, 0);
		if (source == null) {
			return 0xFFFFFF;
		}
		if (entity.level() instanceof BlockAndTintGetter view) {
			return source.colorInWorld(state, view, pos);
		}
		return source.color(state);
	}

	private static @Nullable NativeImage loadBlockImage(Identifier spriteName) {
		NativeImage cached = BLOCK_IMAGES.get(spriteName);
		if (cached != null) {
			return cached;
		}

		Identifier file = Identifier.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + spriteName.getPath() + ".png");
		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(file)) {
			NativeImage image = NativeImage.read(stream);
			BLOCK_IMAGES.put(spriteName, image);
			return image;
		} catch (IOException exception) {
			return null;
		}
	}

	private static NativeImage original() {
		if (originalPixels != null) {
			return originalPixels;
		}

		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(ORIGINAL_TEXTURE)) {
			originalPixels = NativeImage.read(stream);
			return originalPixels;
		} catch (IOException exception) {
			Silverleos.LOGGER.warn("Could not read Silverleos skin for camouflage", exception);
			return null;
		}
	}

	private static void copyImage(NativeImage from, NativeImage to) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				to.setPixel(x, y, from.getPixel(x, y));
			}
		}
	}

	private static int lerpOpaque(int from, int to, float t) {
		int r = Math.round(ARGB.red(from) + (ARGB.red(to) - ARGB.red(from)) * t);
		int g = Math.round(ARGB.green(from) + (ARGB.green(to) - ARGB.green(from)) * t);
		int b = Math.round(ARGB.blue(from) + (ARGB.blue(to) - ARGB.blue(from)) * t);
		return ARGB.color(255, r, g, b);
	}

	static void release(int entityId) {
		Entry entry = ENTRIES.remove(entityId);
		if (entry == null) {
			return;
		}
		Minecraft.getInstance().getTextureManager().release(entry.id);
		entry.texture.close();
	}

	private record Sample(NativeImage image, int tint) {
		int pixel(int x, int y) {
			int sampled = this.image.getPixel(
					Math.floorMod(x, this.image.getWidth()),
					Math.floorMod(y, this.image.getHeight()));
			if (this.tint == -1 || (this.tint & 0xFFFFFF) == 0xFFFFFF) {
				return sampled;
			}
			int r = ARGB.red(sampled) * ARGB.red(this.tint) / 255;
			int g = ARGB.green(sampled) * ARGB.green(this.tint) / 255;
			int b = ARGB.blue(sampled) * ARGB.blue(this.tint) / 255;
			return ARGB.color(255, r, g, b);
		}
	}

	private static final class Entry {
		final Identifier id;
		final DynamicTexture texture;
		float lastCamo = -1.0F;
		BlockPos lastFloor = BlockPos.ZERO;

		Entry(Identifier id, DynamicTexture texture) {
			this.id = id;
			this.texture = texture;
		}
	}
}
