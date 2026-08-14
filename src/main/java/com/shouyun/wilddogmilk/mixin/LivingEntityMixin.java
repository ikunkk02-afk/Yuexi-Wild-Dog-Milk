package com.shouyun.wilddogmilk.mixin;

import com.shouyun.wilddogmilk.time.AcceleratedAging;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void yuexiWildDogMilk$applyAcceleratedAging(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.isAlive() || !(entity.getWorld() instanceof ServerWorld world)) {
			return;
		}

		AcceleratedAging.tick(world, entity);
	}
}
