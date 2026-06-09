package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
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
            bettermountsteering$shouldRestorePlayerYRot = true;
            bettermountsteering$savedPlayerYRot = p.getYRot();
            p.setYRot(MountSteeringHandler.getMountSmoothedYaw());
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void bettermountsteering$afterTravel(Vec3 vec, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (bettermountsteering$shouldRestorePlayerYRot) {
            Entity rider = self.getControllingPassenger();
            if (rider instanceof Player p) {
                p.setYRot(bettermountsteering$savedPlayerYRot);
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
