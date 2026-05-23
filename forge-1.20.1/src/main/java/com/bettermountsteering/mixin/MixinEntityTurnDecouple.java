package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures user-driven mouse-turn deltas during {@code Entity.turn} and routes
 * them to {@link MountSteeringHandler#addCameraDelta} while decouple is
 * active and the call originates from the user's input path
 * (gated by {@link MountSteeringHandler#isProcessingMouseTurn}). Then
 * restores the entity's pre-turn yRot/xRot so {@code player.yRot} stays
 * pinned at body direction.
 */
@Mixin(Entity.class)
public abstract class MixinEntityTurnDecouple {

    @Unique private float bettermountsteering$savedYRot;
    @Unique private float bettermountsteering$savedXRot;
    @Unique private float bettermountsteering$savedYRotO;
    @Unique private float bettermountsteering$savedXRotO;
    @Unique private boolean bettermountsteering$shouldRestore;

    @Inject(method = "turn(DD)V", at = @At("HEAD"))
    private void bettermountsteering$beforeTurn(double yaw, double pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        bettermountsteering$shouldRestore =
                self instanceof LocalPlayer
                && MountSteeringHandler.isDecoupleActive()
                && MountSteeringHandler.isProcessingMouseTurn();
        if (bettermountsteering$shouldRestore) {
            bettermountsteering$savedYRot  = self.getYRot();
            bettermountsteering$savedXRot  = self.getXRot();
            bettermountsteering$savedYRotO = self.yRotO;
            bettermountsteering$savedXRotO = self.xRotO;
        }
    }

    @Inject(method = "turn(DD)V", at = @At("RETURN"))
    private void bettermountsteering$afterTurn(double yaw, double pitch, CallbackInfo ci) {
        if (!bettermountsteering$shouldRestore) return;
        Entity self = (Entity) (Object) this;
        float dyaw  = self.getYRot() - bettermountsteering$savedYRot;
        float dpitch = self.getXRot() - bettermountsteering$savedXRot;
        MountSteeringHandler.addCameraDelta(dyaw, dpitch);
        self.setYRot(bettermountsteering$savedYRot);
        self.setXRot(bettermountsteering$savedXRot);
        self.yRotO = bettermountsteering$savedYRotO;
        self.xRotO = bettermountsteering$savedXRotO;
        bettermountsteering$shouldRestore = false;
    }
}
