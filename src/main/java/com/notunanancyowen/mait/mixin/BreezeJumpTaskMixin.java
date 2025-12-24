package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.task.BreezeJumpTask;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.entity.projectile.AbstractWindChargeEntity.EXPLOSION_BEHAVIOR;

@Mixin(BreezeJumpTask.class)
public abstract class BreezeJumpTaskMixin {
    @Inject(method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/BreezeEntity;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/BreezeEntity;setNoDrag(Z)V"))
    private void onJumpOrOnLand(ServerWorld serverWorld, BreezeEntity breezeEntity, long l, CallbackInfo ci) {
        if(serverWorld.getDifficulty().getId() > 2) serverWorld.createExplosion(breezeEntity, null, EXPLOSION_BEHAVIOR, breezeEntity.getX(), breezeEntity.getY(), breezeEntity.getZ(), 2.5F, false, World.ExplosionSourceType.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST);
        if(MobAITweaks.isOminous(breezeEntity.getTarget()) && breezeEntity.getPose() == EntityPose.LONG_JUMPING) breezeEntity.setPose(EntityPose.SHOOTING);
    }
    @Inject(method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/BreezeEntity;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/BreezeJumpTask;shouldStopInhalingPose(Lnet/minecraft/entity/mob/BreezeEntity;)Z"))
    private void isJumpingAndInTheAir(ServerWorld serverWorld, BreezeEntity breezeEntity, long l, CallbackInfo ci) {
        if(MobAITweaks.isOminous(breezeEntity.getTarget())) {
            if(breezeEntity.getVelocity().getY() < breezeEntity.getFinalGravity() && breezeEntity.getPose() == EntityPose.SHOOTING) {
                double d = breezeEntity.getTarget().getX() - breezeEntity.getX();
                double e = breezeEntity.getTarget().getBodyY(breezeEntity.getTarget().hasVehicle() ? 0.8 : 0.3) - breezeEntity.getBodyY(0.5);
                double f = breezeEntity.getTarget().getZ() - breezeEntity.getZ();
                BreezeWindChargeEntity breezeWindChargeEntity = new BreezeWindChargeEntity(breezeEntity, serverWorld);
                breezeEntity.playSound(SoundEvents.ENTITY_BREEZE_SHOOT, 1.5F, 1.0F);
                breezeWindChargeEntity.setVelocity(d, e, f, 0.7F, 5 - serverWorld.getDifficulty().getId() * 4);
                if(serverWorld.getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
                    float yawVelocity = (float)Math.atan2(breezeWindChargeEntity.getVelocity().getZ(), breezeWindChargeEntity.getVelocity().getX());
                    BreezeWindChargeEntity breezeWindChargeEntity2 = new BreezeWindChargeEntity(breezeEntity, serverWorld);
                    breezeWindChargeEntity2.setVelocity(breezeWindChargeEntity.getVelocity().rotateY(-yawVelocity).rotateX((float)Math.PI / -9F).rotateY(yawVelocity));
                    serverWorld.spawnEntity(breezeWindChargeEntity2);
                    BreezeWindChargeEntity breezeWindChargeEntity3 = new BreezeWindChargeEntity(breezeEntity, serverWorld);
                    breezeWindChargeEntity3.setVelocity(breezeWindChargeEntity.getVelocity().rotateY(-yawVelocity).rotateX((float)Math.PI / 9F).rotateY(yawVelocity));
                    serverWorld.spawnEntity(breezeWindChargeEntity3);
                }
                serverWorld.spawnEntity(breezeWindChargeEntity);
                breezeEntity.setPose(EntityPose.LONG_JUMPING);
            }
            if(breezeEntity.getPose() == EntityPose.SHOOTING) breezeEntity.lookAtEntity(breezeEntity.getTarget(), 30F, 30F);
        }
        else if(breezeEntity.getPose() == EntityPose.SHOOTING) breezeEntity.setPose(EntityPose.LONG_JUMPING);
    }
}
