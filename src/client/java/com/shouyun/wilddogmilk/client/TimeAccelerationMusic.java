package com.shouyun.wilddogmilk.client;

import com.shouyun.wilddogmilk.network.TimeStatePayload;
import com.shouyun.wilddogmilk.registry.ModSounds;
import com.shouyun.wilddogmilk.time.SprintMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

public final class TimeAccelerationMusic {
	private static final float NORMAL_TICK_RATE = 20.0F;
	private static SoundInstance music;

	private TimeAccelerationMusic() {
	}

	public static void update(MinecraftClient client, TimeStatePayload timeState) {
		if (!shouldPlay(client, timeState)) {
			stop(client);
			return;
		}

		if (music == null) {
			start(client);
		}
	}

	public static void stop(MinecraftClient client) {
		if (music == null) {
			return;
		}

		client.getSoundManager().stop(music);
		music = null;
	}

	private static boolean shouldPlay(MinecraftClient client, TimeStatePayload timeState) {
		if (client.player == null || client.world == null) {
			return false;
		}

		boolean frozen = timeState.frozen();
		float tickRate = timeState.tickRate();
		boolean sprinting = timeState.sprintMode() != SprintMode.NONE;
		return !frozen && (tickRate > NORMAL_TICK_RATE || sprinting);
	}

	private static void start(MinecraftClient client) {
		stop(client);
		client.getMusicTracker().stop();
		music = new PositionedSoundInstance(
				ModSounds.TIME_ACCELERATION_MUSIC.getId(),
				SoundCategory.MUSIC,
				1.0F,
				1.0F,
				SoundInstance.createRandom(),
				true,
				0,
				SoundInstance.AttenuationType.NONE,
				0.0,
				0.0,
				0.0,
				true
		);
		client.getSoundManager().play(music);
	}
}
