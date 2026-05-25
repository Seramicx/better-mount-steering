package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
