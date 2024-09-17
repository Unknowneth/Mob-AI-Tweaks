package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TrackTargetGoal {
    ActiveTargetGoalMixin(MobEntity mob, boolean checkVisibility, boolean checkNavigable) {
        super(mob, checkVisibility, checkNavigable);
    }
    @Shadow @Nullable protected LivingEntity targetEntity;
    @ModifyReturnValue(method = "canStart", at = @At("TAIL"))
    private boolean lineOfSight(boolean original) {
        if(MobAITweaks.getModConfigValue("line_of_sight_rework") && original && targetEntity != null) {
            double followRange = getFollowRange();
            if(targetEntity.isInvisible()) followRange /= 4d;
            else if(targetEntity.isCrawling()) followRange /= 3d;
            else if(targetEntity.isSneaking()) followRange /= 2d;
            if(mob.hasStatusEffect(StatusEffects.BLINDNESS)) followRange /= 3d;
            else if(mob.hasStatusEffect(StatusEffects.NAUSEA)) followRange *= 0.8d;
            original = !checkVisibility || targetEntity.hurtTime > 0 || getFollowRange() == followRange || mob.squaredDistanceTo(targetEntity) < followRange * followRange;
        }
        return original;
    }
}
