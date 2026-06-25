package com.bettermountsteering.fabric;

import com.bettermountsteering.compat.IntegrationRegistry;
import com.bettermountsteering.handler.MountSteeringHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class BetterMountSteeringClient implements ClientModInitializer {
    public static final String MOD_ID = "bettermountsteering";

    @Override
    public void onInitializeClient() {
        BetterMountSteeringConfigRegister.register();
        IntegrationRegistry.resolve();
        ClientTickEvents.START_CLIENT_TICK.register(BetterMountSteeringClient::onClientTickStart);
        ClientTickEvents.END_CLIENT_TICK.register(BetterMountSteeringClient::onClientTickPost);
    }

    private static void onClientTickStart(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        MountSteeringHandler.onClientTickStartCombatSnap(player);
    }

    private static void onClientTickPost(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        MountSteeringHandler.onPlayerTickPost(player);
    }
}
