package com.notunanancyowen.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin extends ProjectileEntity {
    @Shadow public abstract boolean isShotFromCrossbow();
    @Shadow public abstract void setCritical(boolean critical);
    @Shadow public abstract boolean isCritical();
    PersistentProjectileEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "setVelocity", at = @At("HEAD"))
    private void shootThisGuy(double x, double y, double z, float power, float uncertainty, CallbackInfo ci) {
        if(isShotFromCrossbow() && !isCritical() && getOwner() instanceof HostileEntity mob && mob.getTarget() != null) setCritical(true);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnFireWhenBurning(CallbackInfo ci) {
        if(isOnFire() && getWorld() instanceof ServerWorld server) server.spawnParticles(age % 2 == 0 ?  isCritical() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1d, 0.1d, 0.1d, 0.05d);
    }
}
