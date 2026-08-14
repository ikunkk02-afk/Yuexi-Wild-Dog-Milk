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
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public final class CenturyAgedWildDogMilkItem extends Item {
	public CenturyAgedWildDogMilkItem(Settings settings) {
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
			boolean firstUnlock = PermanentShelfLifeData.grantDeepTime(player);
			player.playSoundToPlayer(SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.85F, 1.0F);
			if (firstUnlock) {
				player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 10));
				player.networkHandler.sendPacket(new TitleS2CPacket(
						Text.translatable("title.yuexi-wild-dog-milk.beyond_time")
				));
			}
		}

		if (user instanceof PlayerEntity player) {
			return ItemUsage.exchangeStack(stack, player, Items.GLASS_BOTTLE.getDefaultStack(), false);
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
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.century_aged.vintage").formatted(Formatting.GOLD));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.century_aged.age").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.century_aged.shelf_life").formatted(Formatting.GREEN));
		tooltip.add(Text.empty());
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.century_aged.unlock").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("tooltip.yuexi-wild-dog-milk.century_aged.epitaph")
				.formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
	}
}
