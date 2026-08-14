package com.shouyun.wilddogmilk.time.aging;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Maps supported vanilla entity types to small, self-contained aging outcomes.
 * Unknown entities deliberately keep the ordinary shelf-life behavior.
 */
public final class SpecialAgingBehaviors {
	private static final AgingBehavior DEFAULT = new DefaultAgingBehavior();
	private static final AgingBehavior ZOMBIE = new ZombieAgingBehavior();
	private static final AgingBehavior SKELETON = new SkeletonAgingBehavior();
	private static final AgingBehavior SLIME = new SlimeAgingBehavior();
	private static final AgingBehavior MAGMA_CUBE = new MagmaCubeAgingBehavior();
	private static final AgingBehavior ENDERMAN = new EndermanAgingBehavior();
	private static final AgingBehavior PHANTOM = new PhantomAgingBehavior();
	private static final AgingBehavior BLAZE = new BlazeAgingBehavior();
	private static final AgingBehavior SNOW_GOLEM = new SnowGolemAgingBehavior();
	private static final AgingBehavior IRON_GOLEM = new IronGolemAgingBehavior();

	private static final Map<EntityType<?>, AgingBehavior> BEHAVIORS = Map.ofEntries(
			behavior(EntityType.ZOMBIE, ZOMBIE),
			behavior(EntityType.HUSK, ZOMBIE),
			behavior(EntityType.DROWNED, ZOMBIE),
			behavior(EntityType.ZOMBIE_VILLAGER, ZOMBIE),
			behavior(EntityType.SKELETON, SKELETON),
			behavior(EntityType.STRAY, SKELETON),
			behavior(EntityType.BOGGED, SKELETON),
			behavior(EntityType.WITHER_SKELETON, SKELETON),
			behavior(EntityType.SLIME, SLIME),
			behavior(EntityType.MAGMA_CUBE, MAGMA_CUBE),
			behavior(EntityType.ENDERMAN, ENDERMAN),
			behavior(EntityType.PHANTOM, PHANTOM),
			behavior(EntityType.BLAZE, BLAZE),
			behavior(EntityType.SNOW_GOLEM, SNOW_GOLEM),
			behavior(EntityType.IRON_GOLEM, IRON_GOLEM)
	);

	private SpecialAgingBehaviors() {
	}

