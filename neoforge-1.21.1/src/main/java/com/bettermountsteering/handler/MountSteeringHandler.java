package com.bettermountsteering.handler;

import com.bettermountsteering.BetterMountSteeringConfig;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class MountSteeringHandler {

    private static final MountSteeringHandler INSTANCE = new MountSteeringHandler();

    public static MountSteeringHandler getInstance() { return INSTANCE; }

    private volatile boolean mountRotateActive = false;
    private float mountSmoothedYaw = Float.NaN;

    private volatile boolean decoupleActive = false;
    private volatile boolean decoupleTransitioning = false;
    private volatile float decoupledCameraYaw = 0F;
    private volatile float decoupledCameraXRot = 0F;

    private volatile float mountInputMagnitude = 0F;

    private volatile boolean processingMouseTurn = false;

    private volatile boolean wasOnMountLastTick = false;

    private float blockedLockOnYRot = Float.NaN;
    private boolean wasLockingOnLastTick = false;
    private int postLockOffSmoothingTicks = 0;

    private int tpsAimLingerRemaining = 0;

    private MountSteeringHandler() {}

    public static boolean isMountRotateActive()    { return INSTANCE.mountRotateActive; }
    public static boolean isTpsAimLingerActive()   { return INSTANCE.tpsAimLingerRemaining > 0; }
    public static float   getMountSmoothedYaw()    { return INSTANCE.mountSmoothedYaw; }
    public static float   getMountInputMagnitude() { return INSTANCE.mountInputMagnitude; }

    public static boolean isProcessingMouseTurn()        { return INSTANCE.processingMouseTurn; }
    public static void    setProcessingMouseTurn(boolean v) { INSTANCE.processingMouseTurn = v; }

    public static boolean isDecoupleActive()        { return INSTANCE.decoupleActive; }
    public static boolean isDecoupleTransitioning() { return INSTANCE.decoupleTransitioning; }
    public static float   getDecoupledCameraYaw()   { return INSTANCE.decoupledCameraYaw; }
    public static float   getDecoupledCameraXRot()  { return INSTANCE.decoupledCameraXRot; }

    public static void addCameraDelta(float dy, float dx) {
        INSTANCE.decoupledCameraYaw  = Mth.wrapDegrees(INSTANCE.decoupledCameraYaw + dy);
        INSTANCE.decoupledCameraXRot = Mth.clamp(INSTANCE.decoupledCameraXRot + dx, -90F, 90F);
    }

    private float mountTurnSpeed() {
        return (float) BetterMountSteeringConfig.MOUNT_TURN_SPEED.get().doubleValue();
    }

    private boolean smoothLockOnMountTurn() {
        return BetterMountSteeringConfig.SMOOTH_LOCKON_MOUNT_TURN.get();
    }

    private float bloLockOnTurnSmoothness() {
        return (float) BetterMountSteeringConfig.BLO_LOCKON_TURN_SMOOTHNESS.get().doubleValue();
    }

    private int tpsAimLingerTicks() {
        return BetterMountSteeringConfig.TPS_AIM_LINGER_TICKS.get();
    }

    private BetterMountSteeringConfig.IdleBehavior idleBehavior() {
        return BetterMountSteeringConfig.IDLE_BEHAVIOR.get();
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
        Minecraft mc = Minecraft.getInstance();
        float rawForward = 0F;
        if (mc.options.keyUp.isDown())   rawForward += 1.0F;
        if (mc.options.keyDown.isDown()) rawForward -= 1.0F;

        float rawStrafe = 0F;
        if (mc.options.keyLeft.isDown())  rawStrafe += 1.0F;
        if (mc.options.keyRight.isDown()) rawStrafe -= 1.0F;

        if (rawForward == 0F && rawStrafe == 0F) {
            float[] analog = ControllableHelper.readAnalogDirection(input);
            rawForward = analog[0];
            rawStrafe  = analog[1];
        }

        return new float[]{rawForward, rawStrafe};
    }

    private void deactivateDecouple(LocalPlayer player) {
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

    @SubscribeEvent
    public void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (EpicFightHelper.isLockOnTargeting()) {
            if (decoupleActive) {
                player.setYRot(decoupledCameraYaw);
                player.yRotO = decoupledCameraYaw;
                player.setXRot(decoupledCameraXRot);
                player.xRotO = decoupledCameraXRot;
                decoupleActive = false;
                decoupleTransitioning = false;
                mountRotateActive = false;
            }
            mountSmoothedYaw = Float.NaN;
            return;
        }

        handleMountRotate(player, event.getInput());
    }

    private boolean handleMountRotate(LocalPlayer player, Input input) {
        boolean nowOnMount = isOnMountedMob(player);
        boolean freshMount = nowOnMount && !wasOnMountLastTick;
        wasOnMountLastTick = nowOnMount;

        mountRotateActive = false;
        if (Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
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
            tpsAimLingerRemaining = tpsAimLingerTicks();
            deactivateDecouple(player);
            return false;
        }

        float[] dir = readDirectionalInput(input);
        float rawForward = dir[0];
        float rawStrafe  = dir[1];
        float rawMagnitude = Mth.sqrt(rawForward * rawForward + rawStrafe * rawStrafe);
        if (rawMagnitude < 0.01F) {
            if (idleBehavior() == BetterMountSteeringConfig.IdleBehavior.HOLD_DIRECTION
                    && decoupleActive && !Float.isNaN(mountSmoothedYaw)) {
                Mob mount = (Mob) player.getVehicle();
                player.setYRot(mountSmoothedYaw);
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
                mountInputMagnitude = 0F;
                input.forwardImpulse = 0F;
                input.leftImpulse = 0F;
                mountRotateActive = true;
                return true;
            }
            deactivateDecouple(player);
            return false;
        }

        boolean justExitedLockOn = Float.isNaN(mountSmoothedYaw);

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
        } else if (justExitedLockOn) {
            float efYaw = EpicFightHelper.getCameraYRot();
            float efXRot = EpicFightHelper.getCameraXRot();
            if (!Float.isNaN(efYaw)) {
                decoupledCameraYaw = efYaw;
                bodyYaw = Mth.wrapDegrees(efYaw + offsetAngle);
            }
            if (!Float.isNaN(efXRot)) decoupledCameraXRot = efXRot;
            mountSmoothedYaw = bodyYaw;
        }

        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, mountTurnSpeed());
        player.setYRot(mountSmoothedYaw);
        Mob mount = (Mob) player.getVehicle();
        mount.setYRot(mountSmoothedYaw);
        mount.yBodyRot = mountSmoothedYaw;
        if (freshMount || justExitedLockOn) {
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

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (player != Minecraft.getInstance().player) return;

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
                float step = mountTurnSpeed();
                float newYRot = currentYRot + dy * step;
                float newXRot = currentXRot + dx * step;
                player.setYRot(newYRot);
                player.setXRot(newXRot);
                player.yBodyRot = newYRot;
                player.yHeadRot = newYRot;
            }
        }
    }

    @SubscribeEvent
    public void onClientTickEnd(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (tpsAimLingerRemaining > 0
                && !player.isUsingItem()
                && !player.isBlocking()) {
            tpsAimLingerRemaining--;
        }

        if (mountRotateActive) {
            player.setYRot(mountSmoothedYaw);
            player.yBodyRot = mountSmoothedYaw;
            player.yHeadRot = mountSmoothedYaw;
            if (player.getVehicle() instanceof Mob mount) {
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
            }
        }

        boolean isLockingOnNow = EpicFightHelper.isLockOnTargeting();
        boolean lockOffEdge = wasLockingOnLastTick && !isLockingOnNow;
        wasLockingOnLastTick = isLockingOnNow;

        if (lockOffEdge && isOnMountedMob(player)) {
            BLOTransitionSkipHook.skipPostLockOff();
            if (mountRotateActive) {
                player.setYRot(mountSmoothedYaw);
                player.yRotO = mountSmoothedYaw;
                player.yBodyRot = mountSmoothedYaw;
                player.yBodyRotO = mountSmoothedYaw;
                player.yHeadRot = mountSmoothedYaw;
                player.yHeadRotO = mountSmoothedYaw;
                if (player.getVehicle() instanceof Mob mount) {
                    mount.setYRot(mountSmoothedYaw);
                    mount.yRotO = mountSmoothedYaw;
                    mount.yBodyRot = mountSmoothedYaw;
                    mount.yBodyRotO = mountSmoothedYaw;
                }
                blockedLockOnYRot = Float.NaN;
                postLockOffSmoothingTicks = 0;
                return;
            }
            if (!Float.isNaN(blockedLockOnYRot)) {
                player.setYRot(blockedLockOnYRot);
                player.yRotO = blockedLockOnYRot;
                player.yBodyRot = blockedLockOnYRot;
                player.yBodyRotO = blockedLockOnYRot;
                player.yHeadRot = blockedLockOnYRot;
                player.yHeadRotO = blockedLockOnYRot;
                if (player.getVehicle() instanceof Mob mount) {
                    mount.setYRot(blockedLockOnYRot);
                    mount.yRotO = blockedLockOnYRot;
                    mount.yBodyRot = blockedLockOnYRot;
                    mount.yBodyRotO = blockedLockOnYRot;
                }
                decoupledCameraYaw = blockedLockOnYRot;
            }
            postLockOffSmoothingTicks = 15;
        }

        boolean shouldSmooth = smoothLockOnMountTurn()
                && IntegrationRegistry.isBetterLockOn()
                && isOnMountedMob(player)
                && !mountRotateActive
                && (isLockingOnNow || postLockOffSmoothingTicks > 0);

        if (shouldSmooth) {
            float current = player.getYRot();
            if (Float.isNaN(blockedLockOnYRot)) {
                blockedLockOnYRot = Mth.wrapDegrees(current);
            } else {
                float smoothed = smoothAngle(blockedLockOnYRot, current, bloLockOnTurnSmoothness());
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
