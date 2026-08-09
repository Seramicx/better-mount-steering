package com.bettermountsteering.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// Beam guns trace through this and getEntities only skips the shooter, so a rider's beam clips the horse's
// hitbox before anything else. The trace is rerun here rather than redirecting getEntities because @Pseudo
// skips the annotation processor, so no refmap is written and a vanilla @At target never resolves to SRG
@Pseudo
@Mixin(targets = "top.ribs.scguns.common.network.ServerPlayHandler", remap = false)
public abstract class MixinScorchedBeamIgnoreMount {

    @Inject(method = "rayTraceEntities", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bettermountsteering$skipShooterMount(Level world, Entity shooter, Vec3 startVec, Vec3 endVec,
                                                             CallbackInfoReturnable<EntityHitResult> cir) {
        if (shooter == null) return;
        Entity vehicle = shooter.getVehicle();
        if (vehicle == null) return;

        double minDistance = startVec.distanceTo(endVec);
        AABB searchArea = new AABB(startVec, endVec).inflate(1.0D);
        Entity closest = null;
        Vec3 hitVec = null;

        for (Entity entity : world.getEntities(shooter, searchArea,
                e -> e != vehicle && !e.isSpectator() && e.isPickable() && e.isAlive())) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3D).clip(startVec, endVec);
            if (hit.isEmpty()) continue;
            double distance = startVec.distanceTo(hit.get());
            if (distance >= minDistance) continue;
            minDistance = distance;
            closest = entity;
            hitVec = hit.get();
        }

        cir.setReturnValue(closest == null ? null : new EntityHitResult(closest, hitVec));
    }
}
