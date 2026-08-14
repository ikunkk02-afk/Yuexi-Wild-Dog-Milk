package com.shouyun.wilddogmilk.time;

import com.mojang.serialization.Codec;
import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.registry.ModDamageTypes;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Tracks only entities currently ticking, so unloaded entities consume neither
 * memory outside their own NBT nor server-wide scan time.
 */
public final class AcceleratedAging {
	public static final int MILD_AGING_TICKS = 500;
	public static final int AGING_TICKS = 1000;
	public static final int SEVERE_AGING_TICKS = 1500;
	public static final int EXPIRATION_TICKS = 2000;
	private static final int EFFECT_REFRESH_INTERVAL = 20;
	private static final int PARTICLE_INTERVAL = 200;

	public static final AttachmentType<Integer> ACCELERATED_AGE_TICKS = AttachmentRegistry.create(
			YuexiWildDogMilk.id("accelerated_age_ticks"),
			builder -> builder
					.persistent(Codec.INT)
					.initializer(() -> 0)
	);

	private AcceleratedAging() {
	}

	public static void tick(ServerWorld world, LivingEntity entity) {
		if (!TimeAccelerationManager.isAccelerating(world.getServer()) || isExcluded(entity) || entity.isBaby()) {
			return;
		}

		int ageTicks = entity.getAttachedOrCreate(ACCELERATED_AGE_TICKS) + 1;
		entity.setAttached(ACCELERATED_AGE_TICKS, ageTicks);

		if (ageTicks == EXPIRATION_TICKS) {
			entity.damage(world.getDamageSources().create(ModDamageTypes.EXPIRED), Float.MAX_VALUE);
			return;
		}

		if (ageTicks >= MILD_AGING_TICKS && (ageTicks == MILD_AGING_TICKS || ageTicks % EFFECT_REFRESH_INTERVAL == 0)) {
			applyAgingEffects(entity, ageTicks);
		}

		if (ageTicks >= SEVERE_AGING_TICKS && ageTicks % PARTICLE_INTERVAL == 0) {
			world.spawnParticles(
					ParticleTypes.ASH,
					entity.getX(),
					entity.getY() + entity.getHeight() * 0.5D,
					entity.getZ(),
					1,
					0.15D, 0.2D, 0.15D,
					0.0D
			);
		}
	}

	private static boolean isExcluded(LivingEntity entity) {
		return entity instanceof PlayerEntity || entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
	}

	private static void applyAgingEffects(LivingEntity entity, int ageTicks) {
		int amplifier = ageTicks >= SEVERE_AGING_TICKS ? 1 : 0;
		entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, amplifier, true, false, false));
		if (ageTicks >= AGING_TICKS) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, amplifier, true, false, false));
		}
	}
}
