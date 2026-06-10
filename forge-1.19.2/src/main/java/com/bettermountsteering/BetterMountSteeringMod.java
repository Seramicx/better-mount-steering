package com.bettermountsteering;

import com.bettermountsteering.compat.IntegrationRegistry;
import com.bettermountsteering.handler.MountSteeringHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BetterMountSteeringMod.MODID)
public class BetterMountSteeringMod {
    public static final String MODID = "bettermountsteering";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BetterMountSteeringMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BetterMountSteeringConfig.CLIENT_CONFIG, "bettermountsteering-client.toml");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(MountSteeringHandler.getInstance());
        LOGGER.info("Seramicx's Better Mount Steering loaded.");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        IntegrationRegistry.resolve();
    }
}
