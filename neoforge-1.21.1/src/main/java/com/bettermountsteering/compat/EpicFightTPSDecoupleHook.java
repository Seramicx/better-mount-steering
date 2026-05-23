package com.bettermountsteering.compat;

import com.bettermountsteering.handler.MountSteeringHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.camera.ActivateTPSCamera;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;

/**
 * Cancels EF's TPS camera mode while mount-rotate decouple is active so EF
 * defers to vanilla camera and our decouple mixins take over.
 *
 * <p>Gated by {@link IntegrationRegistry#isEpicFight()} at caller — this
 * class is never loaded when EF is absent.
 */
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
                if (MountSteeringHandler.isDecoupleActive()) {
                    event.cancel();
                }
            };
            EpicFightClientEventHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(sub);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register Epic Fight TPS-cancel hook: {}", t.getMessage());
        }
    }
}
