package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MagmaCubeEntity.class)
public abstract class MagmaCubeEntityMixin extends SlimeEntity {
    MagmaCubeEntityMixin(EntityType<? extends SlimeEntity> type, World world) {
        super(type, world);
    }
    @Override public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if(getWorld().isClient() || !isOnGround() || !getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return;
        AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(getWorld(), getX(), getY(), getZ()) {
            @Override public void tick() {
                super.tick();
                getWorld().getOtherEntities(this, getBoundingBox(), e -> e.isAlive() && !e.isFireImmune() && !e.isTouchingWater() && squaredDistanceTo(e.getX(), getY(), e.getZ()) < getRadius()).forEach(e -> e.setFireTicks(60));
            }
        };
        aoe.setParticleType(ParticleTypes.SMALL_FLAME);
        aoe.setOwner(this);
        aoe.setRadius(getSize());
        aoe.setWaitTime(20);
        aoe.setDuration(60 + getSize() * 5);
        getWorld().spawnEntity(aoe);
    }
}
