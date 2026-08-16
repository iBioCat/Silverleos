package com.ibiocat.silverleos.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.ibiocat.silverleos.Silverleos;
import com.ibiocat.silverleos.entity.SilverleosEntity;

/// GeckoLib model binding for the Silverleos.
///
/// [DefaultedEntityGeoModel] resolves the asset paths from the given base id:
///
///   - model: `assets/silverleos/geckolib/models/entity/silverleos.geo.json`
///   - animations: `assets/silverleos/geckolib/animations/entity/silverleos.animation.json`
///   - texture: `assets/silverleos/textures/entity/silverleos.png`
public class SilverleosModel extends DefaultedEntityGeoModel<SilverleosEntity> {
	public SilverleosModel() {
		super(Silverleos.id("silverleos"));
	}
}
