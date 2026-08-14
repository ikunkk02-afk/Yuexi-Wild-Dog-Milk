package com.shouyun.wilddogmilk.time.boss;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import com.shouyun.wilddogmilk.registry.ModDamageTypes;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/** Independent time-lifespan system for bosses excluded from normal aging. */
public final class BossAgingManager {
	public static final int DRAGON_MAX_AGE_TICKS = 60_000;
	public static final int WITHER_MAX_AGE_TICKS = 120_000;
	private static final int YEARS_PER_AGE_TICK = 10;

	public static final AttachmentType<BossLifespanData> BOSS_AGE_TICKS = AttachmentRegistry.create(
			YuexiWildDogMilk.id("boss_age_ticks"),
			builder -> builder.persistent(BossLifespanData.CODEC).initializer(BossLifespanData::new)
	);

	private BossAgingManager() {
	}

	public static void register() {
		EntityTrackingEvents.START_TRACKING.register(BossAgingManager::onStartedTracking);
		EntityTrackingEvents.STOP_TRACKING.register(BossAgingManager::onStoppedTracking);
	}

	public static void tick(ServerWorld world, LivingEntity entity) {
		int maximumAgeTicks = maximumAgeTicks(entity);
		if (maximumAgeTicks == 0 || !TimeAccelerationManager.isAccelerating(world.getServer())) return;

		BossLifespanData data = entity.getAttachedOrCreate(BOSS_AGE_TICKS);
		BossAgingStage stageBefore = stage(data, maximumAgeTicks);
		boolean expiredNow = data.advance(maximumAgeTicks);
		BossAgingStage stageAfter = stage(data, maximumAgeTicks);
		boolean stageChanged = stageBefore != stageAfter;

		if (stageAfter != BossAgingStage.ANCIENT
				&& (stageChanged || data.ageTicks() % particleRefreshInterval(world) == 0)) {
			spawnStageParticles(world, entity, stageAfter, stageChanged);
		}
		if (stageChanged || expiredNow || data.ageTicks() % barRefreshInterval(world) == 0) {
			refreshBar(entity, data, maximumAgeTicks, stageAfter);
		}
		if (expiredNow) {
			entity.damage(world.getDamageSources().create(ModDamageTypes.TIME_EXPIRED), Float.MAX_VALUE);
		}
	}

	public static void onRemoved(Entity entity) {
		if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) {
			((BossLifespanBarHolder) entity).yuexiWildDogMilk$clearLifespanBar();
		}
	}

	private static void onStartedTracking(Entity trackedEntity, ServerPlayerEntity player) {
		if (!(trackedEntity instanceof LivingEntity boss) || !PermanentShelfLifeData.has(player)) return;
		int maximumAgeTicks = maximumAgeTicks(boss);
		if (maximumAgeTicks == 0) return;

		BossLifespanData data = boss.getAttachedOrCreate(BOSS_AGE_TICKS);
		refreshBar(boss, data, maximumAgeTicks, stage(data, maximumAgeTicks));
		getOrCreateBar(boss, data, maximumAgeTicks).addPlayer(player);
	}

	private static void onStoppedTracking(Entity trackedEntity, ServerPlayerEntity player) {
		if (trackedEntity instanceof EnderDragonEntity || trackedEntity instanceof WitherEntity) {
			ServerBossBar bar = ((BossLifespanBarHolder) trackedEntity).yuexiWildDogMilk$getLifespanBar();
			if (bar != null) bar.removePlayer(player);
		}
	}

	private static int maximumAgeTicks(LivingEntity entity) {
		if (entity instanceof EnderDragonEntity) return DRAGON_MAX_AGE_TICKS;
		if (entity instanceof WitherEntity) return WITHER_MAX_AGE_TICKS;
		return 0;
	}

	private static int remainingTicks(BossLifespanData data, int maximumAgeTicks) {
		return Math.max(0, maximumAgeTicks - data.ageTicks());
	}

	private static BossAgingStage stage(BossLifespanData data, int maximumAgeTicks) {
		return BossAgingStage.fromRemaining(remainingTicks(data, maximumAgeTicks), maximumAgeTicks);
	}

	private static void refreshBar(LivingEntity entity, BossLifespanData data, int maximumAgeTicks, BossAgingStage stage) {
		int remainingTicks = remainingTicks(data, maximumAgeTicks);
		ServerBossBar bar = getOrCreateBar(entity, data, maximumAgeTicks);
		bar.setPercent((float) remainingTicks / maximumAgeTicks);
		bar.setColor(stage.color());
		bar.setName(lifespanText(entity, remainingTicks));
	}

	private static ServerBossBar getOrCreateBar(LivingEntity entity, BossLifespanData data, int maximumAgeTicks) {
		return ((BossLifespanBarHolder) entity).yuexiWildDogMilk$getOrCreateLifespanBar(
				lifespanText(entity, remainingTicks(data, maximumAgeTicks))
		);
	}

	private static Text lifespanText(LivingEntity entity, int remainingTicks) {
		long remainingYears = (long) remainingTicks * YEARS_PER_AGE_TICK;
		String key = entity instanceof EnderDragonEntity
				? "bossbar.yuexi-wild-dog-milk.ender_dragon_lifespan"
				: "bossbar.yuexi-wild-dog-milk.wither_lifespan";
		return Text.translatable(key, remainingYears);
	}

	private static int barRefreshInterval(ServerWorld world) {
		return Math.max(100, (int) Math.ceil(TimeAccelerationManager.getAgingTickRate(world.getServer()) / 5.0F));
	}

	private static int particleRefreshInterval(ServerWorld world) {
		return Math.max(100, (int) Math.ceil(TimeAccelerationManager.getAgingTickRate(world.getServer()) / 2.0F));
	}

	private static void spawnStageParticles(ServerWorld world, LivingEntity entity, BossAgingStage stage, boolean stageChanged) {
		if (entity instanceof EnderDragonEntity) {
			switch (stage) {
				case TIME_WORN -> spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 3 : 1, 0.7D);
				case SEVERELY_WORN -> {
					spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 6 : 2, 0.9D);
					spawnParticles(world, entity, ParticleTypes.PORTAL, stageChanged ? 4 : 1, 0.7D);
				}
				case FADING -> {
					spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 10 : 3, 1.1D);
					spawnParticles(world, entity, ParticleTypes.PORTAL, stageChanged ? 8 : 2, 0.9D);
				}
				default -> { }
			}
			return;
		}

		switch (stage) {
			case TIME_WORN -> spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 3 : 1, 0.45D);
			case SEVERELY_WORN -> {
				spawnParticles(world, entity, ParticleTypes.SMOKE, stageChanged ? 5 : 2, 0.55D);
				spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 4 : 1, 0.45D);
			}
			case FADING -> {
				spawnParticles(world, entity, ParticleTypes.SOUL, stageChanged ? 8 : 2, 0.65D);
				spawnParticles(world, entity, ParticleTypes.SMOKE, stageChanged ? 6 : 2, 0.60D);
				spawnParticles(world, entity, ParticleTypes.ASH, stageChanged ? 5 : 1, 0.55D);
			}
			default -> { }
		}
	}

	private static void spawnParticles(ServerWorld world, LivingEntity entity, ParticleEffect particle, int count, double spread) {
		world.spawnParticles(particle, entity.getX(), entity.getY() + entity.getHeight() * 0.5D, entity.getZ(),
				count, spread, spread, spread, 0.0D);
	}
}
