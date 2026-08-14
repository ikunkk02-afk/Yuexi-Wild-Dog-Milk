package com.shouyun.wilddogmilk.mixin;

import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import net.minecraft.server.ServerTickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ServerTickManager owns the actual Tick Sprint loop. We only observe its
 * private completion point so HUD state and player feedback cannot desync.
 */
@Mixin(ServerTickManager.class)
public abstract class ServerTickManagerMixin {
	@Inject(method = "finishSprinting", at = @At("TAIL"))
	private void yuexiWildDogMilk$onSprintFinished(CallbackInfo ci) {
		TimeAccelerationManager.onSprintFinished((ServerTickManager) (Object) this);
	}
}
