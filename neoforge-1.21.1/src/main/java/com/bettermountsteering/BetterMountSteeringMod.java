package com.bettermountsteering;

import com.bettermountsteering.compat.IntegrationRegistry;
import com.bettermountsteering.handler.MountSteeringHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(BetterMountSteeringMod.MODID)
public class BetterMountSteeringMod {
    public static final String MODID = "bettermountsteering";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BetterMountSteeringMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BetterMountSteeringConfig.CLIENT_CONFIG, "bettermountsteering-client.toml");

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);

        NeoForge.EVENT_BUS.register(MountSteeringHandler.getInstance());
        LOGGER.info("Seramicx's Better Mount Steering v1.0.5 loaded (NeoForge 1.21.1).");
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
