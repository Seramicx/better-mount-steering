package com.bettermountsteering.handler;

import com.bettermountsteering.BetterMountSteeringConfig;
import com.bettermountsteering.api.MountCameraSource;
import com.bettermountsteering.api.MountSteeringApi;
import com.bettermountsteering.compat.BLOTransitionSkipHook;
import com.bettermountsteering.compat.BetterCombatHelper;
import com.bettermountsteering.compat.ControllableHelper;
import com.bettermountsteering.compat.EpicFightHelper;
import com.bettermountsteering.compat.GunModHelper;
import com.bettermountsteering.compat.IntegrationRegistry;
import com.bettermountsteering.compat.IronSpellsHelper;
import com.bettermountsteering.compat.SuperbWarfareHelper;
import com.bettermountsteering.compat.TaczHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

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
    private volatile float mountInputForward = 0F;
    private volatile float mountInputStrafe = 0F;

    private volatile boolean processingMouseTurn = false;

    private volatile boolean wasOnMountLastTick = false;

    private float blockedLockOnYRot = Float.NaN;
    private boolean wasLockingOnLastTick = false;
    private int postLockOffSmoothingTicks = 0;

    private int tpsAimLingerRemaining = 0;

    private static final long FIRE_LATCH_MS = 500L;
    private volatile long fireSignalMs = 0L;

    private float combatYawO = Float.NaN;
    private boolean hadCombatLastTick = false;
    private boolean wasCombatLastTick = false;
    private boolean wasTpsBackLastTick = false;

    private MountSteeringHandler() {}

    public static boolean isMountRotateActive()    { return INSTANCE.mountRotateActive; }
    public static boolean isTpsAimLingerActive()   { return INSTANCE.tpsAimLingerRemaining > 0; }
    public static float   getMountSmoothedYaw()    { return INSTANCE.mountSmoothedYaw; }
    public static float   getMountInputMagnitude() { return INSTANCE.mountInputMagnitude; }
    public static float   getMountInputForward()   { return INSTANCE.mountInputForward; }
    public static float   getMountInputStrafe()    { return INSTANCE.mountInputStrafe; }

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

    // Firing an already-loaded crossbow never sets isUsingItem, so the auto-turn's combat check misses it.
    // Latch instead of writing the yaw here: the combat branch keeps the strafe impulse, so the mount faces
    // the camera without the movement veering off, and combatJustEnded snaps the body back with no drift
    public static void snapToCameraNow(LocalPlayer player) {
        if (!BetterMountSteeringConfig.AUTO_FACE_ON_COMBAT.get()) return;
        if (Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
        if (EpicFightHelper.isLockOnTargeting()) return;
        if (!isOnMountedMob(player)) return;

        INSTANCE.fireSignalMs = System.currentTimeMillis();

        MountCameraSource source = activeCameraSource();
        float camYaw;
        if (source != null) {
            camYaw = source.yaw();
        } else {
            camYaw = INSTANCE.decoupleActive ? INSTANCE.decoupledCameraYaw : player.getYRot();
        }

        // ServerboundUseItemPacket carries no rotation, so a charged crossbow fires along whatever yaw was
        // last synced. While decoupled that's the mount body yaw, not the camera
        float target = Mth.wrapDegrees(camYaw);
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            conn.send(new ServerboundMovePlayerPacket.Rot(target, player.getXRot(), player.onGround()));
        }
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

    @Nullable
    private static MountCameraSource activeCameraSource() {
        MountCameraSource source = MountSteeringApi.getCameraSource();
        return source != null && source.isActive() ? source : null;
    }

    private static boolean isCombatActive(LocalPlayer player) {
        if (player.swinging) return true;
        if (BetterCombatHelper.isAttackInProgress()) return true;
        if (TaczHelper.isAimingOrFiring()) return true;
        if (GunModHelper.isGunFiring()) return true;
        if (SuperbWarfareHelper.isAimingOrFiring()) return true;
        if (IronSpellsHelper.isCasting() || IronSpellsHelper.anyCastKeymapDown() || IronSpellsHelper.isCastLatchActive()) return true;
        if (EpicFightHelper.isAttackAnimActive(player)) return true;
        if (player.isBlocking()) return true;
        if ((System.currentTimeMillis() - INSTANCE.fireSignalMs) < FIRE_LATCH_MS) return true;
        return player.isUsingItem();
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
        boolean tpsBack = Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK;
        boolean freshTpsBack = tpsBack && !wasTpsBackLastTick;
        wasTpsBackLastTick = tpsBack;
        if (!tpsBack) {
            deactivateDecouple(player);
            mountSmoothedYaw = Float.NaN;
            return false;
        }
        if (!nowOnMount) {
            float[] dir = readDirectionalInput(input);
            float magnitude = Mth.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
            boolean userIsMoving = magnitude >= 0.01F;
            boolean externalCamera = activeCameraSource() != null;

            if (externalCamera && userIsMoving) {
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
        boolean combat = isCombatActive(player);

        if (player.isUsingItem() || player.isBlocking()) {
            tpsAimLingerRemaining = tpsAimLingerTicks();
            if (!combat) {
                deactivateDecouple(player);
                return false;
            }
        }

        float[] dir = readDirectionalInput(input);
        float rawForward = dir[0];
        float rawStrafe  = dir[1];
        float rawMagnitude = Mth.sqrt(rawForward * rawForward + rawStrafe * rawStrafe);
        boolean combatIdle = combat && rawMagnitude < 0.01F;
        if (freshTpsBack && !decoupleActive) {
            MountCameraSource seedSource = activeCameraSource();
            decoupledCameraYaw  = seedSource != null ? seedSource.yaw()  : player.getYRot();
            decoupledCameraXRot = seedSource != null ? seedSource.xRot() : player.getXRot();
            if (Float.isNaN(mountSmoothedYaw)) mountSmoothedYaw = decoupledCameraYaw;
            decoupleActive = true;
        }

        if (rawMagnitude < 0.01F) {
            if (combat) {
                rawForward = 0F;
                rawStrafe = 0F;
                rawMagnitude = 0F;
            } else if (idleBehavior() == BetterMountSteeringConfig.IdleBehavior.HOLD_DIRECTION
                    && decoupleActive && !Float.isNaN(mountSmoothedYaw)) {
                Mob mount = (Mob) player.getVehicle();
                player.setYRot(mountSmoothedYaw);
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
                mountInputMagnitude = 0F;
                mountInputForward = 0F;
                mountInputStrafe = 0F;
                input.forwardImpulse = 0F;
                input.leftImpulse = 0F;
                mountRotateActive = true;
                return true;
            } else if (decoupleActive) {
                mountInputMagnitude = 0F;
                mountInputForward = 0F;
                mountInputStrafe = 0F;
                input.forwardImpulse = 0F;
                input.leftImpulse = 0F;
                mountRotateActive = true;
                return true;
            } else {
                deactivateDecouple(player);
                return false;
            }
        }

        boolean justExitedLockOn = Float.isNaN(mountSmoothedYaw);

        MountCameraSource source = activeCameraSource();
        float sourceYaw  = source != null ? source.yaw()  : player.getYRot();
        float sourceXRot = source != null ? source.xRot() : player.getXRot();

        if (source != null) {
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

        float offsetAngle = combat ? 0F : -(float) Math.toDegrees(Math.atan2(rawStrafe, rawForward));
        float bodyYaw     = Mth.wrapDegrees(decoupledCameraYaw + offsetAngle);
        boolean combatJustEnded = wasCombatLastTick && !combat;
        wasCombatLastTick = combat;

        if (freshMount || combatJustEnded) {
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

        float turnFactor = combat ? 1.0F : mountTurnSpeed();
        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, turnFactor);
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
        if (combat) {
            float slow = combatIdle ? 0F
                    : (BetterMountSteeringConfig.SLOW_MOUNT_ON_COMBAT.get() ? 0.5F : 1.0F);
            input.forwardImpulse = rawForward * slow;
            input.leftImpulse = rawStrafe * slow;
            mountInputMagnitude = magnitude * slow;
            mountInputForward = rawForward * slow;
            mountInputStrafe = rawStrafe * slow;
        } else {
            input.forwardImpulse = magnitude;
            input.leftImpulse = 0F;
            mountInputMagnitude = magnitude;
            mountInputForward = magnitude;
            mountInputStrafe = 0F;
        }

        decoupleActive = true;
        mountRotateActive = true;
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTickStartCombatSnap(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!BetterMountSteeringConfig.AUTO_FACE_ON_COMBAT.get()) return;
        if (Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
        if (EpicFightHelper.isLockOnTargeting()) return;
        if (!isOnMountedMob(player)) {
            combatYawO = Float.NaN;
            hadCombatLastTick = false;
            return;
        }
        if (!isCombatActive(player)) {
            combatYawO = Float.NaN;
            hadCombatLastTick = false;
            return;
        }

        MountCameraSource source = activeCameraSource();
        float camYaw;
        if (source != null) {
            camYaw = source.yaw();
        } else {
            camYaw = decoupleActive ? decoupledCameraYaw : player.getYRot();
        }

        float target = Mth.wrapDegrees(camYaw);

        player.setYRot(target);
        if (player.getVehicle() instanceof Mob mount) {
            mount.setYRot(target);
        }
        mountSmoothedYaw = target;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!event.side.isClient()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.player != player) return;

        if (mountRotateActive) {
            boolean combat = isCombatActive(player);
            boolean hasInterpPrev = combat && hadCombatLastTick && !Float.isNaN(combatYawO);
            player.setYRot(mountSmoothedYaw);
            player.yBodyRot = mountSmoothedYaw;
            player.yHeadRot = mountSmoothedYaw;
            if (hasInterpPrev) {
                player.yBodyRotO = combatYawO;
                player.yHeadRotO = combatYawO;
            }
            if (player.getVehicle() instanceof Mob mount) {
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
                if (hasInterpPrev) {
                    mount.yBodyRotO = combatYawO;
                }
            }
            if (combat) {
                combatYawO = mountSmoothedYaw;
                hadCombatLastTick = true;
            } else {
                hadCombatLastTick = false;
                combatYawO = Float.NaN;
            }
        }

        if (decoupleActive && decoupleTransitioning) {
            MountCameraSource source = activeCameraSource();
            if (source != null) {
                decoupledCameraYaw = source.yaw();
                decoupledCameraXRot = source.xRot();
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
                if (source != null) {
                    source.onDecoupleEnd(decoupledCameraYaw);
                }
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
    public void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
