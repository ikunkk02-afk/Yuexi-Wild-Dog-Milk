package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * A boolean is deliberately the only client-controlled value: false cycles the
 * fixed server-owned rates, true requests the emergency reset.
 */
public record TimeRatePayload(boolean reset) implements CustomPayload {
	public static final Id<TimeRatePayload> ID = new Id<>(YuexiWildDogMilk.id("time_rate"));
	public static final PacketCodec<RegistryByteBuf, TimeRatePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL,
			TimeRatePayload::reset,
			TimeRatePayload::new
	);

	@Override
	public Id<TimeRatePayload> getId() {
		return ID;
	}
}
