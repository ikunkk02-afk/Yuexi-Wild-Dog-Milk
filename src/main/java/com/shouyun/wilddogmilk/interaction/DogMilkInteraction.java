package com.shouyun.wilddogmilk.interaction;

import com.shouyun.wilddogmilk.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public final class DogMilkInteraction {
	private DogMilkInteraction() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(DogMilkInteraction::tryMilkWolf);
	}

	private static ActionResult tryMilkWolf(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (player.isSpectator() || !(entity instanceof WolfEntity wolf) || wolf.isBaby() || !player.isSneaking()) {
			return ActionResult.PASS;
		}

		ItemStack heldStack = player.getStackInHand(hand);
		if (!heldStack.isOf(Items.BUCKET)) {
			return ActionResult.PASS;
		}

		if (!world.isClient) {
			player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
			ItemStack replacement = ItemUsage.exchangeStack(heldStack, player, ModItems.DOG_MILK_BUCKET.getDefaultStack());
			player.setStackInHand(hand, replacement);
		}

		return ActionResult.success(world.isClient);
	}
}
