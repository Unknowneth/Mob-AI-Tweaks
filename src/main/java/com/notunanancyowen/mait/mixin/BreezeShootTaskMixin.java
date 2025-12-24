package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.task.BreezeShootTask;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BreezeShootTask.class)
public abstract class BreezeShootTaskMixin {
    @ModifyArg(method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/BreezeEntity;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private Entity windChargeShotgun(Entity entity) {
        if(!entity.getWorld().isClient() && entity.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && entity instanceof ProjectileEntity projectile) {
            Entity entity1 = entity.getType().create(entity.getWorld());
            if(entity1 instanceof ProjectileEntity projectile1) {
                entity1.setVelocity(entity.getVelocity().rotateY((float) Math.PI / 9F));
                entity1.setPosition(entity.getPos());
                projectile1.setOwner(projectile.getOwner());
                entity.getWorld().spawnEntity(entity1);
            }
            Entity entity2 = entity.getType().create(entity.getWorld());
            if(entity2 instanceof ProjectileEntity projectile2) {
                entity2.setVelocity(entity.getVelocity().rotateY((float) Math.PI / -9F));
                entity2.setPosition(entity.getPos());
                projectile2.setOwner(projectile.getOwner());
                entity.getWorld().spawnEntity(entity2);
            }
        }
        return entity;
    }
}
