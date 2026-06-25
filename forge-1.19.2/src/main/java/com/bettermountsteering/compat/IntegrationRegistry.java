package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class IntegrationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean controllable;
    private static boolean shoulderSurfing;
    private static boolean epicFight;
    private static boolean resolved = false;

    private IntegrationRegistry() {}

    public static void resolve() {
        if (resolved) return;
        resolved = true;

        ModList mods = ModList.get();
        controllable    = mods.isLoaded("controllable");
        shoulderSurfing = mods.isLoaded("shouldersurfing");
        epicFight       = mods.isLoaded("epicfight");

        LOGGER.info("Companion mods: Controllable:{} SSR:{} EpicFight:{}", controllable, shoulderSurfing, epicFight);
    }

    public static boolean isControllable()    { ensure(); return controllable; }
    public static boolean isShoulderSurfing() { ensure(); return shoulderSurfing; }
    public static boolean isEpicFight()       { ensure(); return epicFight; }

    private static void ensure() { if (!resolved) resolve(); }
}
