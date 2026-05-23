package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric equivalent of Forge's MovementInputUpdateEvent. Injects after
 * Input.tick() in LocalPlayer.aiStep so mount-rotate can read the user's
 * fresh-this-tick input and overwrite forwardImpulse/leftImpulse before
 * the mount travels next tick.
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerInputHook {

    @Inject(
        method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
                 shift = At.Shift.AFTER)
    )
    private void bettermountsteering$afterInputTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.input == null) return;
        MountSteeringHandler.onMovementInput(self, self.input);
    }
}
