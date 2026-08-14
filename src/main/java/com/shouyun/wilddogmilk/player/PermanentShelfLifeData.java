package com.shouyun.wilddogmilk.player;

import com.mojang.serialization.Codec;
import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.registry.ModEffects;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The attachment is the authoritative permanent capability. The status effect is
 * intentionally only a client-visible reminder and is recreated when removed.
 */
public final class PermanentShelfLifeData {
	private static final long DISPLAY_CHECK_INTERVAL_NANOS = 1_000_000_000L;
	private static final Map<MinecraftServer, Long> NEXT_DISPLAY_CHECK = new IdentityHashMap<>();

	public static final AttachmentType<Boolean> PERMANENT_SHELF_LIFE = AttachmentRegistry.create(
			YuexiWildDogMilk.id("permanent_shelf_life"),
			builder -> builder
					.persistent(Codec.BOOL)
					.copyOnDeath()
					.initializer(() -> false)
					.syncWith(PacketCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	public static final AttachmentType<Boolean> DEEP_TIME_POWER = AttachmentRegistry.create(
			YuexiWildDogMilk.id("deep_time_power"),
			builder -> builder
					.persistent(Codec.BOOL)
					.copyOnDeath()
					.initializer(() -> false)
					.syncWith(PacketCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	private PermanentShelfLifeData() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.nanoTime();
			long nextCheck = NEXT_DISPLAY_CHECK.getOrDefault(server, 0L);
			if (now < nextCheck) {
				return;
			}

			NEXT_DISPLAY_CHECK.put(server, now + DISPLAY_CHECK_INTERVAL_NANOS);
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (has(player)) {
					ensureDisplayEffect(player);
				}
			}
		});
		ServerPlayerEvents.JOIN.register(PermanentShelfLifeData::restoreAfterJoin);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> restoreAfterJoin(newPlayer));
		ServerLifecycleEvents.SERVER_STARTED.register(PermanentShelfLifeData::clearDisplayCheck);
		ServerLifecycleEvents.SERVER_STOPPING.register(PermanentShelfLifeData::clearDisplayCheck);
		ServerLifecycleEvents.SERVER_STOPPED.register(PermanentShelfLifeData::clearDisplayCheck);
	}

	public static boolean has(ServerPlayerEntity player) {
		return player.getAttachedOrElse(PERMANENT_SHELF_LIFE, false);
	}

	public static boolean hasDeepTime(ServerPlayerEntity player) {
		return player.getAttachedOrElse(DEEP_TIME_POWER, false);
	}

	public static void grant(ServerPlayerEntity player) {
		boolean changed = grantPermanentIfNeeded(player);
		ensureDisplayEffect(player);
		if (changed) {
			TimeAccelerationManager.syncPlayer(player.getServer(), player);
		}
	}

	public static boolean grantDeepTime(ServerPlayerEntity player) {
		boolean permanentChanged = grantPermanentIfNeeded(player);
		boolean deepTimeChanged = !hasDeepTime(player);
		if (deepTimeChanged) {
			player.setAttached(DEEP_TIME_POWER, true);
		}
		ensureDisplayEffect(player);
		if (permanentChanged || deepTimeChanged) {
			TimeAccelerationManager.syncPlayer(player.getServer(), player);
		}
		return deepTimeChanged;
	}

	public static void ensureDisplayEffect(ServerPlayerEntity player) {
		if (!player.hasStatusEffect(ModEffects.PERMANENT_SHELF_LIFE)) {
			player.addStatusEffect(new StatusEffectInstance(
					ModEffects.PERMANENT_SHELF_LIFE,
					StatusEffectInstance.INFINITE,
					0,
					false,
					false,
					true
			));
		}
	}

	private static void restoreAfterJoin(ServerPlayerEntity player) {
		if (hasDeepTime(player) && !has(player)) {
			player.setAttached(PERMANENT_SHELF_LIFE, true);
		}
		if (has(player)) {
			ensureDisplayEffect(player);
			TimeAccelerationManager.syncPlayer(player.getServer(), player);
		}
	}

	private static boolean grantPermanentIfNeeded(ServerPlayerEntity player) {
		if (has(player)) {
			return false;
		}
		player.setAttached(PERMANENT_SHELF_LIFE, true);
		return true;
	}

	private static void clearDisplayCheck(MinecraftServer server) {
		NEXT_DISPLAY_CHECK.remove(server);
	}
}
