package com.shouyun.wilddogmilk.time.aging;

import com.shouyun.wilddogmilk.time.AcceleratedAging;

/**
 * The stable four-stage shelf-life progression used by accelerated aging.
 */
public enum AgingStage {
	FRESH(0),
	MILD(AcceleratedAging.MILD_AGING_TICKS),
	AGING(AcceleratedAging.AGING_TICKS),
	SEVERE(AcceleratedAging.SEVERE_AGING_TICKS),
	EXPIRED(AcceleratedAging.EXPIRATION_TICKS);

	private final int startsAt;

	AgingStage(int startsAt) {
		this.startsAt = startsAt;
	}

	public static AgingStage fromAgeTicks(int ageTicks) {
		if (ageTicks >= EXPIRED.startsAt) {
			return EXPIRED;
		}
		if (ageTicks >= SEVERE.startsAt) {
			return SEVERE;
		}
		if (ageTicks >= AGING.startsAt) {
			return AGING;
		}
		if (ageTicks >= MILD.startsAt) {
			return MILD;
		}
		return FRESH;
	}

	public boolean startsAt(int ageTicks) {
		return ageTicks == startsAt;
	}
}
