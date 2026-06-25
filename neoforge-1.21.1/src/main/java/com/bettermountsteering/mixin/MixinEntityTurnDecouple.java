package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntityTurnDecouple {

    @Unique private float bettermountsteering$savedYRot;
    @Unique private float bettermountsteering$savedYRotO;
    @Unique private boolean bettermountsteering$shouldRestore;

    @Unique private float bettermountsteering$savedXRot;
    @Unique private float bettermountsteering$savedXRotO;

    @Inject(method = "turn(DD)V", at = @At("HEAD"))
    private void bettermountsteering$beforeTurn(double yaw, double pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        bettermountsteering$shouldRestore =
                self instanceof LocalPlayer
                && MountSteeringHandler.isDecoupleActive()
                && MountSteeringHandler.isProcessingMouseTurn();
        if (bettermountsteering$shouldRestore) {
            bettermountsteering$savedYRot  = self.getYRot();
            bettermountsteering$savedYRotO = self.yRotO;
            bettermountsteering$savedXRot  = self.getXRot();
            bettermountsteering$savedXRotO = self.xRotO;
        }
    }

    @Inject(method = "turn(DD)V", at = @At("RETURN"))
    private void bettermountsteering$afterTurn(double yaw, double pitch, CallbackInfo ci) {
        if (!bettermountsteering$shouldRestore) return;
        Entity self = (Entity) (Object) this;
        float dyaw = self.getYRot() - bettermountsteering$savedYRot;
        float dpitch = self.getXRot() - bettermountsteering$savedXRot;
        MountSteeringHandler.addCameraDelta(dyaw, dpitch);
        self.setYRot(bettermountsteering$savedYRot);
        self.yRotO = bettermountsteering$savedYRotO;
        bettermountsteering$shouldRestore = false;
    }
}
