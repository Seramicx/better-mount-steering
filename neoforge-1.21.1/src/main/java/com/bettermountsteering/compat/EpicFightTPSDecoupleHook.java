package com.bettermountsteering.compat;

import com.bettermountsteering.handler.MountSteeringHandler;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.camera.ActivateTPSCamera;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;

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
            EpicFightClientEventHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(sub);
        } catch (Throwable ignored) {
            // Optional compat — EF API mismatch should not break client startup
        }
    }
}
