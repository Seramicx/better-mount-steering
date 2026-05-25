package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mrcrayfish.controllable.client.CameraHandler", remap = false)
public abstract class MixinControllableCameraTurn {

    @Inject(method = "updateCamera", at = @At("HEAD"), remap = false, require = 0)
    private void bettermountsteering$beforeControllerTurn(DeltaTracker tracker, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(true);
    }

    @Inject(method = "updateCamera", at = @At("RETURN"), remap = false, require = 0)
    private void bettermountsteering$afterControllerTurn(DeltaTracker tracker, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(false);
    }
}
