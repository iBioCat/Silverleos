package com.ibiocat.silverleos;

import com.ibiocat.silverleos.registry.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Common entrypoint for the Silverleos mod.
///
/// Silverleos (Чешуйник) is an ancient, chameleon-like cave dweller built for
/// ModJam 2026 - Echoes of the Past.
public class Silverleos implements ModInitializer {
	public static final String MOD_ID = "silverleos";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/// Builds an [Identifier] inside this mod's namespace.
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.initialize();

		LOGGER.info("Silverleos stirs in the deep dark...");
	}
}
