package com.minimods;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class MiniModsClient implements ClientModInitializer {
	public static final String MOD_ID = "minimods";

	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		Config.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.minimods.open_menu",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_RSHIFT,
				category));

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), MiniModsHud::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MiniModsHud.tick(client);
			while (openMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new MiniModsScreen());
				}
			}
		});
	}
}
