package com.shouyun.wilddogmilk.network;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.time.SprintMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Event-driven server snapshot used by the lightweight client HUD. Vanilla
 * still synchronizes the underlying tick manager independently.
 */
public record TimeStatePayload(float tickRate, boolean frozen, byte sprintModeId) implements CustomPayload {
	public static final Id<TimeStatePayload> ID = new Id<>(YuexiWildDogMilk.id("time_state"));
	public static final PacketCodec<RegistryByteBuf, TimeStatePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			TimeStatePayload::tickRate,
			PacketCodecs.BOOL,
			TimeStatePayload::frozen,
			PacketCodecs.BYTE,
			TimeStatePayload::sprintModeId,
			TimeStatePayload::new
	);

	public TimeStatePayload {
		SprintMode.requireValid(sprintModeId);
	}

	public SprintMode sprintMode() {
		return SprintMode.requireValid(sprintModeId);
	}

	@Override
	public Id<TimeStatePayload> getId() {
		return ID;
	}
}
