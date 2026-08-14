package com.shouyun.wilddogmilk.client;

import com.shouyun.wilddogmilk.network.TimeRatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class YuexiWildDogMilkClient implements ClientModInitializer {
	private static final String KEY_CATEGORY = "key.categories.yuexi-wild-dog-milk";
	private static final String TIME_ACCELERATION_KEY = "key.yuexi-wild-dog-milk.time_acceleration";
	private static KeyBinding timeAccelerationKey;

	@Override
	public void onInitializeClient() {
		timeAccelerationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				TIME_ACCELERATION_KEY,
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (timeAccelerationKey.wasPressed()) {
				ClientPlayNetworking.send(new TimeRatePayload(Screen.hasShiftDown()));
			}
		});
	}
}
