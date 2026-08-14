package com.shouyun.wilddogmilk.time;

import com.shouyun.wilddogmilk.network.TimeStatePayload;
import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The sole owner of this mod's server-side time-control state. World ticking
 * itself remains entirely owned by Minecraft's {@link ServerTickManager}.
 */
public final class TimeAccelerationManager {
	public static final float NORMAL_TICK_RATE = 20.0F;
	public static final float EXTREME_TICK_RATE = 9999.0F;
	public static final int SPRINT_TICKS = 24000;
	private static final float[] TICK_RATES = {
			20.0F, 100.0F, 200.0F, 500.0F, 1000.0F, 2000.0F, 5000.0F, EXTREME_TICK_RATE
	};
	private static final Map<ServerTickManager, TimeControlState> STATES = new IdentityHashMap<>();

	private TimeAccelerationManager() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(TimeAccelerationManager::clearState);
		ServerLifecycleEvents.SERVER_STOPPING.register(TimeAccelerationManager::clearState);
		ServerLifecycleEvents.SERVER_STOPPED.register(TimeAccelerationManager::clearState);
	}

	public static float getTickRate(MinecraftServer server) {
		return server.getTickManager().getTickRate();
	}

	/**
	 * Tick Sprint ignores the configured rate, so cap its performance heuristics
	 * at the same upper bound used for extreme time mode.
	 */
	public static float getAgingTickRate(MinecraftServer server) {
		return server.getTickManager().isSprinting() ? EXTREME_TICK_RATE : getTickRate(server);
	}

	public static boolean isAccelerating(MinecraftServer server) {
		ServerTickManager tickManager = server.getTickManager();
		return tickManager.isSprinting() || tickManager.getTickRate() > NORMAL_TICK_RATE;
	}

	public static void cycle(MinecraftServer server, ServerPlayerEntity player) {
		TimeControlState state = prepareForRateChange(server);
		float currentRate = getControllableTickRate(server, state);
		int currentIndex = findRateIndex(currentRate);
		float nextRate = currentIndex == -1 ? NORMAL_TICK_RATE : TICK_RATES[(currentIndex + 1) % TICK_RATES.length];
		setTickRate(server, state, nextRate);
		showRateMessage(player, nextRate);
		syncEligiblePlayers(server);
	}

	public static void setExtreme(MinecraftServer server, ServerPlayerEntity player) {
		TimeControlState state = prepareForRateChange(server);
		float nextRate = Float.compare(getControllableTickRate(server, state), EXTREME_TICK_RATE) == 0
				? NORMAL_TICK_RATE
				: EXTREME_TICK_RATE;
		setTickRate(server, state, nextRate);
		showRateMessage(player, nextRate);
		syncEligiblePlayers(server);
	}

	/**
	 * The unconditional safety exit. This deliberately does not preserve any
	 * transient mode state.
	 */
	public static void reset(MinecraftServer server, ServerPlayerEntity player) {
		ServerTickManager tickManager = server.getTickManager();
		TimeControlState state = getState(server);
		stopSprint(tickManager, state, SprintFinishReason.SILENT);
		tickManager.setFrozen(false);
		tickManager.setTickRate(NORMAL_TICK_RATE);
		state.clearTemporaryState();
		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.timeline_reset"), true);
		syncEligiblePlayers(server);
	}

	public static void toggleFreeze(MinecraftServer server, ServerPlayerEntity player) {
		ServerTickManager tickManager = server.getTickManager();
		TimeControlState state = getState(server);

		if (tickManager.isSprinting()) {
			stopSprint(tickManager, state, SprintFinishReason.SILENT);
		}

		if (tickManager.isFrozen()) {
			float restoreRate = state.hasPausedPreviousTickRate
					? state.pausedPreviousTickRate
					: tickManager.getTickRate();
			tickManager.setTickRate(restoreRate);
			tickManager.setFrozen(false);
			state.hasPausedPreviousTickRate = false;
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_resumed"), true);
		} else {
			state.pausedPreviousTickRate = tickManager.getTickRate();
			state.hasPausedPreviousTickRate = true;
			tickManager.setFrozen(true);
			player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 0.65F);
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_frozen"), true);
		}

		syncEligiblePlayers(server);
	}

	public static void toggleSprint(MinecraftServer server, ServerPlayerEntity player) {
		ServerTickManager tickManager = server.getTickManager();
		TimeControlState state = getState(server);

		if (tickManager.isSprinting()) {
			stopSprint(tickManager, state, SprintFinishReason.STOPPED);
			return;
		}

		if (tickManager.isFrozen()) {
			tickManager.setFrozen(false);
			state.hasPausedPreviousTickRate = false;
		}

		state.sprintPreviousTickRate = tickManager.getTickRate();
		state.sprintInitiator = player.getUuid();
		state.sprintFinishReason = SprintFinishReason.COMPLETE;
		tickManager.startSprint(SPRINT_TICKS);
		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.sprint_started"), true);
		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.sprint_started_chat"), false);
		syncEligiblePlayers(server);
	}

	/**
	 * Invoked by the small ServerTickManager mixin after vanilla completes or
	 * stops a sprint. This lets vanilla retain ownership of sprint execution.
	 */
	public static void onSprintFinished(ServerTickManager tickManager) {
		TimeControlState state = STATES.get(tickManager);
		if (state == null) {
			return;
		}

		SprintFinishReason finishReason = state.sprintFinishReason;
		UUID initiator = state.sprintInitiator;
		if (initiator != null) {
			tickManager.setTickRate(state.sprintPreviousTickRate);
		}
		state.clearSprintState();

		if (initiator != null && finishReason != SprintFinishReason.SILENT) {
			ServerPlayerEntity player = findPlayer(state.server, initiator);
			if (player != null) {
				if (finishReason == SprintFinishReason.COMPLETE) {
					player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.sprint_complete"), false);
				} else if (finishReason == SprintFinishReason.STOPPED) {
					player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.sprint_stopped"), true);
				}
			}
		}

		syncEligiblePlayers(state.server);
	}

	public static void syncPlayer(MinecraftServer server, ServerPlayerEntity player) {
		if (!PermanentShelfLifeData.has(player) || !ServerPlayNetworking.canSend(player, TimeStatePayload.ID)) {
			return;
		}

		ServerTickManager tickManager = server.getTickManager();
		ServerPlayNetworking.send(player, new TimeStatePayload(
				tickManager.getTickRate(),
				tickManager.isFrozen(),
				tickManager.isSprinting()
		));
	}

	private static TimeControlState prepareForRateChange(MinecraftServer server) {
		ServerTickManager tickManager = server.getTickManager();
		TimeControlState state = getState(server);
		if (tickManager.isSprinting()) {
			stopSprint(tickManager, state, SprintFinishReason.SILENT);
		}
		return state;
	}

	private static boolean stopSprint(ServerTickManager tickManager, TimeControlState state, SprintFinishReason finishReason) {
		if (!tickManager.isSprinting()) {
			return false;
		}

		state.sprintFinishReason = finishReason;
		return tickManager.stopSprinting();
	}

	private static float getControllableTickRate(MinecraftServer server, TimeControlState state) {
		return server.getTickManager().isFrozen() && state.hasPausedPreviousTickRate
				? state.pausedPreviousTickRate
				: getTickRate(server);
	}

	private static void setTickRate(MinecraftServer server, TimeControlState state, float tickRate) {
		ServerTickManager tickManager = server.getTickManager();
		tickManager.setTickRate(tickRate);
		if (tickManager.isFrozen()) {
			state.pausedPreviousTickRate = tickRate;
			state.hasPausedPreviousTickRate = true;
		}
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

		if (Float.compare(tickRate, EXTREME_TICK_RATE) == 0) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_extreme"), true);
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_extreme_chat"), false);
			return;
		}

		int rate = Math.round(tickRate);
		int multiplier = Math.round(tickRate / NORMAL_TICK_RATE);
		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate", rate, multiplier), true);
		if (Float.compare(tickRate, 2000.0F) == 0) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_warning_2000"), false);
		} else if (Float.compare(tickRate, 5000.0F) == 0) {
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.time_rate_warning_5000"), false);
		}
	}

	private static void syncEligiblePlayers(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			syncPlayer(server, player);
		}
	}

	private static ServerPlayerEntity findPlayer(MinecraftServer server, UUID uuid) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.getUuid().equals(uuid)) {
				return player;
			}
		}
		return null;
	}

	private static TimeControlState getState(MinecraftServer server) {
		ServerTickManager tickManager = server.getTickManager();
		return STATES.computeIfAbsent(tickManager, ignored -> new TimeControlState(server));
	}

	private static void clearState(MinecraftServer server) {
		STATES.remove(server.getTickManager());
	}

	private enum SprintFinishReason {
		COMPLETE,
		STOPPED,
		SILENT
	}

	private static final class TimeControlState {
		private final MinecraftServer server;
		private float pausedPreviousTickRate = NORMAL_TICK_RATE;
		private boolean hasPausedPreviousTickRate;
		private float sprintPreviousTickRate = NORMAL_TICK_RATE;
		private UUID sprintInitiator;
		private SprintFinishReason sprintFinishReason = SprintFinishReason.COMPLETE;

		private TimeControlState(MinecraftServer server) {
			this.server = server;
		}

		private void clearSprintState() {
			sprintPreviousTickRate = NORMAL_TICK_RATE;
			sprintInitiator = null;
			sprintFinishReason = SprintFinishReason.COMPLETE;
		}

		private void clearTemporaryState() {
			hasPausedPreviousTickRate = false;
			pausedPreviousTickRate = NORMAL_TICK_RATE;
			clearSprintState();
		}
	}
}
