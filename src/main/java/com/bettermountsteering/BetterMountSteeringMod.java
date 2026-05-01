package com.bettermountsteering;

import com.bettermountsteering.compat.IntegrationRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BetterMountSteeringMod.MODID)
public class BetterMountSteeringMod {
    public static final String MODID = "bettermountsteering";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BetterMountSteeringMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, BetterMountSteeringConfig.CLIENT_CONFIG, "bettermountsteering-client.toml");

        context.getModEventBus().addListener(this::onCommonSetup);
        context.getModEventBus().addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Seramicx's Better Mount Steering v1.0.0 loaded.");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        IntegrationRegistry.resolve();
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        if (IntegrationRegistry.isEpicFight()) {
            event.enqueueWork(com.bettermountsteering.compat.EpicFightTPSDecoupleHook::register);
        }
    }
}
