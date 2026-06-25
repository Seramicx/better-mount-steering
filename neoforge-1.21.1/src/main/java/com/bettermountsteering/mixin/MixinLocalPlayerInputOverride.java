package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerInputOverride {

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void bettermountsteering$forceMountInput(CallbackInfo ci) {
        if (!MountSteeringHandler.isMountRotateActive()) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.input == null) return;
        self.input.forwardImpulse = MountSteeringHandler.getMountInputForward();
        self.input.leftImpulse = MountSteeringHandler.getMountInputStrafe();
    }
}
