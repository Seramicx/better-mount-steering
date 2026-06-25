package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class EpicFightHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean stateResolved = false;
    @Nullable private static Method getEntityPatchMethod = null;
    @Nullable private static Class<?> livingEntityPatchClass = null;
    @Nullable private static Method getEntityStateMethod = null;
    @Nullable private static Method getLevelMethod = null;

    private static boolean lockOnResolved = false;
    @Nullable private static Class<?> localPlayerPatchClass = null;
    @Nullable private static Method isTargetLockedOnMethod = null;

    private EpicFightHelper() {}

    private static void resolveState() {
        if (stateResolved) return;
        stateResolved = true;
        if (!IntegrationRegistry.isEpicFight()) return;
        try {
            Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            getEntityPatchMethod = caps.getMethod("getEntityPatch", Entity.class, Class.class);
            livingEntityPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            getEntityStateMethod = livingEntityPatchClass.getMethod("getEntityState");
            Class<?> stateCls = Class.forName("yesman.epicfight.api.animation.types.EntityState");
            getLevelMethod = stateCls.getMethod("getLevel");
        } catch (Throwable t) {
            LOGGER.debug("EpicFight EntityState reflection unavailable: {}", t.getMessage());
        }
    }

    private static void resolveLockOn() {
        if (lockOnResolved) return;
        lockOnResolved = true;
        if (!IntegrationRegistry.isEpicFight()) return;
        try {
            localPlayerPatchClass = Class.forName("yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch");
            isTargetLockedOnMethod = localPlayerPatchClass.getMethod("isTargetLockedOn");
        } catch (Throwable t) {
            LOGGER.debug("EpicFight lock-on reflection unavailable: {}", t.getMessage());
        }
    }

    public static boolean isLockOnTargeting() {
        resolveLockOn();
        if (localPlayerPatchClass == null || isTargetLockedOnMethod == null) return false;
        resolveState();
        if (getEntityPatchMethod == null) return false;
        try {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            Object patch = getEntityPatchMethod.invoke(null, player, localPlayerPatchClass);
            if (patch == null) return false;
            return (Boolean) isTargetLockedOnMethod.invoke(patch);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isAttackAnimActive(LocalPlayer player) {
        if (player == null) return false;
        resolveState();
        if (getEntityPatchMethod == null || livingEntityPatchClass == null
                || getEntityStateMethod == null) return false;
        try {
            Object patch = getEntityPatchMethod.invoke(null, player, livingEntityPatchClass);
            if (patch == null) return false;
            Object state = getEntityStateMethod.invoke(patch);
            if (state == null) return false;
            if (getLevelMethod != null && ((Integer) getLevelMethod.invoke(state)) > 0) return true;
        } catch (Throwable ignored) {}
        return false;
    }
}
