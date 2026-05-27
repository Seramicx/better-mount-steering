package com.bettermountsteering.handler;

import com.bettermountsteering.BetterMountSteeringConfig;
import com.bettermountsteering.compat.ControllableHelper;
import com.bettermountsteering.compat.ShoulderSurfingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class MountSteeringHandler {

    private static final Minecraft MC = Minecraft.getInstance();

    private static volatile boolean mountRotateActive = false;
    private static float mountSmoothedYaw = Float.NaN;

    private static volatile boolean decoupleActive = false;
    private static volatile boolean decoupleTransitioning = false;
    private static volatile float decoupledCameraYaw = 0F;
    private static volatile float decoupledCameraXRot = 0F;

    private static volatile float mountInputMagnitude = 0F;

    private static volatile boolean processingMouseTurn = false;

    private static volatile boolean wasOnMountLastTick = false;

    private MountSteeringHandler() {}

    public static boolean isMountRotateActive() { return mountRotateActive; }
    public static float   getMountSmoothedYaw() { return mountSmoothedYaw; }
    public static float   getMountInputMagnitude() { return mountInputMagnitude; }

    public static boolean isProcessingMouseTurn() { return processingMouseTurn; }
    public static void    setProcessingMouseTurn(boolean v) { processingMouseTurn = v; }

    public static boolean isDecoupleActive()       { return decoupleActive; }
    public static boolean isDecoupleTransitioning(){ return decoupleTransitioning; }
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

    private static boolean isOnMountedMob(LocalPlayer player) {
        Entity v = player.getVehicle();
        return v instanceof Mob mob && mob.getControllingPassenger() == player;
    }

    private static float smoothAngle(float from, float to, float factor) {
        float delta = Mth.wrapDegrees(to - from);
        return Mth.wrapDegrees(from + delta * factor);
    }

    private static float[] readDirectionalInput(Input input) {
        float rawForward = 0F;
        if (MC.options.keyUp.isDown())   rawForward += 1.0F;
        if (MC.options.keyDown.isDown()) rawForward -= 1.0F;

        float rawStrafe = 0F;
        if (MC.options.keyLeft.isDown())  rawStrafe += 1.0F;
        if (MC.options.keyRight.isDown()) rawStrafe -= 1.0F;

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

    public static void onMovementInput(LocalPlayer player, Input input) {
        handleMountRotate(player, input);
    }

    private static boolean handleMountRotate(LocalPlayer player, Input input) {
        boolean nowOnMount = isOnMountedMob(player);
        boolean freshMount = nowOnMount && !wasOnMountLastTick;
        wasOnMountLastTick = nowOnMount;

        mountRotateActive = false;
        if (MC.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            deactivateDecouple(player);
            mountSmoothedYaw = Float.NaN;
            return false;
        }
        if (!nowOnMount) {
            float[] dir = readDirectionalInput(input);
            float magnitude = Mth.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
            boolean userIsMoving = magnitude >= 0.01F;
            boolean ssrActive = ShoulderSurfingHelper.isShoulderSurfingActive();

            if (ssrActive && userIsMoving) {
                mountRotateActive = false;
                decoupleActive = false;
                decoupleTransitioning = false;
                mountSmoothedYaw = Float.NaN;
            } else {
                deactivateDecouple(player);
                mountSmoothedYaw = Float.NaN;
            }
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

        if (freshMount) {
            mountSmoothedYaw = bodyYaw;
        }

        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, getMountTurnSpeed());
        player.setYRot(mountSmoothedYaw);
        Mob mount = (Mob) player.getVehicle();
        mount.setYRot(mountSmoothedYaw);
        mount.yBodyRot = mountSmoothedYaw;
        if (freshMount) {
            mount.yRotO = mountSmoothedYaw;
            mount.yBodyRotO = mountSmoothedYaw;
        }

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

    public static void onPlayerTickPost(LocalPlayer player) {
        if (mountRotateActive) {
            player.setYRot(mountSmoothedYaw);
            player.yBodyRot = mountSmoothedYaw;
            player.yHeadRot = mountSmoothedYaw;
            if (player.getVehicle() instanceof Mob mount) {
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
            }
        }

        if (decoupleActive && decoupleTransitioning) {
            if (ShoulderSurfingHelper.isShoulderSurfingActive()) {
                decoupledCameraYaw = ShoulderSurfingHelper.getCameraYaw();
                decoupledCameraXRot = ShoulderSurfingHelper.getCameraXRot();
            }
            float currentYRot = player.getYRot();
            float dy = Mth.wrapDegrees(decoupledCameraYaw - currentYRot);
            float currentXRot = player.getXRot();
            float dx = decoupledCameraXRot - currentXRot;
            if (Math.abs(dy) < 1.0F && Math.abs(dx) < 1.0F) {
                player.setYRot(decoupledCameraYaw);
                player.setXRot(decoupledCameraXRot);
                player.yBodyRot = decoupledCameraYaw;
                player.yHeadRot = decoupledCameraYaw;
                ShoulderSurfingHelper.setLastMovedYRot(decoupledCameraYaw);
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
    }
}
