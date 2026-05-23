package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses SSR's {@code ShoulderSurfingCamera.turn} writes to
 * {@code player.yRot/xRot/yRotO/xRotO} during the mount-rotate
 * deactivation transition.
 *
 * <p><b>What SSR does at {@code turn()} lines 487-493:</b>
 * <pre>{@code
 * if((shouldPlayerYRotFollowCamera || followPlayerRotations) && !isMoving) {
 *     float playerYRot = Mth.approachDegrees(this.lastMovedYRot,
 *                                            player.getYRot() + scaledYRot,
 *                                            maxFollowAngle); // default 90°
 *     player.yRotO = player.getYRot();
 *     player.setYRot(playerYRot);
 * }
 * }</pre>
 *
 * <p>{@code followPlayerRotations} defaults TRUE.
 * {@code lastMovedYRot} froze at the last tick mount-rotate set
 * {@code forwardImpulse>0} (so SSR's {@code isMoving} saw movement) —
 * i.e. at the body's movement direction.
 *
 * <p>During the transition, BMS lerps {@code player.yRot} from movement
 * direction toward the camera direction. If the camera is more than
 * {@code maxFollowAngle} (90°) from the movement direction, BMS's lerp
 * pushes past the clamp and SSR's mouse {@code turn()} pins
 * {@code player.yRot} back to {@code lastMovedYRot+90°} on every mouse
 * motion. The visible result: body stops at the 90° wedge and twitches
 * (BMS pushes past, SSR snaps back).
 *
 * <p><b>Fix:</b> save player rotation fields at {@code turn()} HEAD,
 * restore them at RETURN, gated on {@code isDecoupleTransitioning} and
 * the local player. SSR's internal {@code this.yRot/xRot} (which drives
 * the camera) still advances normally, so mouse-look continues to work
 * — only the player.yRot side-effect is suppressed.
 *
 * <p>{@code @Pseudo} because the target is SSR (optional runtime dep).
 * {@code remap = false} because the method is SSR's, not vanilla.
 * {@code require = 0} so the mod still loads when SSR is absent or
 * SSR's signature changes.
 */
@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public abstract class MixinSsrSuppressTurnSnapback {

    @Unique private boolean bettermountsteering$shouldRestore;
    @Unique private float bettermountsteering$savedYRot;
    @Unique private float bettermountsteering$savedXRot;
    @Unique private float bettermountsteering$savedYRotO;
    @Unique private float bettermountsteering$savedXRotO;

    @Inject(
        method = "turn(Lnet/minecraft/client/player/LocalPlayer;DD)Z",
        at = @At("HEAD"),
        require = 0,
        remap = false
    )
    private void bettermountsteering$beforeTurn(LocalPlayer player, double yRot, double xRot,
                                                CallbackInfoReturnable<Boolean> cir) {
        bettermountsteering$shouldRestore =
                MountSteeringHandler.isDecoupleTransitioning()
                && player == Minecraft.getInstance().player;
        if (bettermountsteering$shouldRestore) {
            bettermountsteering$savedYRot  = player.getYRot();
            bettermountsteering$savedXRot  = player.getXRot();
            bettermountsteering$savedYRotO = player.yRotO;
            bettermountsteering$savedXRotO = player.xRotO;
        }
    }

    @Inject(
        method = "turn(Lnet/minecraft/client/player/LocalPlayer;DD)Z",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private void bettermountsteering$afterTurn(LocalPlayer player, double yRot, double xRot,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!bettermountsteering$shouldRestore) return;
        player.setYRot(bettermountsteering$savedYRot);
        player.setXRot(bettermountsteering$savedXRot);
        player.yRotO = bettermountsteering$savedYRotO;
        player.xRotO = bettermountsteering$savedXRotO;
        bettermountsteering$shouldRestore = false;
    }
}
