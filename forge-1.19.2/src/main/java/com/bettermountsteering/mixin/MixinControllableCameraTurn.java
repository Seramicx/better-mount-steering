package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mrcrayfish.controllable.client.ControllerInput", remap = false)
public abstract class MixinControllableCameraTurn {

    @Inject(method = "onRender(Lnet/minecraftforge/event/TickEvent$RenderTickEvent;)V", at = @At("HEAD"), remap = false, require = 0)
    private void bettermountsteering$beforeControllerTurn(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(true);
    }

    @Inject(method = "onRender(Lnet/minecraftforge/event/TickEvent$RenderTickEvent;)V", at = @At("RETURN"), remap = false, require = 0)
    private void bettermountsteering$afterControllerTurn(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        MountSteeringHandler.setProcessingMouseTurn(false);
    }
}
