package com.bettermountsteering.handler;

import com.bettermountsteering.BetterMountSteeringConfig;
import com.bettermountsteering.BetterMountSteeringMod;
import com.bettermountsteering.compat.BLOTransitionSkipHook;
import com.bettermountsteering.compat.ControllableHelper;
import com.bettermountsteering.compat.EpicFightHelper;
import com.bettermountsteering.compat.IntegrationRegistry;
import com.bettermountsteering.compat.ShoulderSurfingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mount-rotate (third-person, riding a Mob, BTP-style camera/body decoupling)
 * + the new BLO+mount lock-on smoothing.
 *
 * <p><b>Mount-rotate (no lock-on):</b> while active, mouse/right-stick deltas
 * route to {@code decoupledCameraYaw} (via {@code MixinEntityTurnDecouple}),
 * and {@code Camera.setup} reads from there (via {@code MixinCameraDecouple}).
 * {@code player.yRot} is set persistently to {@code mountSmoothedYaw} so the
 * mount steers there and the server agrees (no rotation snap-back).
 *
 * <p><b>BLO+mount lock-on smoothing (new in v1.0.0):</b> while BLO is locked
 * on AND riding a vanilla-controlled mount, replace BLO's discrete 8-direction
 * snap on {@code player.yRot} with a per-tick yaw clamp (uses
 * {@code mountTurnSpeed}). Body trails the locked target smoothly while BLO's
 * camera tracks the target as usual. Skipped when not locked on so BLO's
 * free-turn behavior is untouched.
 */
