package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityRiddenInterp {

    @Unique private float bettermountsteering$savedYRotO;
    @Unique private float bettermountsteering$savedYBodyRotO;
    @Unique private float bettermountsteering$savedYHeadRotO;
    @Unique private boolean bettermountsteering$shouldRestore;

    @Unique private float bettermountsteering$savedPlayerYRot;
    @Unique private float bettermountsteering$savedPlayerXRot;
    @Unique private boolean bettermountsteering$shouldRestorePlayerYRot;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void bettermountsteering$beforeTravel(Vec3 vec, CallbackInfo ci) {
        bettermountsteering$shouldRestore = false;
        bettermountsteering$shouldRestorePlayerYRot = false;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level.isClientSide) return;
        Entity rider = self.getControllingPassenger();
        if (!(rider instanceof Player p) || p != Minecraft.getInstance().player) return;
        bettermountsteering$shouldRestore = true;
        bettermountsteering$savedYRotO     = self.yRotO;
        bettermountsteering$savedYBodyRotO = self.yBodyRotO;
        bettermountsteering$savedYHeadRotO = self.yHeadRotO;
        if (MountSteeringHandler.isMountRotateActive()) {
            float steered = MountSteeringHandler.getMountSmoothedYaw();
            bettermountsteering$shouldRestorePlayerYRot = true;
            bettermountsteering$savedPlayerYRot = p.getYRot();
            bettermountsteering$savedPlayerXRot = p.getXRot();
            p.setYRot(steered);
            // The mount mirrors half the rider's pitch, so a flat rider pitch keeps the mount's head level.
            p.setXRot(0F);
            // travel() reads the mount's own yaw, which another mod leaves flipped ~180 from BMS's steered yaw,
            // dragging it camera-ward; pin it before travel.
            if (!Float.isNaN(steered)) {
                self.setYRot(steered);
                self.yBodyRot = steered;
                self.yRotO = steered;
                self.yBodyRotO = steered;
            }
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void bettermountsteering$afterTravel(Vec3 vec, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (bettermountsteering$shouldRestorePlayerYRot) {
            Entity rider = self.getControllingPassenger();
            if (rider instanceof Player p) {
                p.setYRot(bettermountsteering$savedPlayerYRot);
                p.setXRot(bettermountsteering$savedPlayerXRot);
            }
            float steered = MountSteeringHandler.getMountSmoothedYaw();
            if (!Float.isNaN(steered)) {
                self.setYRot(steered);
                self.yBodyRot = steered;
                Vec3 dm = self.getDeltaMovement();
                double horizSpeed = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
                if (horizSpeed > 0.005) {
                    float yawRad = steered * ((float) Math.PI / 180F);
                    self.setDeltaMovement(-Mth.sin(yawRad) * horizSpeed, dm.y, Mth.cos(yawRad) * horizSpeed);
                }
            }
            bettermountsteering$shouldRestorePlayerYRot = false;
        }
        if (bettermountsteering$shouldRestore) {
            self.yRotO     = bettermountsteering$savedYRotO;
            self.yBodyRotO = bettermountsteering$savedYBodyRotO;
            self.yHeadRotO = bettermountsteering$savedYHeadRotO;
            bettermountsteering$shouldRestore = false;
        }
    }
}
