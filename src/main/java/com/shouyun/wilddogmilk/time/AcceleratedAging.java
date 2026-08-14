package com.shouyun.wilddogmilk.time;

import com.mojang.serialization.Codec;
import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.registry.ModDamageTypes;
import com.shouyun.wilddogmilk.time.aging.AgingStage;
import com.shouyun.wilddogmilk.time.aging.AgingTags;
import com.shouyun.wilddogmilk.time.aging.SpecialAgingBehaviors;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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

	public static final AttachmentType<AgingProgress> ACCELERATED_AGE_TICKS = AttachmentRegistry.create(
			YuexiWildDogMilk.id("accelerated_age_ticks"),
			builder -> builder
					// Keep the first-stage NBT shape as a plain integer while
					// avoiding a setAttached write for every entity tick.
					.persistent(Codec.INT.xmap(AgingProgress::new, AgingProgress::ageTicks))
					.initializer(() -> new AgingProgress(0))
	);

	private AcceleratedAging() {
	}

	public static void tick(ServerWorld world, LivingEntity entity) {
		if (!TimeAccelerationManager.isAccelerating(world.getServer()) || isExcluded(entity) || entity.isBaby()) {
			return;
		}

		int ageTicks = entity.getAttachedOrCreate(ACCELERATED_AGE_TICKS).increment();
		AgingStage stage = AgingStage.fromAgeTicks(ageTicks);

		if (stage == AgingStage.EXPIRED) {
			if (!SpecialAgingBehaviors.handleExpiration(world, entity)) {
				entity.damage(world.getDamageSources().create(ModDamageTypes.EXPIRED), Float.MAX_VALUE);
			}
			return;
		}

		boolean stageStarted = stage.startsAt(ageTicks);
		boolean refreshEffects = shouldRefreshEffects(world, ageTicks);
		boolean particlePulse = shouldSpawnParticle(world, stage, ageTicks, stageStarted);
		if (stage != AgingStage.FRESH && (stageStarted || refreshEffects || particlePulse)) {
			SpecialAgingBehaviors.handleStage(world, entity, stage, stageStarted, particlePulse);
		}
	}

	private static boolean isExcluded(LivingEntity entity) {
		return entity instanceof PlayerEntity || entity.getType().isIn(AgingTags.AGING_IMMUNE);
	}

	private static boolean shouldRefreshEffects(ServerWorld world, int ageTicks) {
		return isStageBoundary(ageTicks) || ageTicks % getEffectRefreshInterval(world) == 0;
	}

	private static boolean shouldSpawnParticle(ServerWorld world, AgingStage stage, int ageTicks, boolean stageStarted) {
		return (stage == AgingStage.AGING || stage == AgingStage.SEVERE)
				&& (stageStarted || ageTicks % getParticleInterval(world) == 0);
	}

	private static boolean isStageBoundary(int ageTicks) {
		return ageTicks == MILD_AGING_TICKS || ageTicks == AGING_TICKS || ageTicks == SEVERE_AGING_TICKS;
	}

	private static int getEffectRefreshInterval(ServerWorld world) {
		return Math.max(20, (int) Math.ceil(TimeAccelerationManager.getAgingTickRate(world.getServer()) / 20.0F));
	}

	private static int getParticleInterval(ServerWorld world) {
		return Math.max(200, (int) Math.ceil(TimeAccelerationManager.getAgingTickRate(world.getServer()) / 5.0F));
	}

	/**
	 * The attachment retains this object while an entity is loaded. Its codec
	 * still serializes a single integer, so old worlds remain compatible.
	 */
	public static final class AgingProgress {
		private int ageTicks;

		private AgingProgress(int ageTicks) {
			this.ageTicks = ageTicks;
		}

		private int increment() {
			return ++ageTicks;
		}

		private int ageTicks() {
			return ageTicks;
		}
	}
}
