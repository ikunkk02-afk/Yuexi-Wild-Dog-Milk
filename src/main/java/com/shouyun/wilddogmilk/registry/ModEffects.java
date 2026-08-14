package com.shouyun.wilddogmilk.registry;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.effect.PermanentShelfLifeStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModEffects {
	public static final RegistryEntry.Reference<StatusEffect> PERMANENT_SHELF_LIFE = Registry.registerReference(
			Registries.STATUS_EFFECT,
			YuexiWildDogMilk.id("permanent_shelf_life"),
			new PermanentShelfLifeStatusEffect(StatusEffectCategory.BENEFICIAL, 0x66CC66)
	);

	private ModEffects() {
	}

	public static void register() {
		// Forces class initialization during common mod setup.
	}
}
