package com.shouyun.wilddogmilk.time.aging;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * A focused lifecycle hook for entity-specific aging visuals and expiration.
 */
public interface AgingBehavior {
	void handleStage(ServerWorld world, LivingEntity entity, AgingStage stage, boolean stageStarted, boolean particlePulse);

	/**
	 * @return {@code true} when this behavior owns expiration, including a
	 * retryable transformation that could not yet spawn its replacement.
	 */
	boolean handleExpiration(ServerWorld world, LivingEntity entity);
}
