package com.bettermountsteering.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// CGM bullets only skip the shooter in entity raycasts, not the mount. At close range the horse
// hitbox eats shots in every camera mode. Ignore the controlling vehicle too. This is mount/gun
// physics, not SSR-facing — lives here so SSR fixes stays free of non-SSR gun collision work.
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.entity.ProjectileEntity", remap = false)
public abstract class MixinCgmProjectileIgnoreMount {

    @Redirect(
        method = "findEntityOnPath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;equals(Ljava/lang/Object;)Z"
        ),
        require = 0,
        remap = false
    )
    private boolean bettermountsteering$skipMountOnPath(Entity candidate, Object shooterObj) {
        return skipShooterOrMount(candidate, shooterObj);
    }

    @Redirect(
        method = "findEntitiesOnPath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;equals(Ljava/lang/Object;)Z"
        ),
        require = 0,
        remap = false
    )
    private boolean bettermountsteering$skipMountOnPaths(Entity candidate, Object shooterObj) {
        return skipShooterOrMount(candidate, shooterObj);
    }

    private static boolean skipShooterOrMount(Entity candidate, Object shooterObj) {
        if (candidate.equals(shooterObj)) return true;
        if (shooterObj instanceof LivingEntity shooter) {
            Entity vehicle = shooter.getVehicle();
            return vehicle != null && vehicle.equals(candidate);
        }
        return false;
    }
}
