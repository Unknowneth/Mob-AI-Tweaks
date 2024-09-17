package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DragonFireballEntity.class)
public abstract class DragonFireballEntityMixin extends ExplosiveProjectileEntity {
    @Unique private boolean spawned = false;
    @Unique private PlayerEntity target = null;
    DragonFireballEntityMixin(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void changeBehavior(HitResult hitResult, CallbackInfo ci) {
        if(hitResult.getType().equals(HitResult.Type.ENTITY) && (((EntityHitResult)hitResult).getEntity().equals(getOwner()) || ((EntityHitResult)hitResult).getEntity() instanceof EnderDragonPart || ((EntityHitResult)hitResult).getEntity() instanceof EnderDragonAttacksInterface)) {
            ci.cancel();
            return;
        }
        super.onCollision(hitResult);
        if (!this.getWorld().isClient()) {
            List<LivingEntity> list = getWorld().getNonSpectatingEntities(LivingEntity.class, getBoundingBox().expand(4.0, 2.0, 4.0));
            AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(getWorld(), getX(), getY(), getZ());
            Entity entity = getOwner();
            boolean isEnraged = false;
            if (entity instanceof LivingEntity) {
                areaEffectCloudEntity.setOwner((LivingEntity)entity);
                if(entity instanceof EnderDragonAttacksInterface enrageCheck) isEnraged = enrageCheck.isEnraged();
            }
            areaEffectCloudEntity.setParticleType(ParticleTypes.DRAGON_BREATH);
            areaEffectCloudEntity.setRadius(isEnraged ? 4.0F : 3.0F);
            areaEffectCloudEntity.setDuration(60);
            areaEffectCloudEntity.setRadiusGrowth((7.0F - areaEffectCloudEntity.getRadius()) / (float)areaEffectCloudEntity.getDuration());
            areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1,  isEnraged ? 2 : 1));
            if (!list.isEmpty()) for (LivingEntity livingEntity : list) if (squaredDistanceTo(livingEntity) < 16.0) {
                areaEffectCloudEntity.setPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
                break;
            }
            getWorld().syncWorldEvent(WorldEvents.DRAGON_BREATH_CLOUD_SPAWNS, getBlockPos(), isSilent() ? -1 : 1);
            getWorld().spawnEntity(areaEffectCloudEntity);
            getWorld().createExplosion(entity, getX(), getY(), getZ(), isEnraged ? 4.0F : 3.0F, World.ExplosionSourceType.NONE);
            discard();
        }
        ci.cancel();
    }
    @Override public void tick() {
        if(!spawned) {
            if(MobAITweaks.getModConfigValue("ender_dragon_rework") && getOwner() != null && getOwner().getType() == EntityType.ENDER_DRAGON) target = getWorld().getClosestPlayer(getX(), getY(), getZ(), 128, p -> p instanceof PlayerEntity p2 && !p2.isSpectator() && !p2.isCreative() && p2.isFallFlying());
            spawned = true;
        }
        super.tick();
        if(spawned) if(target != null) if(!target.isFallFlying()) {
            target = null;
            powerX = getVelocity().x;
            powerY = getVelocity().y;
            powerZ = getVelocity().z;
        }
        else setVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(getVelocity().length()));
    }
    @Override protected void onEntityHit(EntityHitResult entityHitResult) {
        if(!entityHitResult.getEntity().equals(getOwner()) && !(entityHitResult.getEntity() instanceof EnderDragonPart) && !(entityHitResult.getEntity() instanceof EnderDragonAttacksInterface)) super.onEntityHit(entityHitResult);
    }
    @Override protected boolean canHit(Entity entity) {
        return !isOwner(entity) && !(entity instanceof EnderDragonPart) && !(entity instanceof EnderDragonAttacksInterface) && super.canHit(entity);
    }
}
