package com.shouyun.wilddogmilk.time.sideeffect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.MathHelper;

/** Persistent, immutable player state. Transient timelines belong in the manager. */
public record TemporalOverloadData(int load, long lastUpdateEpochMillis) {
	public static final TemporalOverloadData EMPTY = new TemporalOverloadData(0, 0L);

	public static final Codec<TemporalOverloadData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("load", 0).forGetter(TemporalOverloadData::load),
			Codec.LONG.optionalFieldOf("last_update_epoch_millis", 0L)
					.forGetter(TemporalOverloadData::lastUpdateEpochMillis)
	).apply(instance, TemporalOverloadData::new));

	public static final PacketCodec<RegistryByteBuf, TemporalOverloadData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			TemporalOverloadData::load,
			PacketCodecs.VAR_LONG,
			TemporalOverloadData::lastUpdateEpochMillis,
			TemporalOverloadData::new
	);

	public TemporalOverloadData {
		load = MathHelper.clamp(load, 0, 100);
		lastUpdateEpochMillis = Math.max(0L, lastUpdateEpochMillis);
	}

	public static TemporalOverloadData emptyAt(long epochMillis) {
		return new TemporalOverloadData(0, epochMillis);
	}

	public TemporalOverloadStage stage() {
		return TemporalOverloadStage.fromLoad(load);
	}
}
