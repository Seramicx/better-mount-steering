package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class IntegrationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean betterLockOn;
    private static boolean epicFight;
    private static boolean controllable;
    private static boolean resolved = false;

    private IntegrationRegistry() {}

    public static void resolve() {
        if (resolved) return;
        resolved = true;

        ModList mods = ModList.get();
        betterLockOn    = mods.isLoaded("betterlockon");
        epicFight       = mods.isLoaded("epicfight");
        controllable    = mods.isLoaded("controllable");

        LOGGER.info(
            "Companion mods: BLO:{} EpicFight:{} Controllable:{}",
            betterLockOn, epicFight, controllable);
    }

    public static boolean isBetterLockOn()    { ensure(); return betterLockOn; }
    public static boolean isEpicFight()       { ensure(); return epicFight; }
    public static boolean isControllable()    { ensure(); return controllable; }

    private static void ensure() { if (!resolved) resolve(); }
}
