package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin extends ProjectileEntity {
    @Shadow public abstract boolean isShotFromCrossbow();
    @Shadow private @Nullable ItemStack weapon;
    @Shadow public abstract void setCritical(boolean critical);
    @Shadow public abstract boolean isCritical();
    PersistentProjectileEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
    }
    @WrapMethod(method = "writeCustomDataToNbt")
    private void antiCrashMeasure1(NbtCompound nbt, Operation<Void> original) {
        try {
            original.call(nbt);
        }
        catch (Throwable t) {
            discard();
            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
    }
    @WrapMethod(method = "readCustomDataFromNbt")
    private void antiCrashMeasure2(NbtCompound nbt, Operation<Void> original) {
        try {
            original.call(nbt);
        }
        catch (Throwable t) {
            discard();
            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
    }
    @Inject(method = "setVelocity", at = @At("HEAD"))
    private void shootThisGuy(double x, double y, double z, float power, float uncertainty, CallbackInfo ci) {
        if(isOnFire() && getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.1d, 0.1d, 0.1d, 0.1d);
        if(getOwner() instanceof HostileEntity mob) if(weapon != null && (isShotFromCrossbow() || weapon.getItem() instanceof CrossbowItem) && !isCritical() && mob.getTarget() != null) setCritical(true);
        else if(mob.getType().isIn(MobAITweaks.ALWAYS_SHOOTS_CRIT_ARROWS)) setCritical(true);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnFireWhenBurning(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("burning_projectile_visuals") && isOnFire() && getWorld() instanceof ServerWorld server) server.spawnParticles(age % 2 == 0 ?  isCritical() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1d, 0.1d, 0.1d, 0.05d);
    }
}
