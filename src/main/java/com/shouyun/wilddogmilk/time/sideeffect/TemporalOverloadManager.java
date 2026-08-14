package com.shouyun.wilddogmilk.time.sideeffect;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.network.TemporalDistortionPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the personal, real-time-limited side effects of drinking too much dog
 * milk. It intentionally neither changes world time nor rolls back gameplay
 * data beyond a safe position-and-rotation teleport.
 */
public final class TemporalOverloadManager {
	public static final int NORMAL_MILK_LOAD = 30;
	public static final int CENTURY_AGED_MILK_LOAD = 60;
	public static final long LOAD_DECAY_INTERVAL_MILLIS = 2_000L;

	private static final long PROCESS_INTERVAL_NANOS = 250_000_000L;
	private static final long MILLIS_TO_NANOS = 1_000_000L;
	private static final Map<MinecraftServer, ServerState> STATES = new IdentityHashMap<>();

	private static final String[] DISPLACED_MESSAGES = {
			"message.yuexi-wild-dog-milk.temporal.displaced.event_one",
			"message.yuexi-wild-dog-milk.temporal.displaced.event_two",
			"message.yuexi-wild-dog-milk.temporal.displaced.event_three"
	};
	private static final String[] UNSTABLE_MESSAGES = {
			"message.yuexi-wild-dog-milk.temporal.unstable.event_one",
			"message.yuexi-wild-dog-milk.temporal.unstable.event_two"
	};
	private static final String[] OVERLOAD_MESSAGES = {
			"message.yuexi-wild-dog-milk.temporal.overload.event_one",
			"message.yuexi-wild-dog-milk.temporal.overload.event_two",
			"message.yuexi-wild-dog-milk.temporal.overload.event_three",
			"message.yuexi-wild-dog-milk.temporal.overload.event_four"
	};

