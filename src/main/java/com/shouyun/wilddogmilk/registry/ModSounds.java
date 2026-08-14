package com.shouyun.wilddogmilk.registry;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final SoundEvent TIME_ACCELERATION_MUSIC = register("time_acceleration_music");

	private ModSounds() {
	}

	public static void register() {
		YuexiWildDogMilk.LOGGER.info("Registered Yuexi Wild Dog Milk sounds.");
	}

	private static SoundEvent register(String path) {
		Identifier id = YuexiWildDogMilk.id(path);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}
}
