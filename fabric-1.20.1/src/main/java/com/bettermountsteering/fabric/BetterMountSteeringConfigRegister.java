package com.bettermountsteering.fabric;

import com.bettermountsteering.BetterMountSteeringConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;

final class BetterMountSteeringConfigRegister {
    private BetterMountSteeringConfigRegister() {}

    static void register() {
        ForgeConfigRegistry.INSTANCE.register(
            BetterMountSteeringClient.MOD_ID,
            ModConfig.Type.CLIENT,
            BetterMountSteeringConfig.CLIENT_CONFIG,
            BetterMountSteeringClient.MOD_ID + "-client.toml"
        );
    }
}