	public static final AttachmentType<TemporalOverloadData> TEMPORAL_OVERLOAD = AttachmentRegistry.create(
			YuexiWildDogMilk.id("temporal_overload"),
			builder -> builder
					.persistent(TemporalOverloadData.CODEC)
					.copyOnDeath()
					.initializer(() -> TemporalOverloadData.emptyAt(System.currentTimeMillis()))
					.syncWith(TemporalOverloadData.PACKET_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	private TemporalOverloadManager() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TemporalOverloadManager::tick);
		ServerPlayerEvents.JOIN.register(TemporalOverloadManager::onJoin);
		ServerPlayerEvents.LEAVE.register(TemporalOverloadManager::clearPlayerSession);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			clearPlayerSession(oldPlayer);
			clearPlayerSession(newPlayer);
			restoreAfterRespawn(newPlayer);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				clearPlayerSession(player)
		);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				clearPlayerSession(player);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(TemporalOverloadManager::clearServerState);
		ServerLifecycleEvents.SERVER_STOPPING.register(TemporalOverloadManager::clearServerState);
		ServerLifecycleEvents.SERVER_STOPPED.register(TemporalOverloadManager::clearServerState);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("wilddogmilk")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("temporal_load")
								.then(CommandManager.literal("get").executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									int load = getLoad(player);
									context.getSource().sendFeedback(
											() -> Text.translatable("command.yuexi-wild-dog-milk.temporal.get", load),
											false
									);
									return 1;
								}))
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("load", IntegerArgumentType.integer(0, 100))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayer();
													int load = IntegerArgumentType.getInteger(context, "load");
													setLoad(player, load);
													context.getSource().sendFeedback(
															() -> Text.translatable(
																	"command.yuexi-wild-dog-milk.temporal.set", load
															),
															false
													);
													return 1;
												})))
								.then(CommandManager.literal("clear").executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									setLoad(player, 0);
									context.getSource().sendFeedback(
											() -> Text.translatable("command.yuexi-wild-dog-milk.temporal.clear"),
											false
									);
									return 1;
								})))
				)
		);
	}

	public static int getLoad(ServerPlayerEntity player) {
		return refreshLoad(player, System.currentTimeMillis()).load();
	}

	public static TemporalOverloadStage getStage(ServerPlayerEntity player) {
		return TemporalOverloadStage.fromLoad(getLoad(player));
	}

	public static void addLoad(ServerPlayerEntity player, int amount, boolean suppressOverloadTitle) {
		if (amount <= 0) {
			return;
		}
		TemporalOverloadData before = refreshLoad(player, System.currentTimeMillis());
		changeLoad(player, before, Math.min(100, before.load() + amount), suppressOverloadTitle);
	}

	public static void setLoad(ServerPlayerEntity player, int load) {
		TemporalOverloadData before = refreshLoad(player, System.currentTimeMillis());
		changeLoad(player, before, load, false);
	}

	private static void changeLoad(
			ServerPlayerEntity player,
			TemporalOverloadData before,
			int requestedLoad,
			boolean suppressOverloadTitle
	) {
		long nowMillis = System.currentTimeMillis();
		long nowNanos = System.nanoTime();
		TemporalOverloadStage beforeStage = before.stage();
		TemporalOverloadData after = new TemporalOverloadData(requestedLoad, nowMillis);
		player.setAttached(TEMPORAL_OVERLOAD, after);

		ServerState state = stateFor(player.getServer());
		if (after.load() == 0) {
			state.sessions.remove(player.getUuid());
			player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.temporal.load", after.load()), true);
			return;
		}
		PlayerSession session = state.sessions.computeIfAbsent(
				player.getUuid(),
				ignored -> new PlayerSession()
		);
		updateSessionStage(session, after.stage(), nowNanos);

		player.sendMessage(Text.translatable("message.yuexi-wild-dog-milk.temporal.load", after.load()), true);
		if (beforeStage != after.stage()) {
			announceStageEntry(player, after.stage(), suppressOverloadTitle);
		}
	}

	private static void announceStageEntry(
			ServerPlayerEntity player,
			TemporalOverloadStage stage,
			boolean suppressOverloadTitle
	) {
		switch (stage) {
			case DISPLACED -> player.sendMessage(
					Text.translatable("message.yuexi-wild-dog-milk.temporal.displaced.enter"),
					false
			);
			case UNSTABLE -> player.sendMessage(
					Text.translatable("message.yuexi-wild-dog-milk.temporal.unstable.enter"),
					false
			);
			case OVERLOAD -> {
				if (suppressOverloadTitle) {
					player.sendMessage(
							Text.translatable("message.yuexi-wild-dog-milk.temporal.overload.enter"),
							false
					);
					return;
				}
				player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 10));
				player.networkHandler.sendPacket(new TitleS2CPacket(
						Text.translatable("title.yuexi-wild-dog-milk.temporal_overload")
				));
				player.networkHandler.sendPacket(new SubtitleS2CPacket(
						Text.translatable("subtitle.yuexi-wild-dog-milk.temporal_overload")
				));
			}
			case STABLE -> {
				// Falling below a threshold is intentionally quiet.
			}
		}
	}

	private static void onJoin(ServerPlayerEntity player) {
		restorePlayer(player, true);
	}

	private static void restoreAfterRespawn(ServerPlayerEntity player) {
		restorePlayer(player, true);
	}

	private static void restorePlayer(ServerPlayerEntity player, boolean forceSync) {
		if (!player.hasAttached(TEMPORAL_OVERLOAD)) {
			return;
		}

		TemporalOverloadData before = player.getAttachedOrElse(TEMPORAL_OVERLOAD, TemporalOverloadData.EMPTY);
		TemporalOverloadData after = decay(before, System.currentTimeMillis());
		if (forceSync || !before.equals(after)) {
			player.setAttached(TEMPORAL_OVERLOAD, after);
		}
		if (after.load() > 0) {
			PlayerSession session = stateFor(player.getServer()).sessions.computeIfAbsent(
					player.getUuid(),
					ignored -> new PlayerSession()
			);
			updateSessionStage(session, after.stage(), System.nanoTime());
		}
	}

	private static void tick(MinecraftServer server) {
		long nowNanos = System.nanoTime();
		ServerState state = stateFor(server);
		if (nowNanos < state.nextProcessNanos) {
			return;
		}
		state.nextProcessNanos = nowNanos + PROCESS_INTERVAL_NANOS;
		long nowMillis = System.currentTimeMillis();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			TemporalOverloadData data = refreshLoad(player, nowMillis);
			if (data.load() <= 0) {
				state.sessions.remove(player.getUuid());
				continue;
			}

			PlayerSession session = state.sessions.computeIfAbsent(player.getUuid(), ignored -> new PlayerSession());
			updateSessionStage(session, data.stage(), nowNanos);
			samplePosition(player, session, nowNanos);
			tickSideEffects(server, player, session, data.stage(), nowNanos);
		}
	}

	private static TemporalOverloadData refreshLoad(ServerPlayerEntity player, long nowMillis) {
		if (!player.hasAttached(TEMPORAL_OVERLOAD)) {
			return TemporalOverloadData.EMPTY;
		}

		TemporalOverloadData before = player.getAttachedOrElse(TEMPORAL_OVERLOAD, TemporalOverloadData.EMPTY);
		TemporalOverloadData after = decay(before, nowMillis);
		if (!before.equals(after)) {
			player.setAttached(TEMPORAL_OVERLOAD, after);
		}
		return after;
	}

	private static TemporalOverloadData decay(TemporalOverloadData data, long nowMillis) {
		if (data.load() <= 0 || data.lastUpdateEpochMillis() <= 0L) {
			return data;
		}
		long elapsed = Math.max(0L, nowMillis - data.lastUpdateEpochMillis());
		long steps = elapsed / LOAD_DECAY_INTERVAL_MILLIS;
		if (steps <= 0L) {
			return data;
		}

		int remaining = (int) Math.max(0L, data.load() - steps);
		long updatedAt = remaining == 0
				? nowMillis
				: data.lastUpdateEpochMillis() + steps * LOAD_DECAY_INTERVAL_MILLIS;
		return new TemporalOverloadData(remaining, updatedAt);
	}

	private static void samplePosition(ServerPlayerEntity player, PlayerSession session, long nowNanos) {
		if (nowNanos - session.lastSampleNanos < PROCESS_INTERVAL_NANOS) {
			return;
		}
		session.lastSampleNanos = nowNanos;
		session.history.add(new TemporalPositionSample(
				player.getServerWorld().getRegistryKey(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYaw(),
				player.getPitch(),
				nowNanos
		));
	}

	private static void tickSideEffects(
			MinecraftServer server,
			ServerPlayerEntity player,
			PlayerSession session,
			TemporalOverloadStage stage,
			long nowNanos
	) {
		if (stage == TemporalOverloadStage.STABLE) {
			return;
		}
		if (server.getTickManager().isSprinting()) {
			session.suppressedBySprint = true;
			return;
		}
		if (session.suppressedBySprint) {
			session.suppressedBySprint = false;
			scheduleNextEvent(session, stage, nowNanos);
			return;
		}
		if (session.nextEventNanos == 0L) {
			scheduleNextEvent(session, stage, nowNanos);
			return;
		}
		if (nowNanos < session.nextEventNanos) {
			return;
		}

		triggerDistortion(player, session, stage, nowNanos);
		scheduleNextEvent(session, stage, nowNanos);
	}

	private static void triggerDistortion(
			ServerPlayerEntity player,
			PlayerSession session,
			TemporalOverloadStage stage,
			long nowNanos
	) {
		switch (stage) {
			case DISPLACED -> {
				sendDistortion(player, TemporalDistortionPayload.LIGHT, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.35F, 0.7F);
				player.sendMessage(Text.translatable(randomKey(DISPLACED_MESSAGES)), true);
			}
			case UNSTABLE -> {
				attemptRewind(player, session, nowNanos, 1_000L, 1_500L);
				sendDistortion(player, TemporalDistortionPayload.MEDIUM, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.42F, 0.8F);
				player.sendMessage(Text.translatable(randomKey(UNSTABLE_MESSAGES)), true);
			}
			case OVERLOAD -> {
				attemptRewind(player, session, nowNanos, 2_000L, 3_000L);
				sendDistortion(player, TemporalDistortionPayload.STRONG, SoundEvents.BLOCK_PORTAL_AMBIENT, 0.5F, 0.7F);
				player.sendMessage(Text.translatable(randomKey(OVERLOAD_MESSAGES)), true);
			}
			case STABLE -> {
				// Handled before scheduling.
			}
		}
	}

	private static boolean attemptRewind(
			ServerPlayerEntity player,
			PlayerSession session,
			long nowNanos,
			long minimumMillisAgo,
			long maximumMillisAgo
	) {
		if (shouldSkipRewind(player)) {
			return false;
		}

		long offsetMillis = ThreadLocalRandom.current().nextLong(minimumMillisAgo, maximumMillisAgo + 1L);
		long latestSampleNanos = nowNanos - offsetMillis * MILLIS_TO_NANOS;
		ServerWorld world = player.getServerWorld();
		Optional<TemporalPositionSample> candidate = session.history.latestAtOrBefore(
				world.getRegistryKey(),
				latestSampleNanos
		);
		if (candidate.isEmpty() || !isSafeRewindTarget(player, world, candidate.get())) {
			return false;
		}

		TemporalPositionSample sample = candidate.get();
		boolean teleported = player.teleport(
				world,
				sample.x(),
				sample.y(),
				sample.z(),
				Set.of(),
				sample.yaw(),
				sample.pitch()
		);
		if (teleported) {
			session.history.discardAfter(sample.sampledAtNanos());
		}
		return teleported;
	}

	private static boolean shouldSkipRewind(ServerPlayerEntity player) {
		return !player.isAlive()
				|| player.isDead()
				|| player.isSpectator()
				|| player.isSleeping()
				|| player.hasVehicle()
				|| player.isInTeleportationState()
				|| player.hasPortalCooldown()
				|| player.isFallFlying()
				|| player.getServer().getTickManager().isSprinting();
	}

	private static boolean isSafeRewindTarget(
			ServerPlayerEntity player,
			ServerWorld world,
			TemporalPositionSample sample
	) {
		if (!world.getRegistryKey().equals(sample.dimension())) {
			return false;
		}

		BlockPos targetPos = BlockPos.ofFloored(sample.x(), sample.y(), sample.z());
		if (targetPos.getY() < world.getBottomY() || targetPos.getY() >= world.getTopY()) {
			return false;
		}
		if (!world.isChunkLoaded(ChunkPos.toLong(targetPos))) {
			return false;
		}

		Box targetBox = player.getBoundingBox().offset(
				sample.x() - player.getX(),
				sample.y() - player.getY(),
				sample.z() - player.getZ()
		);
		if (!world.getWorldBorder().contains(targetBox) || !world.isSpaceEmpty(player, targetBox)) {
			return false;
		}

		boolean supported = world.getBlockCollisions(player, targetBox.offset(0.0D, -0.15D, 0.0D))
				.iterator()
				.hasNext();
		boolean inFluid = !world.getFluidState(targetPos).isEmpty()
				|| !world.getFluidState(BlockPos.ofFloored(sample.x(), sample.y() + 0.5D, sample.z())).isEmpty();
		return supported || inFluid;
	}

	private static void sendDistortion(
			ServerPlayerEntity player,
			byte strength,
			SoundEvent sound,
			float volume,
			float pitch
	) {
		if (ServerPlayNetworking.canSend(player, TemporalDistortionPayload.ID)) {
			ServerPlayNetworking.send(player, new TemporalDistortionPayload(strength));
		}
		player.playSoundToPlayer(sound, SoundCategory.PLAYERS, volume, pitch);
	}

	private static void updateSessionStage(PlayerSession session, TemporalOverloadStage stage, long nowNanos) {
		if (session.stage == stage) {
			return;
		}
		session.stage = stage;
		session.suppressedBySprint = false;
		if (stage == TemporalOverloadStage.STABLE) {
			session.nextEventNanos = 0L;
			return;
		}
		scheduleNextEvent(session, stage, nowNanos);
	}

	private static void scheduleNextEvent(PlayerSession session, TemporalOverloadStage stage, long nowNanos) {
		session.nextEventNanos = nowNanos + eventDelayNanos(stage);
	}

	private static long eventDelayNanos(TemporalOverloadStage stage) {
		long seconds = switch (stage) {
			case DISPLACED -> ThreadLocalRandom.current().nextLong(25L, 41L);
			case UNSTABLE -> ThreadLocalRandom.current().nextLong(12L, 21L);
			case OVERLOAD -> ThreadLocalRandom.current().nextLong(6L, 13L);
			case STABLE -> 0L;
		};
		return seconds * 1_000_000_000L;
	}

	private static String randomKey(String[] keys) {
		return keys[ThreadLocalRandom.current().nextInt(keys.length)];
	}

	private static ServerState stateFor(MinecraftServer server) {
		return STATES.computeIfAbsent(server, ignored -> new ServerState());
	}

	private static void clearPlayerSession(ServerPlayerEntity player) {
		ServerState state = STATES.get(player.getServer());
		if (state != null) {
			state.sessions.remove(player.getUuid());
		}
	}

	private static void clearServerState(MinecraftServer server) {
		STATES.remove(server);
	}

	private static final class ServerState {
		private final Map<UUID, PlayerSession> sessions = new HashMap<>();
		private long nextProcessNanos;
	}

	private static final class PlayerSession {
		private final PlayerTemporalHistory history = new PlayerTemporalHistory();
		private TemporalOverloadStage stage = TemporalOverloadStage.STABLE;
		private long lastSampleNanos;
		private long nextEventNanos;
		private boolean suppressedBySprint;
	}
}
