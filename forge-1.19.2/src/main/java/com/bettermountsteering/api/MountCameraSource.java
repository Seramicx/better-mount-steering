package com.bettermountsteering.api;

public interface MountCameraSource {

    boolean isActive();

    float yaw();

    float xRot();

    void onDecoupleEnd(float yaw);
}