	public static void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
		behaviorFor(entity).handleStage(world, entity, stage, stageStarted, particlePulse);
	}

	/**
	 * Special expiration always uses discard after its own drops, so vanilla loot,
	 * XP, player kill credit, and slime splitting cannot run.
	 */
	public static boolean handleExpiration(ServerWorld world, LivingEntity entity) {
		return behaviorFor(entity).handleExpiration(world, entity);
	}

	private static AgingBehavior behaviorFor(LivingEntity entity) {
		return BEHAVIORS.getOrDefault(entity.getType(), DEFAULT);
	}

	private static Map.Entry<EntityType<?>, AgingBehavior> behavior(EntityType<?> entityType, AgingBehavior agingBehavior) {
		return Map.entry(entityType, agingBehavior);
	}

	private static void applyStandardEffects(LivingEntity entity, AgingStage stage) {
		if (stage == AgingStage.FRESH) {
			return;
		}

		int amplifier = stage == AgingStage.SEVERE ? 1 : 0;
		entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, amplifier, true, false, false));
		if (stage == AgingStage.AGING || stage == AgingStage.SEVERE) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, amplifier, true, false, false));
		}
	}

	private static void spawnParticles(ServerWorld world, LivingEntity entity, ParticleEffect particle, int count, double spread) {
		world.spawnParticles(
				particle,
				entity.getX(),
				entity.getY() + entity.getHeight() * 0.5D,
				entity.getZ(),
				count,
				spread, spread, spread,
				0.0D
		);
	}

	private static void playSound(ServerWorld world, LivingEntity entity, net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.HOSTILE, volume, pitch);
	}

	private static int count(boolean stageStarted, int stageCount, int pulseCount) {
		return stageStarted ? stageCount : pulseCount;
	}

	private static boolean isParticleEvent(boolean stageStarted, boolean particlePulse) {
		return stageStarted || particlePulse;
	}

	private static void shrink(SlimeEntity slime) {
		if (slime.getSize() > 1) {
			slime.setSize(slime.getSize() - 1, true);
		}
	}

	private static class StandardAgingBehavior implements AgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			applyStandardEffects(entity, stage);
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			return false;
		}
	}

	private static final class DefaultAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stage == AgingStage.SEVERE && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.ASH, count(stageStarted, 3, 1), 0.18D);
			}
		}
	}

	private static final class ZombieAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stage == AgingStage.SEVERE && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.ASH, count(stageStarted, 6, 2), 0.24D);
				spawnParticles(world, entity, ParticleTypes.SMOKE, count(stageStarted, 4, 1), 0.20D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			SkeletonEntity skeleton = EntityType.SKELETON.create(world);
			if (skeleton == null) {
				return true;
			}

			skeleton.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
			skeleton.setCustomName(entity.getCustomName());
			skeleton.setCustomNameVisible(entity.isCustomNameVisible());
			if (!world.spawnEntity(skeleton)) {
				return true;
			}

			spawnParticles(world, entity, ParticleTypes.ASH, 8, 0.25D);
			spawnParticles(world, entity, ParticleTypes.CLOUD, 6, 0.20D);
			playSound(world, entity, SoundEvents.BLOCK_BONE_BLOCK_BREAK, 0.9F, 0.85F);
			entity.discard();
			return true;
		}
	}

	private static final class SkeletonAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stage == AgingStage.SEVERE && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.ASH, count(stageStarted, 6, 2), 0.20D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			entity.dropItem(Items.BONE_MEAL, 2 + world.getRandom().nextInt(4));
			spawnParticles(world, entity, ParticleTypes.CLOUD, 8, 0.22D);
			spawnParticles(world, entity, ParticleTypes.ASH, 8, 0.22D);
			playSound(world, entity, SoundEvents.BLOCK_BONE_BLOCK_BREAK, 0.9F, 1.1F);
			entity.discard();
			return true;
		}
	}

	private static final class SlimeAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stageStarted && (stage == AgingStage.AGING || stage == AgingStage.SEVERE) && entity instanceof SlimeEntity slime) {
				shrink(slime);
				spawnParticles(world, entity, ParticleTypes.ITEM_SLIME, 4, 0.16D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			spawnParticles(world, entity, ParticleTypes.ITEM_SLIME, 8, 0.22D);
			playSound(world, entity, SoundEvents.ENTITY_SLIME_SQUISH, 0.7F, 0.65F);
			entity.discard();
			return true;
		}
	}

	private static final class MagmaCubeAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stageStarted && (stage == AgingStage.AGING || stage == AgingStage.SEVERE) && entity instanceof MagmaCubeEntity magmaCube) {
				shrink(magmaCube);
			}
			if (stage == AgingStage.SEVERE && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.SMOKE, count(stageStarted, 5, 1), 0.20D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			int magmaCream = world.getRandom().nextInt(3);
			if (magmaCream > 0) {
				entity.dropItem(Items.MAGMA_CREAM, magmaCream);
			}
			spawnParticles(world, entity, ParticleTypes.LARGE_SMOKE, 8, 0.22D);
			playSound(world, entity, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.9F, 0.8F);
			entity.discard();
			return true;
		}
	}

	private static final class EndermanAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if ((stage == AgingStage.AGING || stage == AgingStage.SEVERE) && isParticleEvent(stageStarted, particlePulse)) {
				int particles = stage == AgingStage.SEVERE ? count(stageStarted, 8, 3) : count(stageStarted, 4, 1);
				spawnParticles(world, entity, ParticleTypes.PORTAL, particles, 0.22D);
			}
			if (stage == AgingStage.SEVERE && stageStarted && world.getRandom().nextInt(4) == 0) {
				playSound(world, entity, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.45F, 0.85F);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			spawnParticles(world, entity, ParticleTypes.PORTAL, 24, 0.35D);
			playSound(world, entity, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
			if (world.getRandom().nextInt(4) == 0) {
				entity.dropItem(Items.ENDER_PEARL);
			}
			entity.discard();
			return true;
		}
	}

	private static final class PhantomAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if ((stage == AgingStage.AGING || stage == AgingStage.SEVERE) && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.ASH, stage == AgingStage.SEVERE ? count(stageStarted, 6, 2) : count(stageStarted, 3, 1), 0.22D);
				if (stage == AgingStage.SEVERE) {
					spawnParticles(world, entity, ParticleTypes.SMOKE, count(stageStarted, 4, 1), 0.18D);
				}
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			spawnParticles(world, entity, ParticleTypes.ASH, 10, 0.25D);
			spawnParticles(world, entity, ParticleTypes.SMOKE, 6, 0.20D);
			playSound(world, entity, SoundEvents.ENTITY_PHANTOM_DEATH, 0.8F, 0.9F);
			if (world.getRandom().nextInt(10) < 3) {
				entity.dropItem(Items.PHANTOM_MEMBRANE);
			}
			entity.discard();
			return true;
		}
	}

	private static final class BlazeAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if (stage == AgingStage.AGING && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.SMOKE, count(stageStarted, 4, 1), 0.20D);
			}
			if (stage == AgingStage.SEVERE && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.LARGE_SMOKE, count(stageStarted, 6, 2), 0.22D);
				spawnParticles(world, entity, ParticleTypes.SMOKE, count(stageStarted, 4, 1), 0.20D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			spawnParticles(world, entity, ParticleTypes.LARGE_SMOKE, 12, 0.28D);
			spawnParticles(world, entity, ParticleTypes.SMOKE, 6, 0.22D);
			playSound(world, entity, SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.75F);
			if (world.getRandom().nextInt(4) == 0) {
				entity.dropItem(Items.BLAZE_ROD);
			}
			entity.discard();
			return true;
		}
	}

	private static final class SnowGolemAgingBehavior extends StandardAgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			super.handleStage(world, entity, stage, stageStarted, particlePulse);
			if ((stage == AgingStage.AGING || stage == AgingStage.SEVERE) && isParticleEvent(stageStarted, particlePulse)) {
				spawnParticles(world, entity, ParticleTypes.DRIPPING_WATER, stage == AgingStage.SEVERE ? count(stageStarted, 5, 2) : count(stageStarted, 3, 1), 0.18D);
				spawnParticles(world, entity, ParticleTypes.SNOWFLAKE, stage == AgingStage.SEVERE ? count(stageStarted, 5, 2) : count(stageStarted, 3, 1), 0.18D);
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			entity.dropItem(Items.SNOWBALL, 2 + world.getRandom().nextInt(5));
			spawnParticles(world, entity, ParticleTypes.SNOWFLAKE, 10, 0.25D);
			spawnParticles(world, entity, ParticleTypes.CLOUD, 6, 0.20D);
			playSound(world, entity, SoundEvents.BLOCK_SNOW_BREAK, 0.9F, 0.85F);
			entity.discard();
			return true;
		}
	}

	private static final class IronGolemAgingBehavior implements AgingBehavior {
		@Override
		public void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse) {
			switch (stage) {
				case MILD -> entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, false));
				case AGING -> {
					entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false, false));
					entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, false, false));
				}
				case SEVERE -> {
					entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, true, false, false));
					entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, true, false, false));
					if (isParticleEvent(stageStarted, particlePulse)) {
						spawnParticles(world, entity, ParticleTypes.ASH, count(stageStarted, 4, 1), 0.18D);
					}
				}
				default -> {
				}
			}
		}

		@Override
		public boolean handleExpiration(ServerWorld world, LivingEntity entity) {
			return false;
		}
	}
}
