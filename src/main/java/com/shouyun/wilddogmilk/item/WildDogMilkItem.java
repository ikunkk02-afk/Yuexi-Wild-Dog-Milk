package com.shouyun.wilddogmilk.item;

import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public final class WildDogMilkItem extends Item {
	public WildDogMilkItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return ItemUsage.consumeHeldItem(world, user, hand);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof ServerPlayerEntity player) {
			Criteria.CONSUME_ITEM.trigger(player, stack);
			player.incrementStat(Stats.USED.getOrCreateStat(this));
			PermanentShelfLifeData.grant(player);
		}

		if (user instanceof PlayerEntity player) {
			return ItemUsage.exchangeStack(stack, player, Items.BUCKET.getDefaultStack(), false);
		}

		stack.decrementUnlessCreative(1, user);
		return stack;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 32;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.wild_dog_milk.supply").formatted(Formatting.GOLD));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.wild_dog_milk.wild").formatted(Formatting.WHITE));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.wild_dog_milk.shelf_life").formatted(Formatting.GREEN));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.wild_dog_milk.epitaph").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}
}
