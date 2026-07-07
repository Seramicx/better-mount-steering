package com.bettermountsteering.mixin;

import com.bettermountsteering.api.MountCameraSource;
import com.bettermountsteering.api.MountSteeringApi;
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

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float bettermountsteering$redirectViewYRot(Entity entity, float partialTick) {
        if (bettermountsteering$shouldRedirect(entity)) {
            return MountSteeringHandler.getDecoupledCameraYaw();
        }
        return entity.getViewYRot(partialTick);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float bettermountsteering$redirectViewXRot(Entity entity, float partialTick) {
        if (bettermountsteering$shouldRedirect(entity)) {
            return MountSteeringHandler.getDecoupledCameraXRot();
        }
        return entity.getViewXRot(partialTick);
    }

    // An active camera source (e.g. shoulder surfing via ssr-camera-fixes) positions the camera itself, so
    // redirecting here would fight it
    @Unique
    private static boolean bettermountsteering$shouldRedirect(Entity entity) {
        if (!MountSteeringHandler.isDecoupleActive() || entity != Minecraft.getInstance().player) {
            return false;
        }
        MountCameraSource source = MountSteeringApi.getCameraSource();
        return source == null || !source.isActive();
    }
}
