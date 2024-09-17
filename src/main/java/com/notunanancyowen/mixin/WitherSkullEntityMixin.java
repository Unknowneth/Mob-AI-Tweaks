package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullEntityMixin extends ExplosiveProjectileEntity {
    @Shadow public abstract boolean isCharged();
    @Shadow protected abstract float getDrag();
    @Unique private boolean spawned = false;
    @Unique private boolean canBluesHome = true;
    WitherSkullEntityMixin(EntityType<? extends WitherSkullEntity> type, World world) {
        super(type, world);
    }
    @Override public void tick() {
        if(!spawned) {
            if(getWorld().isClient()) {
                for (int i = 0; i < 12; i++) getWorld().addParticle(isCharged() && i > 6 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, getPos().getX(), getPos().getY(), getPos().getZ(), random.nextBetween(-100, 100) * 0.002d, random.nextBetween(-100, 100) * 0.002d, random.nextBetween(-100, 100) * 0.002d);
                if (isCharged()) getWorld().addParticle(ParticleTypes.FLASH, getPos().getX(), getPos().getY(), getPos().getZ(), 0d, 0d, 0d);
            }
            super.tick();
            ProjectileUtil.setRotationFromVelocity(this, 1.0F);
            spawned = true;
            canBluesHome = MobAITweaks.getModConfigValue("wither_rework");
        }
        if(isCharged() && canBluesHome) {
            if(getOwner() instanceof HostileEntity shooter && shooter.getTarget() != null && (shooter.getTarget().isBlocking() || shooter.getTarget().isOnGround() || distanceTo(shooter.getTarget()) > 2) && shooter.getTarget().isAlive() && spawned) {
                powerX = powerY = powerZ = 0d;
                super.tick();
                setVelocity(getVelocity().multiply(getDrag()));
                addVelocity(shooter.getTarget().getEyePos().subtract(getPos()).normalize().multiply(0.17d));
            }
            else {
                if(powerX == 0d || powerY == 0d || powerZ == 0d) {
                    powerX = getVelocity().normalize().getX() * 0.1d;
                    powerY = getVelocity().normalize().getY() * 0.1d;
                    powerZ = getVelocity().normalize().getZ() * 0.1d;
                }
                super.tick();
            }
            velocityDirty = true;
            if(age % 2 == 0 && getWorld().isClient()) getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getPos().getX(), getPos().getY(), getPos().getZ(), random.nextBetween(-100, 100) * 0.001d, random.nextBetween(-100, 100) * 0.001d + 0.02d, random.nextBetween(-100, 100) * 0.001d);
        }
        else if(spawned) super.tick();
    }
}
