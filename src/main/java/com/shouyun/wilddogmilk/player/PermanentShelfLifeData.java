package com.shouyun.wilddogmilk.player;

import com.mojang.serialization.Codec;
import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.registry.ModEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The attachment is the authoritative permanent capability. The status effect is
 * intentionally only a client-visible reminder and is recreated when removed.
 */
public final class PermanentShelfLifeData {
	public static final AttachmentType<Boolean> PERMANENT_SHELF_LIFE = AttachmentRegistry.create(
			YuexiWildDogMilk.id("permanent_shelf_life"),
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
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (has(player)) {
					ensureDisplayEffect(player);
				}
			}
		});
	}

	public static boolean has(ServerPlayerEntity player) {
		return player.getAttachedOrElse(PERMANENT_SHELF_LIFE, false);
	}

	public static void grant(ServerPlayerEntity player) {
		player.setAttached(PERMANENT_SHELF_LIFE, true);
		ensureDisplayEffect(player);
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
}
