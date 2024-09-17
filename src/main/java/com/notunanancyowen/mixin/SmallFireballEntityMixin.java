package com.notunanancyowen.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends AbstractFireballEntity {
    SmallFireballEntityMixin(EntityType<? extends SmallFireballEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void doCoolFireExplosion(EntityHitResult entityHitResult, CallbackInfo ci) {
        particleExplosion(entityHitResult.getPos());
    }
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void doCoolFlameExplosion(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (getOwner() != null && getOwner().getType() == EntityType.EVOKER) {
            super.onBlockHit(blockHitResult);
            ci.cancel();
        }
        particleExplosion(blockHitResult.getPos());
    }
    @Inject(method = "onCollision", at = @At("HEAD"))
    private void coolExplosionEffectIGuess(HitResult hitResult, CallbackInfo ci) {
        particleExplosion(hitResult.getPos());
    }
    @Inject(method = "canHit", at = @At("TAIL"), cancellable = true)
    private void getParried1(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getOwner() != null && getOwner().getType() == EntityType.EVOKER);
    }
    @Inject(method = "damage", at = @At("TAIL"), cancellable = true)
    private void getParried2(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(canHit() && !isInvulnerableTo(source));
    }
    @Unique private void particleExplosion(Vec3d pos) {
        try {
            if(getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, pos.getX(), pos.getY(), pos.getZ(), 3, 0.1d, 0.1d, 0.1d, 0.1d);
        }
        catch (Throwable ignore) {
        }
    }
}
