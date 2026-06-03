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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BetterMountSteeringMod.MODID, value = Dist.CLIENT)
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

    private static volatile boolean wasOnMountLastTick = false;

    private static float blockedLockOnYRot = Float.NaN;
    private static boolean wasLockingOnLastTick = false;
    private static int postLockOffSmoothingTicks = 0;

    private static int tpsAimLingerRemaining = 0;

    public static boolean isMountRotateActive() { return mountRotateActive; }
    public static boolean isTpsAimLingerActive() { return tpsAimLingerRemaining > 0; }
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

    private static boolean getSmoothLockOnMountTurn() {
        try { return BetterMountSteeringConfig.SMOOTH_LOCKON_MOUNT_TURN.get(); }
        catch (Exception e) { return true; }
    }

    private static float getBloLockOnTurnSmoothness() {
        try { return (float) BetterMountSteeringConfig.BLO_LOCKON_TURN_SMOOTHNESS.get().doubleValue(); }
        catch (Exception e) { return 0.50F; }
    }

    private static int getTpsAimLingerTicks() {
        try { return BetterMountSteeringConfig.TPS_AIM_LINGER_TICKS.get(); }
        catch (Exception e) { return 0; }
    }

    private static BetterMountSteeringConfig.IdleBehavior getIdleBehavior() {
        try { return BetterMountSteeringConfig.IDLE_BEHAVIOR.get(); }
        catch (Exception e) { return BetterMountSteeringConfig.IdleBehavior.HOLD_DIRECTION; }
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = MC.player;
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
            tpsAimLingerRemaining = getTpsAimLingerTicks();
            deactivateDecouple(player);
            return false;
        }

        float[] dir = readDirectionalInput(input);
        float rawForward = dir[0];
        float rawStrafe  = dir[1];
        float rawMagnitude = Mth.sqrt(rawForward * rawForward + rawStrafe * rawStrafe);
        if (rawMagnitude < 0.01F) {
            if (getIdleBehavior() == BetterMountSteeringConfig.IdleBehavior.HOLD_DIRECTION
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

        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, getMountTurnSpeed());
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (player != MC.player) return;

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEnd(ClientTickEvent.Post event) {
        LocalPlayer player = MC.player;
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

        boolean shouldSmooth = getSmoothLockOnMountTurn()
                && IntegrationRegistry.isBetterLockOn()
                && isOnMountedMob(player)
                && !mountRotateActive
                && (isLockingOnNow || postLockOffSmoothingTicks > 0);

        if (shouldSmooth) {
            float current = player.getYRot();
            if (Float.isNaN(blockedLockOnYRot)) {
                blockedLockOnYRot = Mth.wrapDegrees(current);
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
