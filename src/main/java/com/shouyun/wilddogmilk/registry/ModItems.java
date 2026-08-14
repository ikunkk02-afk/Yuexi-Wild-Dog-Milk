package com.shouyun.wilddogmilk.registry;

import com.shouyun.wilddogmilk.YuexiWildDogMilk;
import com.shouyun.wilddogmilk.item.CenturyAgedWildDogMilkItem;
import com.shouyun.wilddogmilk.item.DogMilkBucketItem;
import com.shouyun.wilddogmilk.item.WildDogMilkItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.util.Rarity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	public static final Item DOG_MILK_BUCKET = Registry.register(
			Registries.ITEM,
			YuexiWildDogMilk.id("dog_milk_bucket"),
			new DogMilkBucketItem(new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))
	);

	public static final Item WILD_DOG_MILK = Registry.register(
			Registries.ITEM,
			YuexiWildDogMilk.id("wild_dog_milk"),
			new WildDogMilkItem(new Item.Settings().maxCount(1))
	);

	public static final Item CENTURY_AGED_WILD_DOG_MILK = Registry.register(
			Registries.ITEM,
			YuexiWildDogMilk.id("century_aged_wild_dog_milk"),
			new CenturyAgedWildDogMilkItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
	);

	private ModItems() {
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
			entries.add(DOG_MILK_BUCKET);
			entries.add(WILD_DOG_MILK);
			entries.add(CENTURY_AGED_WILD_DOG_MILK);
		});
	}
}
