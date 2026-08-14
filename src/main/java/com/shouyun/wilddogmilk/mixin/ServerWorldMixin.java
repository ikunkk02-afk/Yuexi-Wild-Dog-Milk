package com.shouyun.wilddogmilk.mixin;

import com.shouyun.wilddogmilk.time.nature.TemporalRandomTickManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hooks vanilla's chunk random-tick entry point without scanning blocks. */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
	@ModifyVariable(method = "tickChunk(Lnet/minecraft/world/chunk/WorldChunk;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int yuexiWildDogMilk$scaleRandomTickBudget(int randomTickSpeed) {
		return TemporalRandomTickManager.scaleRandomTickBudget((ServerWorld) (Object) this, randomTickSpeed);
	}
}
