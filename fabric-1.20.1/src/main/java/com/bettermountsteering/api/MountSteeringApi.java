package com.bettermountsteering.api;

import org.jetbrains.annotations.Nullable;

public final class MountSteeringApi {

    @Nullable private static volatile MountCameraSource cameraSource;

    private MountSteeringApi() {}

    public static void setCameraSource(@Nullable MountCameraSource source) {
        cameraSource = source;
    }

    @Nullable
    public static MountCameraSource getCameraSource() {
        return cameraSource;
    }
}
