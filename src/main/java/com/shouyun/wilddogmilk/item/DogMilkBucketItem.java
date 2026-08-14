package com.shouyun.wilddogmilk.item;

import net.minecraft.item.Item;
import net.minecraft.item.MilkBucketItem;

/**
 * Uses vanilla milk-bucket consumption behavior: drink animation, effect clearing,
 * and a returned empty bucket.
 */
public final class DogMilkBucketItem extends MilkBucketItem {
	public DogMilkBucketItem(Item.Settings settings) {
		super(settings);
	}
}
