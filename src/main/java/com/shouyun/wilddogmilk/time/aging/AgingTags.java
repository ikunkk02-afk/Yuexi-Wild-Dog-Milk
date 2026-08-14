package com.shouyun.wilddogmilk.time.aging;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public final class AgingTags {
	public static final TagKey<EntityType<?>> AGING_IMMUNE = TagKey.of(
			RegistryKeys.ENTITY_TYPE,
			YuexiWildDogMilk.id("aging_immune")
	);

	private AgingTags() {
	}
}
