package com.shouyun.wilddogmilk.time.nature;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Scales vanilla's per-section random-tick budget only after vanilla has
 * selected a ticking chunk. The player's game rule is never changed.
 */
public final class TemporalRandomTickManager {
	public static final int MAX_RANDOM_TICK_BUDGET = 4096;

	private TemporalRandomTickManager() {
	}

	public static int scaleRandomTickBudget(ServerWorld world, int baseRandomTickSpeed) {
		if (baseRandomTickSpeed <= 0) {
			return 0;
		}

		long effectiveBudget = (long) baseRandomTickSpeed * getRandomTickMultiplier(world.getServer());
		return (int) Math.min(effectiveBudget, MAX_RANDOM_TICK_BUDGET);
	}

	/** Returns the total vanilla random-tick budget multiplier for one world tick. */
	public static int getRandomTickMultiplier(MinecraftServer server) {
		if (server.getTickManager().isFrozen()) {
			return 1;
		}
		if (server.getTickManager().isSprinting()) {
			return 32;
		}

		float tickRate = TimeAccelerationManager.getTickRate(server);
		if (tickRate >= TimeAccelerationManager.EXTREME_TICK_RATE) return 24;
		if (tickRate >= 5000.0F) return 16;
		if (tickRate >= 2000.0F) return 12;
		if (tickRate >= 1000.0F) return 8;
		if (tickRate >= 500.0F) return 5;
		if (tickRate >= 200.0F) return 3;
		if (tickRate >= 100.0F) return 2;
		return 1;
	}

	/** Called only when time-control state changes, never from a world tick. */
	public static void logCurrentBudgets(MinecraftServer server) {
		int multiplier = getRandomTickMultiplier(server);
		for (ServerWorld world : server.getWorlds()) {
			int base = world.getGameRules().getInt(net.minecraft.world.GameRules.RANDOM_TICK_SPEED);
			YuexiWildDogMilk.LOGGER.debug(
					"Natural time budget: dimension={}, tickRate={}, randomTickSpeed={}, multiplier={}, effectiveBudget={}",
					world.getRegistryKey().getValue(), TimeAccelerationManager.getAgingTickRate(server),
					base, multiplier, scaleRandomTickBudget(world, base)
			);
		}
	}
}
