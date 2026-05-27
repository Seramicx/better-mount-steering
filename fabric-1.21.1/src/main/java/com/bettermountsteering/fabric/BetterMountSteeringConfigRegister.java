package com.bettermountsteering.fabric;

import com.bettermountsteering.BetterMountSteeringConfig;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.neoforged.fml.config.ModConfig;

final class BetterMountSteeringConfigRegister {
    private BetterMountSteeringConfigRegister() {}

    static void register() {
        NeoForgeConfigRegistry.INSTANCE.register(
            BetterMountSteeringClient.MOD_ID,
            ModConfig.Type.CLIENT,
            BetterMountSteeringConfig.CLIENT_CONFIG,
            BetterMountSteeringClient.MOD_ID + "-client.toml"
        );
    }
}
