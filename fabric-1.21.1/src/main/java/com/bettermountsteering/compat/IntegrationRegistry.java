package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class IntegrationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean controllable;
    private static boolean shoulderSurfing;
    private static boolean resolved = false;

    private IntegrationRegistry() {}

    public static void resolve() {
        if (resolved) return;
        resolved = true;

        FabricLoader loader = FabricLoader.getInstance();
        controllable    = loader.isModLoaded("controllable");
        shoulderSurfing = loader.isModLoaded("shouldersurfing");

        LOGGER.info("Companion mods: Controllable:{} SSR:{}", controllable, shoulderSurfing);
    }

    public static boolean isControllable()    { ensure(); return controllable; }
    public static boolean isShoulderSurfing() { ensure(); return shoulderSurfing; }

    private static void ensure() { if (!resolved) resolve(); }
}
