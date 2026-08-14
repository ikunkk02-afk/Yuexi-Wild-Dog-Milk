package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** One-shot server-to-client visual event; it never carries player state. */
public record TemporalDistortionPayload(byte strength) implements CustomPayload {
	public static final byte LIGHT = 1;
	public static final byte MEDIUM = 2;
	public static final byte STRONG = 3;

	public static final Id<TemporalDistortionPayload> ID = new Id<>(
			YuexiWildDogMilk.id("temporal_distortion")
	);
	public static final PacketCodec<RegistryByteBuf, TemporalDistortionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BYTE,
			TemporalDistortionPayload::strength,
			TemporalDistortionPayload::new
	);

	public TemporalDistortionPayload {
		if (strength < LIGHT || strength > STRONG) {
			throw new IllegalArgumentException("Unknown temporal distortion strength: " + strength);
		}
	}

	@Override
	public Id<TemporalDistortionPayload> getId() {
		return ID;
	}
}
