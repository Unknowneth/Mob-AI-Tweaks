package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

import static net.minecraft.entity.projectile.AbstractWindChargeEntity.EXPLOSION_BEHAVIOR;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends AbstractFireballEntity {
    SmallFireballEntityMixin(EntityType<? extends SmallFireballEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void doCoolFireExplosion(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(!getWorld().isClient && entityHitResult.getEntity() instanceof AbstractWindChargeEntity wc) try {
            getWorld().createExplosion(wc, null, EXPLOSION_BEHAVIOR, getX(), getY(), getZ(), 2.5F, true, World.ExplosionSourceType.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST);
            getWorld().createExplosion(this, getX(), getY(), getZ(), 2.5F, true, World.ExplosionSourceType.MOB);
            if(getOwner() instanceof LivingEntity owner) owner.updateKilledAdvancementCriterion(this, 0, getDamageSources().windCharge(wc, owner));
            for(int i = 0; i < 5; i++) particleExplosion(wc.getPos());
            wc.discard();
        }
        catch (Throwable ignore) {
        }
        particleExplosion(entityHitResult.getPos());
    }
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void doCoolFlameExplosion(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (getOwner() != null && (getOwner().getType() == EntityType.EVOKER || getOwner().getType() == EntityType.ALLAY)) {
            super.onBlockHit(blockHitResult);
            ci.cancel();
        }
        particleExplosion(blockHitResult.getPos());
    }
    @Inject(method = "onCollision", at = @At("HEAD"))
    private void coolExplosionEffectIGuess(HitResult hitResult, CallbackInfo ci) {
        particleExplosion(hitResult.getPos());
    }
    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    private void getParried(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(canHit() && !isInvulnerableTo(source));
    }
    @Override public boolean canHit() {
        return getOwner() != null && getOwner().getType() == EntityType.EVOKER && getVehicle() == null;
    }
    @Unique private void particleExplosion(Vec3d pos) {
        if(getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, pos.getX(), pos.getY(), pos.getZ(), 3, 0.1d, 0.1d, 0.1d, 0.1d);
    }
}
