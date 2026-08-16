package com.ibiocat.silverleos.client;

import com.ibiocat.silverleos.client.render.SilverleosRenderer;
import com.ibiocat.silverleos.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/// Client entrypoint: binds the Silverleos entity to its GeckoLib renderer.
public class SilverleosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.SILVERLEOS, SilverleosRenderer::new);
	}
}
