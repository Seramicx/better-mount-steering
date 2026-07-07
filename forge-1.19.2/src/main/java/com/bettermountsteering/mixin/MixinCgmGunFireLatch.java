package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.GunModHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Scorched Guns is a CGM addon here, so both fire through this handler. Arm the latch at fire() so semi-auto
// guns, which drop the shooting flag the same tick, still auto-turn the mount toward the camera
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.client.handler.ShootingHandler", remap = false)
public abstract class MixinCgmGunFireLatch {

    @Inject(method = "fire", at = @At("HEAD"), require = 0, remap = false)
    private void bettermountsteering$latchFire(CallbackInfo ci) {
        GunModHelper.signalFire();
    }
}
