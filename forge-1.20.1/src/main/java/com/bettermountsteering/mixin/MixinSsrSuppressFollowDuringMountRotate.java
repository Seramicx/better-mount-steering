package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public abstract class MixinSsrSuppressFollowDuringMountRotate {

    @Shadow private int followPlayerRotationsDelay;

    @Inject(method = "tick", at = @At("HEAD"), require = 0, remap = false)
    private void bettermountsteering$preventFollowDuringMountRotate(CallbackInfo ci) {
        boolean active = MountSteeringHandler.isMountRotateActive()
                || MountSteeringHandler.isDecoupleTransitioning();
        if (active && this.followPlayerRotationsDelay < 2) {
            this.followPlayerRotationsDelay = 2;
        }
    }
}
