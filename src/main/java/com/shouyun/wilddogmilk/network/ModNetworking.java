package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(TimeControlPayload.ID, TimeControlPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TimeStatePayload.ID, TimeStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TemporalDistortionPayload.ID, TemporalDistortionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TimeControlPayload.ID, ModNetworking::handleTimeControlRequest);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				TimeAccelerationManager.syncPlayer(server, handler.player)
		);
	}

	private static void handleTimeControlRequest(TimeControlPayload payload, ServerPlayNetworking.Context context) {
		TimeControlAction action = TimeControlAction.fromId(payload.actionId());
		if (action == null) {
			return;
		}

		ServerPlayerEntity player = context.player();
		if (action == TimeControlAction.DEEP_TIME) {
			if (!PermanentShelfLifeData.has(player) || !PermanentShelfLifeData.hasDeepTime(player)) {
				player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.no_deep_time_power"), true);
				return;
			}
			TimeAccelerationManager.toggleDeepTime(context.server(), player);
			return;
		}

		if (!PermanentShelfLifeData.has(player)) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.no_permanent_shelf_life"), true);
			return;
		}

		switch (action) {
			case CYCLE -> TimeAccelerationManager.cycle(context.server(), player);
			case RESET -> TimeAccelerationManager.reset(context.server(), player);
			case EXTREME -> TimeAccelerationManager.setExtreme(context.server(), player);
			case TOGGLE_FREEZE -> TimeAccelerationManager.toggleFreeze(context.server(), player);
			case SPRINT -> TimeAccelerationManager.toggleSprint(context.server(), player);
			case DEEP_TIME -> throw new IllegalStateException("Deep Time is handled before permanent shelf-life actions");
		}
	}
}
