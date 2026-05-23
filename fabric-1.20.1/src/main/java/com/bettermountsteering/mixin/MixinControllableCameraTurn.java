package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mrcrayfish.controllable.client.ControllerInput", remap = false)
public abstract class MixinControllableCameraTurn {

    @Inject(method = "onRenderTickEnd", at = @At("HEAD"), remap = false, require = 0)
    private void bettermountsteering$beforeControllerTurn(float partialTick, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(true);
    }

    @Inject(method = "onRenderTickEnd", at = @At("RETURN"), remap = false, require = 0)
    private void bettermountsteering$afterControllerTurn(float partialTick, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(false);
    }
}
