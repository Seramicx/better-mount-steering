package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public abstract class MixinSsrSuppressTurnSnapback {

    @Unique private boolean bettermountsteering$shouldRestore;
    @Unique private float bettermountsteering$savedYRot;
    @Unique private float bettermountsteering$savedXRot;
    @Unique private float bettermountsteering$savedYRotO;
    @Unique private float bettermountsteering$savedXRotO;

    @Inject(
        method = "turn",
        at = @At("HEAD"),
        require = 0,
        remap = false
    )
    private void bettermountsteering$beforeTurn(LocalPlayer player, double yRot, double xRot,
                                                CallbackInfoReturnable<Boolean> cir) {
        bettermountsteering$shouldRestore =
                MountSteeringHandler.isDecoupleTransitioning()
                && player == Minecraft.getInstance().player;
        if (bettermountsteering$shouldRestore) {
            bettermountsteering$savedYRot  = player.getYRot();
            bettermountsteering$savedXRot  = player.getXRot();
            bettermountsteering$savedYRotO = player.yRotO;
            bettermountsteering$savedXRotO = player.xRotO;
        }
    }

    @Inject(
        method = "turn",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private void bettermountsteering$afterTurn(LocalPlayer player, double yRot, double xRot,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!bettermountsteering$shouldRestore) return;
        player.setYRot(bettermountsteering$savedYRot);
        player.setXRot(bettermountsteering$savedXRot);
        player.yRotO = bettermountsteering$savedYRotO;
        player.xRotO = bettermountsteering$savedXRotO;
        bettermountsteering$shouldRestore = false;
    }
}
