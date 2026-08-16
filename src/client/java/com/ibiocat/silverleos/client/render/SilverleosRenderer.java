package com.ibiocat.silverleos.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/// GeckoLib renderer for the Silverleos entity.
public class SilverleosRenderer extends GeoEntityRenderer<SilverleosEntity, LivingEntityRenderState> {
	public SilverleosRenderer(EntityRendererProvider.Context context) {
		super(context, new SilverleosModel());
	}
}
