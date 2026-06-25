package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class BetterCombatHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BC_API_CLASS = "net.bettercombat.api.MinecraftClient_BetterCombat";
    private static final String BC_API_METHOD = "isWeaponSwingInProgress";

    private static volatile Method isWeaponSwingInProgressMethod = null;
    private static volatile boolean methodResolved = false;
    private static volatile boolean bcPresent = false;

    private BetterCombatHelper() {}

    private static synchronized void resolveMethod() {
        if (methodResolved) return;
        methodResolved = true;
        try {
            Class<?> apiClass = Class.forName(BC_API_CLASS);
            isWeaponSwingInProgressMethod = apiClass.getMethod(BC_API_METHOD);
            bcPresent = true;
            LOGGER.info("Better Combat compat: bound to MinecraftClient_BetterCombat.isWeaponSwingInProgress()");
        } catch (ClassNotFoundException notLoaded) {
        } catch (Throwable t) {
            LOGGER.warn("Better Combat compat: failed to bind: {}", t.toString());
        }
    }

    public static boolean isAttackInProgress() {
        if (!methodResolved) resolveMethod();
        if (!bcPresent) return false;
        Method m = isWeaponSwingInProgressMethod;
        if (m == null) return false;
        try {
            Object result = m.invoke(Minecraft.getInstance());
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }
}
