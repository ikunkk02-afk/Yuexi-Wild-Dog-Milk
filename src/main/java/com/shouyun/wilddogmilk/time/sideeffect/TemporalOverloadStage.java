package com.shouyun.wilddogmilk.time.sideeffect;

/** The player-facing severity derived from the persistent temporal load. */
public enum TemporalOverloadStage {
	STABLE,
	DISPLACED,
	UNSTABLE,
	OVERLOAD;

	public static TemporalOverloadStage fromLoad(int load) {
		if (load >= 100) {
			return OVERLOAD;
		}
		if (load >= 75) {
			return UNSTABLE;
		}
		if (load >= 50) {
			return DISPLACED;
		}
		return STABLE;
	}
}
