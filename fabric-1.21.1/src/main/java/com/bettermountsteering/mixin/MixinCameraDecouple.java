package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class MixinCameraDecouple {

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"), require = 0)
    private float bettermountsteering$redirectViewYRot(Entity entity, float partialTick) {
        if (bettermountsteering$shouldRedirect(entity)) {
            return MountSteeringHandler.getDecoupledCameraYaw();
        }
        return entity.getViewYRot(partialTick);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"), require = 0)
    private float bettermountsteering$redirectViewXRot(Entity entity, float partialTick) {
        if (bettermountsteering$shouldRedirect(entity)) {
            return MountSteeringHandler.getDecoupledCameraXRot();
        }
        return entity.getViewXRot(partialTick);
    }

    // Better Third Person redirects the same calls, so these stay optional and step aside for any external camera
    @Unique
    private static boolean bettermountsteering$shouldRedirect(Entity entity) {
        if (!MountSteeringHandler.isDecoupleActive() || entity != Minecraft.getInstance().player) {
            return false;
        }
        return !MountSteeringHandler.hasExternalCamera();
    }
}
