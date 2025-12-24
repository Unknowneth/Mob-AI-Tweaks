package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin {
    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;teleportTo(Lnet/minecraft/world/TeleportTarget;)Lnet/minecraft/entity/Entity;"))
    private Entity redirectTpToMount(Entity instance, TeleportTarget teleportTarget) {
        if(instance.getVehicle() instanceof PathAwareEntity p) {
            p.teleportTo(teleportTarget);
            return p;
        }
        instance.teleportTo(teleportTarget);
        return instance;
    }
    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onLanding()V"))
    private void redirectLandingToMount(Entity instance) {
        if(instance.getVehicle() instanceof PathAwareEntity p) p.onLanding();
        else instance.onLanding();
    }
    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;detach()V"))
    private void redirectDismountToMount(Entity instance) {
        if(instance.getVehicle() instanceof PathAwareEntity p) p.stopRiding();
        else instance.detach();
    }
}
