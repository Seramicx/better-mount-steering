package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class MixinCameraDecouple {

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float bettermountsteering$redirectViewYRot(Entity entity, float partialTick) {
        if (MountSteeringHandler.isDecoupleActive() && entity == Minecraft.getInstance().player) {
            return MountSteeringHandler.getDecoupledCameraYaw();
        }
        return entity.getViewYRot(partialTick);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float bettermountsteering$redirectViewXRot(Entity entity, float partialTick) {
        if (MountSteeringHandler.isDecoupleActive() && entity == Minecraft.getInstance().player) {
            return MountSteeringHandler.getDecoupledCameraXRot();
        }
        return entity.getViewXRot(partialTick);
    }
}
