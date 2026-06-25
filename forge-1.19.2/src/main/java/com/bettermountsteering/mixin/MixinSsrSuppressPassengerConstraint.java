package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import com.github.exopandora.shouldersurfing.math.Vec2f;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public abstract class MixinSsrSuppressPassengerConstraint {

    @Inject(method = "applyPassengerRotationConstraints", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void bettermountsteering$skipPassengerConstraint(Player player, float cameraXRot, float cameraYRot,
                                                                    float cameraXRotO, float cameraYRotO,
                                                                    CallbackInfoReturnable<Vec2f> cir) {
        if (MountSteeringHandler.isMountRotateActive()) {
            cir.setReturnValue(new Vec2f(cameraXRot, cameraYRot));
        }
    }
}
