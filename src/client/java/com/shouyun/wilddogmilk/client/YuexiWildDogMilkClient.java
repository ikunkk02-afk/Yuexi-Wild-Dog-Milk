package com.shouyun.wilddogmilk.client;

import com.shouyun.wilddogmilk.network.TimeControlAction;
import com.shouyun.wilddogmilk.network.TimeControlPayload;
import com.shouyun.wilddogmilk.network.TimeStatePayload;
import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import com.shouyun.wilddogmilk.time.SprintMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class YuexiWildDogMilkClient implements ClientModInitializer {
	private static final String KEY_CATEGORY = "key.categories.yuexi-wild-dog-milk";
	private static final String TIME_FLOW_KEY = "key.yuexi-wild-dog-milk.time_acceleration";
	private static final String TIME_PAUSE_KEY = "key.yuexi-wild-dog-milk.time_pause";
	private static final String TIME_SPRINT_KEY = "key.yuexi-wild-dog-milk.time_sprint";
	private static final String DEEP_TIME_KEY = "key.yuexi-wild-dog-milk.deep_time";
	private static final float NORMAL_TICK_RATE = 20.0F;
	private static final float EXTREME_TICK_RATE = 9999.0F;
	private static final long FREEZE_FLASH_NANOS = 250_000_000L;
	private static KeyBinding timeFlowKey;
	private static KeyBinding timePauseKey;
	private static KeyBinding timeSprintKey;
	private static KeyBinding deepTimeKey;
	private static TimeStatePayload timeState = new TimeStatePayload(NORMAL_TICK_RATE, false, SprintMode.NONE.id());
	private static boolean hasReceivedTimeState;
	private static long freezeFlashUntilNanos;

	@Override
	public void onInitializeClient() {
		timeFlowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				TIME_FLOW_KEY,
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				KEY_CATEGORY
		));
		timePauseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				TIME_PAUSE_KEY,
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				KEY_CATEGORY
		));
		timeSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				TIME_SPRINT_KEY,
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				KEY_CATEGORY
		));
		deepTimeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				DEEP_TIME_KEY,
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (timeFlowKey.wasPressed()) {
				if (Screen.hasShiftDown()) {
					sendAction(TimeControlAction.RESET);
				} else if (Screen.hasControlDown()) {
					sendAction(TimeControlAction.EXTREME);
				} else {
					sendAction(TimeControlAction.CYCLE);
				}
			}

			while (timePauseKey.wasPressed()) {
				sendAction(TimeControlAction.TOGGLE_FREEZE);
			}

			while (timeSprintKey.wasPressed()) {
				sendAction(TimeControlAction.SPRINT);
			}

			while (deepTimeKey.wasPressed()) {
				sendAction(TimeControlAction.DEEP_TIME);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(TimeStatePayload.ID, (payload, context) -> {
			if (hasReceivedTimeState && timeState.frozen() != payload.frozen()) {
				freezeFlashUntilNanos = System.nanoTime() + FREEZE_FLASH_NANOS;
			}
			timeState = payload;
			hasReceivedTimeState = true;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetTimeState());
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderTimeHud(MinecraftClient.getInstance(), drawContext));
	}

	private static void sendAction(TimeControlAction action) {
		if (ClientPlayNetworking.canSend(TimeControlPayload.ID)) {
			ClientPlayNetworking.send(new TimeControlPayload(action.id()));
		}
	}

	private static void renderTimeHud(MinecraftClient client, net.minecraft.client.gui.DrawContext drawContext) {
		if (client.player == null || !client.player.getAttachedOrElse(PermanentShelfLifeData.PERMANENT_SHELF_LIFE, false)) {
			return;
		}

		int x = 6;
		int y = 6;
		drawContext.drawText(client.textRenderer, Text.translatable("hud.yuexi-wild-dog-milk.shelf_life"), x, y, 0x66CC66, true);

		boolean hasDeepTime = client.player.getAttachedOrElse(PermanentShelfLifeData.DEEP_TIME_POWER, false);
		int timeStateOffset = 10;
		if (hasDeepTime) {
			drawContext.drawText(client.textRenderer, Text.translatable("hud.yuexi-wild-dog-milk.deep_time_unlocked"),
					x, y + 10, 0xC77DFF, true);
			timeStateOffset = 20;
		}

		SprintMode sprintMode = timeState.sprintMode();
		boolean sprinting = sprintMode != SprintMode.NONE;
		boolean frozen = client.world != null ? client.world.getTickManager().isFrozen() : timeState.frozen();
		float tickRate = client.world != null ? client.world.getTickManager().getTickRate() : timeState.tickRate();
		Text rateText;
		int rateColor;
		if (sprinting) {
			rateText = Text.translatable("hud.yuexi-wild-dog-milk.time_rate", "∞");
			rateColor = 0xC77DFF;
		} else if (frozen) {
			rateText = Text.translatable("hud.yuexi-wild-dog-milk.time_rate", "0×");
			rateColor = 0xA0A0A0;
		} else {
			rateText = Text.translatable("hud.yuexi-wild-dog-milk.time_rate", formatMultiplier(tickRate));
			rateColor = getRateColor(tickRate);
		}
		drawContext.drawText(client.textRenderer, rateText, x, y + timeStateOffset, rateColor, true);

		if (sprinting) {
			String key = sprintMode == SprintMode.DEEP_TIME
					? "hud.yuexi-wild-dog-milk.deep_time"
					: "hud.yuexi-wild-dog-milk.sprint";
			int color = sprintMode == SprintMode.DEEP_TIME ? 0xAA55FF : 0xC77DFF;
			drawContext.drawText(client.textRenderer, Text.translatable(key), x, y + timeStateOffset + 10, color, true);
		} else if (frozen) {
			drawContext.drawText(client.textRenderer, Text.translatable("hud.yuexi-wild-dog-milk.frozen"),
					x, y + timeStateOffset + 10, 0xA0A0A0, true);
		}

		drawFreezeFlash(client, drawContext);
	}

	private static String formatMultiplier(float tickRate) {
		if (Float.compare(tickRate, EXTREME_TICK_RATE) == 0) {
			return "≈500×";
		}
		return Math.round(tickRate / NORMAL_TICK_RATE) + "×";
	}

	private static int getRateColor(float tickRate) {
		if (tickRate >= EXTREME_TICK_RATE) {
			return 0xFF5555;
		}
		if (tickRate >= 5000.0F) {
			return 0xFFAA00;
		}
		if (tickRate > NORMAL_TICK_RATE) {
			return 0xFFFF55;
		}
		return 0xFFFFFF;
	}

	private static void drawFreezeFlash(MinecraftClient client, net.minecraft.client.gui.DrawContext drawContext) {
		long remaining = freezeFlashUntilNanos - System.nanoTime();
		if (remaining <= 0L) {
			return;
		}

		int alpha = (int) Math.max(0L, Math.min(32L, remaining * 32L / FREEZE_FLASH_NANOS));
		drawContext.fill(
				0,
				0,
				client.getWindow().getScaledWidth(),
				client.getWindow().getScaledHeight(),
				alpha << 24
		);
	}

	private static void resetTimeState() {
		timeState = new TimeStatePayload(NORMAL_TICK_RATE, false, SprintMode.NONE.id());
		hasReceivedTimeState = false;
		freezeFlashUntilNanos = 0L;
	}
}
