package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Force-applies the mount-rotate input magnitude at HEAD of
 * {@code serverAiStep}, after any lower-priority mod may have rewritten the
 * impulses during {@code MovementInputUpdateEvent}. Without this, the mount
 * steers with the wrong magnitude when other handlers also modify input.
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerInputOverride {

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void bettermountsteering$forceMountInput(CallbackInfo ci) {
        if (!MountSteeringHandler.isMountRotateActive()) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.input == null) return;
        self.input.forwardImpulse = MountSteeringHandler.getMountInputMagnitude();
        self.input.leftImpulse = 0F;
    }
}
