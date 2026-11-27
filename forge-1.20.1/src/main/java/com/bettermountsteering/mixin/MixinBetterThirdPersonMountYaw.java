package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Better Third Person turns the rider and the mount itself, which fights this mod's mount steering
@Pseudo
@Mixin(targets = "io.socol.betterthirdperson.impl.PlayerAdapter", remap = false)
public abstract class MixinBetterThirdPersonMountYaw {

    @Inject(method = {"setVehicleYaw", "setRotationYaw"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bettermountsteering$leaveMountToUs(float value, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && MountSteeringHandler.isSteeringMount(mc.player)) ci.cancel();
    }
}
