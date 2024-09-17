package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.CrossbowAttackTask;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowAttackTask.class)
public abstract class RangedCrossbowAttackTaskMixin {
    @Shadow private int chargingCooldown;
    @Inject(method = "shouldRun(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/MobEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void fixCrossbowCheckForTask(ServerWorld serverWorld, MobEntity mobEntity, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = mobEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(mobEntity.getTarget());
        boolean hasCrossbow = mobEntity.getMainHandStack().getItem() instanceof CrossbowItem || mobEntity.getOffHandStack().getItem() instanceof CrossbowItem;
        cir.setReturnValue(hasCrossbow && LookTargetUtil.isVisibleInMemory(mobEntity, target) && LookTargetUtil.isTargetWithinAttackRange(mobEntity, target, 0));
    }
    @Inject(method = "finishRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/MobEntity;J)V", at = @At("TAIL"))
    private void fixCrossbowUnchargerCheck(ServerWorld serverWorld, MobEntity mobEntity, long l, CallbackInfo ci) {
        if(mobEntity.getMainHandStack().getItem() instanceof CrossbowItem && CrossbowItem.isCharged(mobEntity.getMainHandStack())) {
            if(mobEntity instanceof CrossbowUser ranger) ranger.setCharging(false);
            CrossbowItem.setCharged(mobEntity.getMainHandStack(), false);
        }
        else if(mobEntity.getOffHandStack().getItem() instanceof CrossbowItem && CrossbowItem.isCharged(mobEntity.getOffHandStack())) {
            if(mobEntity instanceof CrossbowUser ranger) ranger.setCharging(false);
            CrossbowItem.setCharged(mobEntity.getOffHandStack(), false);
        }
    }
    @Inject(method = "tickState", at = @At("HEAD"), cancellable = true)
    private void addSomeDelayBetweenShots(MobEntity entity, LivingEntity target, CallbackInfo ci) {
        if(!CrossbowItem.isCharged(entity.getMainHandStack().getItem() instanceof CrossbowItem ? entity.getMainHandStack() : entity.getOffHandStack()) && chargingCooldown > -6) {
            chargingCooldown--;
            ci.cancel();
        }
        if(entity.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && chargingCooldown > 4) chargingCooldown = 4;
    }
    @Inject(method = "tickState", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/ai/brain/task/CrossbowAttackTask;state:Lnet/minecraft/entity/ai/brain/task/CrossbowAttackTask$CrossbowState;", ordinal = 8))
    private void fixCrossbowShootCheck(MobEntity entity, LivingEntity target, CallbackInfo ci) {
        if(entity.isHolding(Items.CROSSBOW)) return;
        Hand whichHandThough = entity.getMainHandStack().getItem() instanceof CrossbowItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
        CrossbowItem.shootAll(entity.getWorld(), entity, whichHandThough, entity.getStackInHand(whichHandThough), 1.0F, (float)(14 - entity.getWorld().getDifficulty().getId() * 4));
        CrossbowItem.setCharged(entity.getStackInHand(whichHandThough), false);
    }
    @Inject(method = "tickState", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/ai/brain/task/CrossbowAttackTask;state:Lnet/minecraft/entity/ai/brain/task/CrossbowAttackTask$CrossbowState;", ordinal = 1))
    private void fixCrossbowHandCheck(MobEntity entity, LivingEntity target, CallbackInfo ci) {
        entity.stopUsingItem();
        entity.setCurrentHand(entity.getMainHandStack().getItem() instanceof CrossbowItem ? Hand.MAIN_HAND : Hand.OFF_HAND);
    }
}
