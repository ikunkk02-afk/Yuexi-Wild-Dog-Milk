package com.shouyun.wilddogmilk.time;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class TimeAccelerationManager {
	public static final float NORMAL_TICK_RATE = 20.0F;
	private static final float[] TICK_RATES = {20.0F, 100.0F, 200.0F, 500.0F, 1000.0F};

	private TimeAccelerationManager() {
	}

	public static float getTickRate(MinecraftServer server) {
		return server.getTickManager().getTickRate();
	}

	public static boolean isAccelerating(MinecraftServer server) {
		return getTickRate(server) > NORMAL_TICK_RATE;
	}

	public static void cycle(MinecraftServer server, ServerPlayerEntity player) {
		float currentRate = getTickRate(server);
		int currentIndex = findRateIndex(currentRate);
		float nextRate = currentIndex == -1 ? NORMAL_TICK_RATE : TICK_RATES[(currentIndex + 1) % TICK_RATES.length];
		setTickRate(server, nextRate);
		showRateMessage(player, nextRate);
	}

	public static void reset(MinecraftServer server, ServerPlayerEntity player) {
		setTickRate(server, NORMAL_TICK_RATE);
		showRateMessage(player, NORMAL_TICK_RATE);
	}

	private static void setTickRate(MinecraftServer server, float tickRate) {
		server.getTickManager().setTickRate(tickRate);
	}

	private static int findRateIndex(float tickRate) {
		for (int index = 0; index < TICK_RATES.length; index++) {
			if (Float.compare(TICK_RATES[index], tickRate) == 0) {
				return index;
			}
		}
		return -1;
	}

	private static void showRateMessage(ServerPlayerEntity player, float tickRate) {
		if (Float.compare(tickRate, NORMAL_TICK_RATE) == 0) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_normal"), true);
			return;
		}

		int multiplier = Math.round(tickRate / NORMAL_TICK_RATE);
		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate", multiplier), true);
		if (Float.compare(tickRate, 1000.0F) == 0) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_warning"), false);
		}
	}
}
