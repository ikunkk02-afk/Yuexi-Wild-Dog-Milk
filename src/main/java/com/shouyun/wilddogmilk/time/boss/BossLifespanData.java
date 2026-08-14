package com.shouyun.wilddogmilk.time.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent mutable attachment state for one supported boss. */
public final class BossLifespanData {
	public static final Codec<BossLifespanData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("age_ticks").forGetter(BossLifespanData::ageTicks),
			Codec.BOOL.optionalFieldOf("expiration_started", false).forGetter(BossLifespanData::expirationStarted)
	).apply(instance, BossLifespanData::new));

	private int ageTicks;
	private boolean expirationStarted;

	public BossLifespanData() {
		this(0, false);
	}

	private BossLifespanData(int ageTicks, boolean expirationStarted) {
		this.ageTicks = Math.max(0, ageTicks);
		this.expirationStarted = expirationStarted;
	}

	public int ageTicks() {
		return ageTicks;
	}

	public boolean expirationStarted() {
		return expirationStarted;
	}

	/** @return true exactly once, when this boss reaches its maximum lifespan. */
	public boolean advance(int maximumAgeTicks) {
		if (expirationStarted) return false;
		if (ageTicks < maximumAgeTicks) ageTicks++;
		if (ageTicks >= maximumAgeTicks) {
			expirationStarted = true;
			return true;
		}
		return false;
	}
}
