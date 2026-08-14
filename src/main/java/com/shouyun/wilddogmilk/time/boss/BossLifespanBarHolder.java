package com.shouyun.wilddogmilk.time.boss;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.text.Text;

/** Implemented directly on Dragon and Wither through a mixin-owned field. */
public interface BossLifespanBarHolder {
	ServerBossBar yuexiWildDogMilk$getOrCreateLifespanBar(Text initialName);

	ServerBossBar yuexiWildDogMilk$getLifespanBar();

	void yuexiWildDogMilk$clearLifespanBar();
}
