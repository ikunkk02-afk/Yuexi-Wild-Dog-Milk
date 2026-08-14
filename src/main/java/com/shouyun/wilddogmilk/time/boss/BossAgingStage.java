package com.shouyun.wilddogmilk.time.boss;

import net.minecraft.entity.boss.BossBar;

public enum BossAgingStage {
	ANCIENT(BossBar.Color.PURPLE),
	TIME_WORN(BossBar.Color.PURPLE),
	SEVERELY_WORN(BossBar.Color.YELLOW),
	FADING(BossBar.Color.RED);

	private final BossBar.Color color;

	BossAgingStage(BossBar.Color color) {
		this.color = color;
	}

	public static BossAgingStage fromRemaining(int remainingTicks, int maximumTicks) {
		long remaining = Math.max(0, remainingTicks);
		if (remaining * 4L > (long) maximumTicks * 3L) return ANCIENT;
		if (remaining * 2L > maximumTicks) return TIME_WORN;
		if (remaining * 4L > maximumTicks) return SEVERELY_WORN;
		return FADING;
	}

	public BossBar.Color color() {
		return color;
	}
}
