package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses SSR's "follow player rotations" lerp while mount-rotate is
 * active. Without this, holding S on a mount makes SSR drag the camera
 * around 180° to face the player's body direction (because mount-rotate
 * sets {@code player.yRot} to the body-steering yaw, and SSR's
 * {@code renderTick} per-frame lerps the camera toward {@code player.yRot}
 * once {@code followPlayerRotationsDelay} decays to 0).
 *
 * <p><b>SSR's gate</b> ({@code ShoulderSurfingCamera.java:130}):
 * <pre>{@code
 * if(this.instance.isShoulderSurfing() && this.instance.isCameraDecoupled()
 *    && Config.CLIENT.getFollowPlayerRotations()
 *    && this.followPlayerRotationsDelay == 0
 *    && !EntityHelper.isPlayerSpectatingEntity()) {
 *     // lerp camera yaw toward player.yRot
 * }
 * }</pre>
 *
 * <p><b>Fix</b>: hold {@code followPlayerRotationsDelay} >= 1 at tick HEAD
 * while mount-rotate is active. SSR's tick decrements the field by 1 each
 * tick (line 96), so we set it to 2 - after decrement it's 1, the
 * {@code delay == 0} gate fails, lerp skipped. Once mount-rotate ends, we
 * stop touching the field and SSR's normal flow resumes (delay decays to 0,
 * follow-lerp re-engages on the next idle period).
 *
 * <p>This is the equivalent of constantly mouse-nudging during mount-rotate
 * (which would also keep the delay topped up via SSR's
 * {@code turn()} method) - without actually moving the camera.
 *
 * <p>{@code @Pseudo} because the target class is in SSR (runtime classpath).
 * {@code remap = false} because the class/method names are SSR's, not
 * vanilla MC. {@code require = 0} so the mod still loads if SSR is absent
 * or this method's signature changes in a future SSR release.
 */
@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public abstract class MixinSsrSuppressFollowDuringMountRotate {

    @Shadow private int followPlayerRotationsDelay;

    @Inject(method = "tick", at = @At("HEAD"), require = 0, remap = false)
    private void bettermountsteering$preventFollowDuringMountRotate(CallbackInfo ci) {
        if (MountSteeringHandler.isMountRotateActive() && this.followPlayerRotationsDelay < 2) {
            this.followPlayerRotationsDelay = 2;
        }
    }
}
