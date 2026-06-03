package com.bettermountsteering.compat;

import com.bettermountsteering.handler.MountSteeringHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.event.types.ActivateTPSCamera;
import yesman.epicfight.api.event.subscriptions.DefaultEventSubscription;

public final class EpicFightTPSDecoupleHook {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered = false;

    private EpicFightTPSDecoupleHook() {}

    public static void register() {
        if (registered) return;
        if (!IntegrationRegistry.isEpicFight()) return;
        registered = true;

        try {
            DefaultEventSubscription<ActivateTPSCamera> sub = event -> {
                if (MountSteeringHandler.isMountRotateActive() || MountSteeringHandler.isTpsAimLingerActive()) {
                    event.cancel();
                }
            };
            EpicFightClientHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(sub);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register Epic Fight TPS-cancel hook: {}", t.getMessage());
        }
    }
}
