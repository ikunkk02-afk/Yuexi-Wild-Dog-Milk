package com.shouyun.wilddogmilk.time.sideeffect;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/** A bounded, in-memory point on one player's personal timeline. */
public record TemporalPositionSample(
		RegistryKey<World> dimension,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		long sampledAtNanos
) {
}
