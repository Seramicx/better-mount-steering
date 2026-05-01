package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Substitutes {@code pPlayer.yRot} with mount-rotate's smoothed yaw inside
 * {@code travelRidden}, so the mount steers smoothly. Also preserves the
 * mount's previous-tick rotation fields ({@code yRotO}, {@code yBodyRotO},
 * {@code yHeadRotO}) which {@code travelRidden} clobbers - without that, the
 * client's per-frame interp would snap the mount's render rotation each tick.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityRiddenInterp {

    @Unique private float bettermountsteering$savedYRotO;
    @Unique private float bettermountsteering$savedYBodyRotO;
    @Unique private float bettermountsteering$savedYHeadRotO;
    @Unique private boolean bettermountsteering$shouldRestore;

    @Unique private float bettermountsteering$savedPlayerYRot;
    @Unique private boolean bettermountsteering$shouldRestorePlayerYRot;

    @Inject(method = "travelRidden", at = @At("HEAD"))
    private void bettermountsteering$beforeTravelRidden(Player pPlayer, Vec3 vec, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean clientLocalRider = self.level().isClientSide && pPlayer == Minecraft.getInstance().player;
        bettermountsteering$shouldRestore = clientLocalRider;
        if (clientLocalRider) {
            bettermountsteering$savedYRotO     = self.yRotO;
            bettermountsteering$savedYBodyRotO = self.yBodyRotO;
            bettermountsteering$savedYHeadRotO = self.yHeadRotO;
        }
        bettermountsteering$shouldRestorePlayerYRot = clientLocalRider && MountSteeringHandler.isMountRotateActive();
        if (bettermountsteering$shouldRestorePlayerYRot) {
            bettermountsteering$savedPlayerYRot = pPlayer.getYRot();
            pPlayer.setYRot(MountSteeringHandler.getMountSmoothedYaw());
        }
    }

    @Inject(method = "travelRidden", at = @At("RETURN"))
    private void bettermountsteering$afterTravelRidden(Player pPlayer, Vec3 vec, CallbackInfo ci) {
        if (bettermountsteering$shouldRestorePlayerYRot) {
            pPlayer.setYRot(bettermountsteering$savedPlayerYRot);
            bettermountsteering$shouldRestorePlayerYRot = false;
        }
        if (bettermountsteering$shouldRestore) {
            LivingEntity self = (LivingEntity) (Object) this;
            self.yRotO     = bettermountsteering$savedYRotO;
            self.yBodyRotO = bettermountsteering$savedYBodyRotO;
            self.yHeadRotO = bettermountsteering$savedYHeadRotO;
        }
    }
}
