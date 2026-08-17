package com.ibiocat.silverleos.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/// GeckoLib renderer for the Silverleos entity.
///
/// Camouflage is a texture swap, not alpha: nearby block skins are captured in
/// [CamouflageTextures] and read back from the render state by [SilverleosModel].
public class SilverleosRenderer extends GeoEntityRenderer<SilverleosEntity, LivingEntityRenderState> {
	public SilverleosRenderer(EntityRendererProvider.Context context) {
		super(context, new SilverleosModel());
	}

	@Override
	public void addRenderData(SilverleosEntity animatable, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		((GeoRenderState) renderState).addGeckolibData(
				CamouflageTextures.TEXTURE_TICKET,
				CamouflageTextures.textureFor(animatable));
	}
}
