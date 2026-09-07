package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The 1.21.1 JEG port keeps no shooting flag anywhere, so the mount auto-turn's combat check has nothing to poll.
// Latch at the shoot packet instead, the same way held crossbows are handled
@Pseudo
@Mixin(targets = "ttv.migami.jeg.network.ClientNetworkHandler", remap = false)
public abstract class MixinJegShootTurn {

    @Inject(method = "sendShoot", at = @At("HEAD"), require = 0, remap = false)
    private static void bettermountsteering$latchGunFire(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null) return;

        MountSteeringHandler.snapToCameraNow(local);
    }
}
