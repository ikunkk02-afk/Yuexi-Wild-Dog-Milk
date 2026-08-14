package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(TimeRatePayload.ID, TimeRatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TimeRatePayload.ID, ModNetworking::handleTimeRateRequest);
	}

	private static void handleTimeRateRequest(TimeRatePayload payload, ServerPlayNetworking.Context context) {
		ServerPlayerEntity player = context.player();
		if (!PermanentShelfLifeData.has(player)) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.no_permanent_shelf_life"), true);
			return;
		}

		if (payload.reset()) {
			TimeAccelerationManager.reset(context.server(), player);
		} else {
			TimeAccelerationManager.cycle(context.server(), player);
		}
	}
}
