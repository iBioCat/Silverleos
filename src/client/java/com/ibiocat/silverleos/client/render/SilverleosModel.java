package com.ibiocat.silverleos.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.ibiocat.silverleos.Silverleos;
import com.ibiocat.silverleos.entity.SilverleosEntity;
import net.minecraft.resources.Identifier;

/// GeckoLib model binding for the Silverleos.
///
/// [DefaultedEntityGeoModel] resolves the asset paths from the given base id:
///
///   - model: `assets/silverleos/geckolib/models/entity/silverleos.geo.json`
///   - animations: `assets/silverleos/geckolib/animations/entity/silverleos.animation.json`
///   - texture: `assets/silverleos/textures/entity/silverleos.png`
///
/// When camouflaged, [getTextureResource] swaps in a dynamic collage of nearby
/// block textures captured on the render state.
public class SilverleosModel extends DefaultedEntityGeoModel<SilverleosEntity> {
	public SilverleosModel() {
		super(Silverleos.id("silverleos"));
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		if (renderState.hasGeckolibData(CamouflageTextures.TEXTURE_TICKET)) {
			return renderState.getGeckolibData(CamouflageTextures.TEXTURE_TICKET);
		}
		return super.getTextureResource(renderState);
	}
}
