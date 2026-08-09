package com.bettermountsteering.compat;

import com.bettermountsteering.handler.MountSteeringHandler;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.event.types.ActivateTPSCamera;
import yesman.epicfight.api.event.subscriptions.DefaultEventSubscription;

public final class EpicFightTPSDecoupleHook {

    private static boolean registered = false;

    private EpicFightTPSDecoupleHook() {}

    public static void register() {
        if (registered) return;
        if (!IntegrationRegistry.isEpicFight()) return;
        // SSR already disables EF TPS; a second subscriber breaks on-foot SSR bow aim
        if (IntegrationRegistry.isShoulderSurfing()) {
            registered = true;
            return;
        }
        registered = true;

        try {
            DefaultEventSubscription<ActivateTPSCamera> sub = event -> {
                if (MountSteeringHandler.isMountRotateActive() || MountSteeringHandler.isTpsAimLingerActive()) {
                    event.cancel();
                }
            };
            EpicFightClientHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(sub);
        } catch (Throwable ignored) {
            // Optional compat — EF API mismatch should not break client startup
        }
    }
}
