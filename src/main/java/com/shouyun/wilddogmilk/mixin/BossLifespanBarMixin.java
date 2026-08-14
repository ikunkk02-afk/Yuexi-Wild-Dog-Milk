package com.shouyun.wilddogmilk.mixin;

import com.shouyun.wilddogmilk.time.boss.BossLifespanBarHolder;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores the transient lifetime BossBar on the boss itself, not in a map. */
@Mixin({EnderDragonEntity.class, WitherEntity.class})
public abstract class BossLifespanBarMixin implements BossLifespanBarHolder {
	@Unique private ServerBossBar yuexiWildDogMilk$lifespanBar;

	@Override
	public ServerBossBar yuexiWildDogMilk$getOrCreateLifespanBar(Text initialName) {
		if (yuexiWildDogMilk$lifespanBar == null) {
			yuexiWildDogMilk$lifespanBar = new ServerBossBar(initialName, BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
		}
		return yuexiWildDogMilk$lifespanBar;
	}

	@Override
	public ServerBossBar yuexiWildDogMilk$getLifespanBar() {
		return yuexiWildDogMilk$lifespanBar;
	}

	@Override
	public void yuexiWildDogMilk$clearLifespanBar() {
		if (yuexiWildDogMilk$lifespanBar != null) {
			yuexiWildDogMilk$lifespanBar.clearPlayers();
			yuexiWildDogMilk$lifespanBar = null;
		}
	}
}
