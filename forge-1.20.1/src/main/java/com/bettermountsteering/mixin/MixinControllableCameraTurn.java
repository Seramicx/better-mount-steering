package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same role as {@link MixinMouseHandlerProcessing} but for Controllable's
 * controller-driven turn dispatch, which calls {@code mc.player.turn(...)}
 * directly (bypassing {@code MouseHandler.turnPlayer}). Without this,
 * controller right-stick input wouldn't flow through the decouple path —
 * BMS's {@link MixinEntityTurnDecouple}'s {@code shouldRestore} gate
 * requires {@code processingMouseTurn = true} to capture the delta to
 * {@code decoupledCameraYaw} and restore {@code player.yRot}.
 *
 * <p>Controllable 0.21.9 (MC 1.20.1) calls {@code player.turn} from
 * {@code com.mrcrayfish.controllable.client.ControllerInput#onRenderTickEnd}
 * (registered to {@code TickEvents.END_RENDER}). Older Controllable versions
 * had the same dispatch in {@code CameraHandler.updateCamera}; that class
 * no longer exists in 0.21.9, which is what broke compat.
 *
 * <p>{@code @Pseudo} because Controllable is optional. {@code require = 0}
 * tolerates the inject being absent (e.g., on a Controllable version that
 * has neither class).
 */
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
