package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

public final class IntegrationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean epicFight;
    private static boolean controllable;
    private static boolean shoulderSurfing;
    private static boolean resolved = false;

    private IntegrationRegistry() {}

    public static void resolve() {
        if (resolved) return;
        resolved = true;

        ModList mods = ModList.get();
        epicFight       = mods.isLoaded("epicfight");
        controllable    = mods.isLoaded("controllable");
        shoulderSurfing = mods.isLoaded("shouldersurfing");

        LOGGER.info(
            "Companion mods: EpicFight:{} Controllable:{} SSR:{}",
            epicFight, controllable, shoulderSurfing);
    }

    public static boolean isEpicFight()       { ensure(); return epicFight; }
    public static boolean isControllable()    { ensure(); return controllable; }
    public static boolean isShoulderSurfing() { ensure(); return shoulderSurfing; }

    private static void ensure() { if (!resolved) resolve(); }
}