@Mod.EventBusSubscriber(modid = BetterMountSteeringMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MountSteeringHandler {

    private static final Minecraft MC = Minecraft.getInstance();

    private static volatile boolean mountRotateActive = false;
    private static float mountSmoothedYaw = Float.NaN;

    private static volatile boolean decoupleActive = false;
    private static volatile boolean decoupleTransitioning = false;
    private static volatile float decoupledCameraYaw = 0F;
    private static volatile float decoupledCameraXRot = 0F;

    private static volatile float mountInputMagnitude = 0F;

    private static volatile boolean processingMouseTurn = false;

    private static float blockedLockOnYRot = Float.NaN;
    private static boolean wasLockingOnLastTick = false;
    private static int postLockOffSmoothingTicks = 0;
    private static final int POST_LOCKOFF_DURATION = 15;

    public static boolean isMountRotateActive() { return mountRotateActive; }
    public static float   getMountSmoothedYaw() { return mountSmoothedYaw; }
    public static float   getMountInputMagnitude() { return mountInputMagnitude; }

    public static boolean isProcessingMouseTurn() { return processingMouseTurn; }
    public static void    setProcessingMouseTurn(boolean v) { processingMouseTurn = v; }

    public static boolean isDecoupleActive()       { return decoupleActive; }
    public static float   getDecoupledCameraYaw() { return decoupledCameraYaw; }
    public static float   getDecoupledCameraXRot(){ return decoupledCameraXRot; }

    public static void addCameraDelta(float dy, float dx) {
        decoupledCameraYaw  = Mth.wrapDegrees(decoupledCameraYaw + dy);
        decoupledCameraXRot = Mth.clamp(decoupledCameraXRot + dx, -90F, 90F);
    }

    private static float getMountTurnSpeed() {
        try { return (float) BetterMountSteeringConfig.MOUNT_TURN_SPEED.get().doubleValue(); }
        catch (Exception e) { return 0.25F; }
    }

    private static boolean getSmoothLockOnMountTurn() {
        try { return BetterMountSteeringConfig.SMOOTH_LOCKON_MOUNT_TURN.get(); }
        catch (Exception e) { return true; }
    }

    private static float getBloLockOnTurnSmoothness() {
        try { return (float) BetterMountSteeringConfig.BLO_LOCKON_TURN_SMOOTHNESS.get().doubleValue(); }
        catch (Exception e) { return 0.50F; }
    }

    private static boolean isOnMountedMob(LocalPlayer player) {
        Entity v = player.getVehicle();
        return v instanceof Mob mob && mob.getControllingPassenger() == player;
    }

    private static float smoothAngle(float from, float to, float factor) {
        float delta = Mth.wrapDegrees(to - from);
        return from + delta * factor;
    }

    /**
     * Reads the user's directional INTENT (raw keyboard state, with controller
     * analog fallback). Bypasses {@code input.forwardImpulse/leftImpulse}
     * because SSR's decoupled mode rotates those to align WASD with the
     * camera direction - which would corrupt mount-rotate's
     * {@code atan2(strafe, forward)} body-yaw calculation. Reading key state
     * directly gives us the user's intended direction in body-relative terms,
     * which is what mount-rotate needs.
     */
    private static float[] readDirectionalInput(Input input) {
        float rawForward = 0F;
        if (MC.options.keyUp.isDown())   rawForward += 1.0F;
        if (MC.options.keyDown.isDown()) rawForward -= 1.0F;

        float rawStrafe = 0F;
        if (MC.options.keyLeft.isDown())  rawStrafe += 1.0F;
        if (MC.options.keyRight.isDown()) rawStrafe -= 1.0F;

        // Controllable updates input.forwardImpulse/leftImpulse but not the
        // keyboard isDown() states. Fall back to analog when no keys are
        // pressed so controller users still get mount-rotate.
        if (rawForward == 0F && rawStrafe == 0F) {
            float[] analog = ControllableHelper.readAnalogDirection(input);
            rawForward = analog[0];
            rawStrafe  = analog[1];
        }

        return new float[]{rawForward, rawStrafe};
    }

    private static void deactivateDecouple(LocalPlayer player) {
        if (decoupleActive && !decoupleTransitioning) {
            float py = player.getYRot();
            float wrapped = Mth.wrapDegrees(py - decoupledCameraYaw);
            float normalized = decoupledCameraYaw + wrapped;
            if (normalized != py) {
                player.setYRot(normalized);
                player.yRotO = normalized;
            }
            decoupleTransitioning = true;
        }
        mountRotateActive = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = MC.player;
        if (player == null) return;

        // Lock-on path takes priority. Mod 1's LockOnMovementHandler owns
        // input there. We don't run mount-rotate during lock-on, but the
        // post-tick smoothing in onPlayerTick still applies for BLO+mount.
        if (EpicFightHelper.isLockOnTargeting()) {
            if (decoupleActive) {
                decoupleActive = false;
                decoupleTransitioning = false;
                mountRotateActive = false;
            }
            mountSmoothedYaw = Float.NaN;
            return;
        }

        handleMountRotate(player, event.getInput());
    }

    private static boolean handleMountRotate(LocalPlayer player, Input input) {
        mountRotateActive = false;
        if (MC.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            deactivateDecouple(player);
            mountSmoothedYaw = Float.NaN;
            return false;
        }
        if (!isOnMountedMob(player)) {
            deactivateDecouple(player);
            mountSmoothedYaw = Float.NaN;
            return false;
        }
        if (player.isUsingItem() || player.isBlocking()) {
            deactivateDecouple(player);
            return false;
        }

        float[] dir = readDirectionalInput(input);
        float rawForward = dir[0];
        float rawStrafe  = dir[1];
        float rawMagnitude = Mth.sqrt(rawForward * rawForward + rawStrafe * rawStrafe);
        if (rawMagnitude < 0.01F) {
            deactivateDecouple(player);
            return false;
        }

        // SSR is the camera authority only when the user has actually toggled
        // into SHOULDER_SURFING perspective. In vanilla 3P with SSR merely
        // loaded, SSR's camera state is stale - reading from it makes the
        // mount-rotate body yaw drift away from the actual on-screen camera,
        // producing the per-tick jitter the user sees only when SSR is
        // installed-but-inactive.
        boolean ssr = ShoulderSurfingHelper.isShoulderSurfingActive();
        float sourceYaw  = ssr ? ShoulderSurfingHelper.getCameraYaw()  : player.getYRot();
        float sourceXRot = ssr ? ShoulderSurfingHelper.getCameraXRot() : player.getXRot();

        if (ssr) {
            decoupledCameraYaw = sourceYaw;
            decoupledCameraXRot = sourceXRot;
            if (decoupleTransitioning) {
                decoupleTransitioning = false;
                mountSmoothedYaw = sourceYaw;
            }
            if (!decoupleActive) {
                mountSmoothedYaw = sourceYaw;
            }
        } else {
            if (decoupleTransitioning) {
                decoupleTransitioning = false;
                mountSmoothedYaw = sourceYaw;
            }
            if (!decoupleActive) {
                decoupledCameraYaw = sourceYaw;
                decoupledCameraXRot = sourceXRot;
                mountSmoothedYaw = sourceYaw;
            }
        }

        float offsetAngle = -(float) Math.toDegrees(Math.atan2(rawStrafe, rawForward));
        float bodyYaw     = Mth.wrapDegrees(decoupledCameraYaw + offsetAngle);

        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, getMountTurnSpeed());
        player.setYRot(mountSmoothedYaw);

        float modMagnitude = Mth.sqrt(input.forwardImpulse * input.forwardImpulse
                + input.leftImpulse * input.leftImpulse);
        float magnitude = Math.min(rawMagnitude, modMagnitude);
        input.forwardImpulse = magnitude;
        input.leftImpulse = 0F;
        mountInputMagnitude = magnitude;

        decoupleActive = true;
        mountRotateActive = true;
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!event.side.isClient()) return;

        LocalPlayer player = MC.player;
        if (player == null || event.player != player) return;

        // Mount-rotate: re-apply persistent body yaw after BLO/EF/etc. may
        // have rewritten yRot during the tick.
        if (mountRotateActive) {
            player.setYRot(mountSmoothedYaw);
            player.yBodyRot = mountSmoothedYaw;
            player.yHeadRot = mountSmoothedYaw;
        }

        // Smooth deactivation lerp.
        if (decoupleActive && decoupleTransitioning) {
            float currentYRot = player.getYRot();
            float dy = Mth.wrapDegrees(decoupledCameraYaw - currentYRot);
            float currentXRot = player.getXRot();
            float dx = decoupledCameraXRot - currentXRot;
            if (Math.abs(dy) < 1.0F && Math.abs(dx) < 1.0F) {
                player.setYRot(decoupledCameraYaw);
                player.setXRot(decoupledCameraXRot);
                player.yBodyRot = decoupledCameraYaw;
                player.yHeadRot = decoupledCameraYaw;
                decoupleActive = false;
                decoupleTransitioning = false;
            } else {
                float step = getMountTurnSpeed();
                float newYRot = currentYRot + dy * step;
                float newXRot = currentXRot + dx * step;
                player.setYRot(newYRot);
                player.setXRot(newXRot);
                player.yBodyRot = newYRot;
                player.yHeadRot = newYRot;
            }
        }

        // BLO+mount lock-on smoothing - see onClientTickEnd below. We
        // intentionally do NOT smooth in PlayerTickEvent.END: BLO writes
        // player.yRot at ClientTickEvent.Post (via EpicFightCameraAPI.postClientTick),
        // which fires AFTER PlayerTickEvent.END. Anything we'd smooth here
        // would be overwritten by BLO before next tick's mount.travelRidden
        // ever reads it. Smoothing must run at ClientTickEvent.END LOWEST
        // priority (after BLO) for it to actually persist into the next tick.
    }

    /**
     * BLO+mount lock-on smoothing. Runs at the very end of the tick, AFTER
     * BLO's {@code rewroteClientTick} has snapped {@code player.yRot} to the
     * locked-target direction. Reads what BLO wrote, lerps the body yaw
     * toward it via {@code smoothAngle}, and writes the smoothed value back.
     * Next tick's {@code mount.travelRidden} reads this smoothed
     * {@code player.yRot} as the steering direction.
     *
     * <p>BLO doesn't break: at its next {@code postClientTick}, BLO reads
     * {@code player.yRot} (= my smoothed value), recomputes
     * {@code yDelta = target - current} from that smaller delta, and writes a
     * smaller change toward target. So BLO's convergence to the locked target
     * just takes more ticks - which IS the smooth feel.
     *
     * <p>Why proportional lerp instead of a hard clamp: clamp = constant
     * velocity = mechanical feel. Proportional lerp = ease-out = smooth feel.
     * Lower {@code bloLockOnTurnSmoothness} = longer trail behind target.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer player = MC.player;
        if (player == null) return;

        boolean isLockingOnNow = EpicFightHelper.isLockOnTargeting();
        boolean lockOffEdge = wasLockingOnLastTick && !isLockingOnNow;
        wasLockingOnLastTick = isLockingOnNow;

        // Lock-off edge: BLO's setLockOn injection (EpicFightCameraAPIMixin:242)
        // snaps player.yRot to cameraYRot in one tick AND BLO's two
        // post-lock-off transition timers (BLOCameraSetting.transitionTick
        // and EpicFightCameraAPI.blo$unlockDelayTick) keep BLO's lock-on
        // camera transform engaged for a beat after lock-off - producing the
        // visible split-second flicker on a mount.
        //
        // Two-part fix:
        //   1. Force-skip BLO's transition timers via reflection so BLO
        //      releases the camera transform immediately (kills the flicker).
        //   2. Keep our own smoothing engaged for POST_LOCKOFF_DURATION ticks
        //      so the body eases from its lagging smoothed position to BLO's
        //      snap target instead of jumping in one tick.
        if (lockOffEdge && isOnMountedMob(player)) {
            BLOTransitionSkipHook.skipPostLockOff();
            postLockOffSmoothingTicks = POST_LOCKOFF_DURATION;
        }

        boolean shouldSmooth = getSmoothLockOnMountTurn()
                && IntegrationRegistry.isBetterLockOn()
                && isOnMountedMob(player)
                && (isLockingOnNow || postLockOffSmoothingTicks > 0);

        if (shouldSmooth) {
            float current = player.getYRot();
            if (Float.isNaN(blockedLockOnYRot)) {
                blockedLockOnYRot = current;
            } else {
                float smoothed = smoothAngle(blockedLockOnYRot, current, getBloLockOnTurnSmoothness());
                player.setYRot(smoothed);
                player.yRotO = smoothed;
                player.yBodyRot = smoothed;
                player.yBodyRotO = smoothed;
                player.yHeadRot = smoothed;
                player.yHeadRotO = smoothed;
                blockedLockOnYRot = smoothed;
            }
            if (!isLockingOnNow && postLockOffSmoothingTicks > 0) {
                postLockOffSmoothingTicks--;
            }
        } else {
            blockedLockOnYRot = Float.NaN;
            postLockOffSmoothingTicks = 0;
        }
    }
}
