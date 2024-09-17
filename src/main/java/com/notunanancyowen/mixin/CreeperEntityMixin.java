package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends HostileEntity {
    @Shadow protected abstract void spawnEffectsCloud();
    @Shadow private int explosionRadius;
    @Shadow @Final private static TrackedData<Integer> FUSE_SPEED;
    @Shadow public abstract boolean isIgnited();
    @Shadow public abstract boolean shouldRenderOverlay();
    CreeperEntityMixin(EntityType<? extends CreeperEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void explodeButWithExtraSteps(CallbackInfo ci) {
        if (MobAITweaks.getModConfigValue("creeper_explosions_rework") && getWorld() instanceof ServerWorld server) {
            float f = shouldRenderOverlay() ? 2.0F : 1.0F;
            for(int i = 0; i < getStuckArrowCount(); i++) {
                ArrowEntity arrows = new ArrowEntity(getWorld(), 0d, 0d, 0d);
                arrows.setOwner(this);
                arrows.initFromStack(Items.ARROW.getDefaultStack());
                arrows.setVelocity(getRandom().nextGaussian() * f * explosionRadius, getRandom().nextGaussian() * f * explosionRadius, getRandom().nextGaussian() * f * explosionRadius);
                arrows.setPosition(arrows.getPos().add(arrows.getVelocity()));
                arrows.setVelocity(arrows.getVelocity().normalize().multiply(f / Math.max(explosionRadius, 1)));
                arrows.setFireTicks(getFireTicks());
                arrows.setCritical(true);
                server.spawnEntity(arrows);
            }
            boolean bl = getFireTicks() > 0;
            dead = true;
            if(bl) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, getX(), getY(), getZ(), explosionRadius * (int)f, 0.1d, 0.1d, 0.1d, f * 0.1d);
            getWorld().createExplosion(this, getX(), getY(), getZ(), (float)explosionRadius * f, bl, World.ExplosionSourceType.MOB);
            spawnEffectsCloud();
            discard();
        }
        ci.cancel();
    }
    @Inject(method = "setFuseSpeed", at = @At("HEAD"), cancellable = true)
    private void edgeExplosionTimer(int fuseSpeed, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("creeper_explosions_rework") && !isIgnited() && fuseSpeed > 0 && hurtTime > 0 && hurtTime < lastDamageTaken) fuseSpeed = -fuseSpeed;
        getDataTracker().set(FUSE_SPEED, fuseSpeed);
        ci.cancel();
    }
}
