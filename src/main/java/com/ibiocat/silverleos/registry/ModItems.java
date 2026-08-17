package com.ibiocat.silverleos.registry;

import com.ibiocat.silverleos.Silverleos;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

/// Holder and registration point for this mod's items.
public final class ModItems {
	public static final Item SILVERLEOS_SPAWN_EGG = register(
			"silverleos_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.SILVERLEOS)
	);

	private ModItems() {}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(tab -> tab.accept(SILVERLEOS_SPAWN_EGG));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Silverleos.id(name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}
}
