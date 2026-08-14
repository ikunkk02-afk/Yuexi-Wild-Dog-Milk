package com.shouyun.wilddogmilk.registry;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModDamageTypes {
	public static final RegistryKey<DamageType> EXPIRED = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, YuexiWildDogMilk.id("expired"));

	private ModDamageTypes() {
	}
}
