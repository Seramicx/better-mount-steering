package com.bettermountsteering.handler;

import com.bettermountsteering.BetterMountSteeringConfig;
import com.bettermountsteering.api.MountCameraSource;
import com.bettermountsteering.api.MountSteeringApi;
import com.bettermountsteering.compat.BetterCombatHelper;
import com.bettermountsteering.compat.ControllableHelper;
import com.bettermountsteering.compat.EpicFightHelper;
import com.bettermountsteering.compat.GunModHelper;
import com.bettermountsteering.compat.IronSpellsHelper;
import com.bettermountsteering.compat.TaczHelper;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    private float combatYawO = Float.NaN;
    private boolean hadCombatLastTick = false;
    private boolean wasCombatLastTick = false;
    private boolean wasTpsBackLastTick = false;
    private boolean wasCastKeyDownLastTick = false;

    private MountSteeringHandler() {}

    public static boolean isMountRotateActive()    { return INSTANCE.mountRotateActive; }
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

    private float mountTurnSpeed() {
        return (float) BetterMountSteeringConfig.MOUNT_TURN_SPEED.get().doubleValue();
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
        if (IronSpellsHelper.isCasting() || IronSpellsHelper.anyCastKeymapDown()) return true;
        if (IronSpellsHelper.isCastingAnySpell()) return true;
        if (IronSpellsHelper.isCastLatchActive()) return true;
        if (EpicFightHelper.isAttackAnimActive(player)) return true;
        if (TaczHelper.isAimingOrFiring()) return true;
        if (GunModHelper.isGunFiring()) return true;
        if (player.isBlocking()) return true;
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

        if ((player.isUsingItem() || player.isBlocking()) && !combat) {
            deactivateDecouple(player);
            return false;
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
        }

        float turnFactor = combat ? 1.0F : mountTurnSpeed();
        mountSmoothedYaw = smoothAngle(mountSmoothedYaw, bodyYaw, turnFactor);
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
        if (combat) {
            float slow = combatIdle ? 0F : 0.5F;
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

        boolean castNow = IronSpellsHelper.anyCastKeymapDown() || IronSpellsHelper.isCasting()
                || IronSpellsHelper.isCastingAnySpell()
                || (player.isUsingItem() && IronSpellsHelper.isIronsItem(player.getUseItem().getItem()));
        if (castNow && !wasCastKeyDownLastTick) IronSpellsHelper.signalCast();
        wasCastKeyDownLastTick = castNow;

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

        if (mountRotateActive) {
            player.setYRot(mountSmoothedYaw);
            player.yBodyRot = mountSmoothedYaw;
            player.yHeadRot = mountSmoothedYaw;
            if (player.getVehicle() instanceof Mob mount) {
                mount.setYRot(mountSmoothedYaw);
                mount.yBodyRot = mountSmoothedYaw;
            }
        }

        // The mount's head tracks where the rider looks, twisting away from its body when BMS aims the rider at
        // the camera. Keep the mount's head aligned with its body and level while mounted in third person.
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON
                && isOnMountedMob(player) && player.getVehicle() instanceof Mob mount) {
            mount.yHeadRot = mount.yBodyRot;
            mount.yHeadRotO = mount.yBodyRotO;
            mount.setXRot(0F);
            mount.xRotO = 0F;
        }
    }
}
