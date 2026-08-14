package com.shouyun.wilddogmilk.mixin;

import com.shouyun.wilddogmilk.time.boss.BossAgingManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ensures a removed boss cannot retain lifetime-BossBar player references. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "remove", at = @At("HEAD"))
	private void yuexiWildDogMilk$clearLifespanBar(Entity.RemovalReason reason, CallbackInfo ci) {
		BossAgingManager.onRemoved((Entity) (Object) this);
	}
}
