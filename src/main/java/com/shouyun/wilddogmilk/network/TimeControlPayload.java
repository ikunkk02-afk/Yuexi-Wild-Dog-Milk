package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * A compact action id is deliberately the only client-controlled value.
 * {@link TimeControlAction#fromId(byte)} validates it again on the server.
 */
public record TimeControlPayload(byte actionId) implements CustomPayload {
	public static final Id<TimeControlPayload> ID = new Id<>(YuexiWildDogMilk.id("time_control"));
	public static final PacketCodec<RegistryByteBuf, TimeControlPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BYTE,
			TimeControlPayload::actionId,
			TimeControlPayload::new
	);

	@Override
	public Id<TimeControlPayload> getId() {
		return ID;
	}
}
