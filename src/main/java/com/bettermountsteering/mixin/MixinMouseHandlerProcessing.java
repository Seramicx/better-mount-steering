package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets the {@code processingMouseTurn} flag while
 * {@code MouseHandler.turnPlayer} runs, so {@code MixinEntityTurnDecouple}
 * knows the in-flight {@code Entity.turn} call originates from user mouse
 * input (and routes its delta to the decoupled camera) rather than a
 * synthetic Entity.turn from EpicFight / BLO transitions.
 */
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
