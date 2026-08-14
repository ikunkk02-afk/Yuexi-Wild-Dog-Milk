package com.shouyun.wilddogmilk.time.sideeffect;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

/** Fixed-size history deliberately kept outside attachments and player saves. */
public final class PlayerTemporalHistory {
	public static final int MAX_SAMPLES = 20;

	private final Deque<TemporalPositionSample> samples = new ArrayDeque<>(MAX_SAMPLES);

	public void add(TemporalPositionSample sample) {
		samples.addLast(sample);
		while (samples.size() > MAX_SAMPLES) {
			samples.removeFirst();
		}
	}

	public Optional<TemporalPositionSample> latestAtOrBefore(RegistryKey<World> dimension, long latestNanos) {
		Iterator<TemporalPositionSample> iterator = samples.descendingIterator();
		while (iterator.hasNext()) {
			TemporalPositionSample sample = iterator.next();
			if (sample.sampledAtNanos() <= latestNanos && sample.dimension().equals(dimension)) {
				return Optional.of(sample);
			}
		}
		return Optional.empty();
	}

	public void discardAfter(long nanos) {
		while (!samples.isEmpty() && samples.peekLast().sampledAtNanos() > nanos) {
			samples.removeLast();
		}
	}

	public void clear() {
		samples.clear();
	}
}
