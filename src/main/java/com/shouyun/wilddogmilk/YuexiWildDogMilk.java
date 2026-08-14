package com.shouyun.wilddogmilk;

import com.shouyun.wilddogmilk.interaction.DogMilkInteraction;
import com.shouyun.wilddogmilk.network.ModNetworking;
import com.shouyun.wilddogmilk.player.PermanentShelfLifeData;
import com.shouyun.wilddogmilk.registry.ModEffects;
import com.shouyun.wilddogmilk.registry.ModItems;
import com.shouyun.wilddogmilk.time.TimeAccelerationManager;
import com.shouyun.wilddogmilk.time.boss.BossAgingManager;
import com.shouyun.wilddogmilk.time.sideeffect.TemporalOverloadManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuexiWildDogMilk implements ModInitializer {
	public static final String MOD_ID = "yuexi-wild-dog-milk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEffects.register();
		ModItems.register();
		TimeAccelerationManager.register();
		BossAgingManager.register();
		PermanentShelfLifeData.register();
		TemporalOverloadManager.register();
		ModNetworking.register();
		DogMilkInteraction.register();

		LOGGER.info("Yuexi Wild Dog Milk is ready.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
