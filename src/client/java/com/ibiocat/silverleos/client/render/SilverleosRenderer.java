package com.ibiocat.silverleos.client.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/// GeckoLib renderer for the Silverleos entity.
///
/// Alpha is captured in [GeoEntityRenderer#getRenderColor] (GeckoLib copies it into
/// the render state) and the pass switches to a translucent buffer whenever the
/// packed color is not fully opaque. Cutout would discard partial alpha.
public class SilverleosRenderer extends GeoEntityRenderer<SilverleosEntity, LivingEntityRenderState> {
	public SilverleosRenderer(EntityRendererProvider.Context context) {
		super(context, new SilverleosModel());
	}

	@Override
	public int getRenderColor(SilverleosEntity animatable, @Nullable Void relatedObject, float partialTick) {
		int color = super.getRenderColor(animatable, relatedObject, partialTick);
		int alpha = Mth.clamp(Math.round(animatable.getRenderVisibility(partialTick) * 255.0F), 0, 255);
		return ARGB.color(alpha, color);
	}

	@Override
	public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
		int color = ((GeoRenderState) renderState).getOrDefaultGeckolibData(DataTickets.RENDER_COLOR, 0xFFFFFFFF);
		if (ARGB.alpha(color) < 255) {
			return RenderTypes.entityTranslucent(texture);
		}
		return super.getRenderType(renderState, texture);
	}
}
