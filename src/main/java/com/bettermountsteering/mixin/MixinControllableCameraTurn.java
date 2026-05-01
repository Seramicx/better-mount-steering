package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same role as {@link MixinMouseHandlerProcessing} but for Controllable's
 * {@code CameraHandler.updateCamera}, which calls {@code mc.player.turn}
 * directly (bypassing {@code MouseHandler.turnPlayer}). Without this,
 * controller right-stick input wouldn't flow through the decouple path.
 *
 * <p>{@code @Pseudo} because Controllable is optional.
 */
@Pseudo
@Mixin(targets = "com.mrcrayfish.controllable.client.CameraHandler", remap = false)
public abstract class MixinControllableCameraTurn {

    @Inject(method = "updateCamera", at = @At("HEAD"), remap = false, require = 0)
    private void bettermountsteering$beforeUpdateCamera(CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(true);
    }

    @Inject(method = "updateCamera", at = @At("RETURN"), remap = false, require = 0)
    private void bettermountsteering$afterUpdateCamera(CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(false);
    }
}
