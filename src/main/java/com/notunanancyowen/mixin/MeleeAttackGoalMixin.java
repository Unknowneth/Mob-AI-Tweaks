package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {
    @Shadow @Final protected PathAwareEntity mob;
    @Shadow @Final  private double speed;
    @Shadow private Path path;
    @ModifyReturnValue(method="shouldContinue()Z", at = @At(value = "RETURN", ordinal = 2))
    public boolean keepAttacking(boolean canContinue) {
        if (!canContinue && mob.distanceTo(mob.getTarget()) < 10) {
            path = mob.getNavigation().findPathTo(mob.getTarget(), 0);
            mob.getNavigation().startMovingAlong(path, speed);
            return true;
        }
        return canContinue;
    }
    @Inject(method = "resetCooldown", at = @At("TAIL"))
    private void dashAttack(CallbackInfo ci) {
        if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) mob.addVelocity(mob.getRotationVector().multiply(speed, 0d, speed));
    }
}
