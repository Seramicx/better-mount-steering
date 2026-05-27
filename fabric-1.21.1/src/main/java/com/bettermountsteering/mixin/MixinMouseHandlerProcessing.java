package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandlerProcessing {

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void bettermountsteering$beforeTurnPlayer(CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(true);
    }

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void bettermountsteering$afterTurnPlayer(CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(false);
    }
}
