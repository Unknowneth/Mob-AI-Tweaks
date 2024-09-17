package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileAttackGoal.class)
public abstract class RangedAttackGoalMixin {
    @Shadow @Final private MobEntity mob;
    @Shadow @Final private double mobSpeed;
    @Shadow @Final private float maxShootRange;
    @Shadow private int updateCountdownTicks;
    @Shadow @Final private int minIntervalTicks;
    @Unique private Vec3d randomPosition = Vec3d.ZERO;
    @Unique private boolean beginRepositioning = false;
    @Unique private int attackInterval = -1;
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/RangedAttackMob;attack(Lnet/minecraft/entity/LivingEntity;F)V"), cancellable = true)
    private void shouldStartPullingBow(CallbackInfo ci) {
        boolean hasBow = false;
        if(mob.getMainHandStack() != null && mob.getMainHandStack().getItem() instanceof BowItem bow) {
            if(!mob.isUsingItem()) {
                mob.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(mob, bow));
                mob.swingHand(Hand.OFF_HAND);
            }
            hasBow = true;
        }
        else if(mob.getOffHandStack() != null && mob.getOffHandStack().getItem() instanceof BowItem bow) {
            if(!mob.isUsingItem()) {
                mob.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(mob, bow));
                mob.swingHand(Hand.MAIN_HAND);
            }
            hasBow = true;
        }
        if(!hasBow) {
            if(mob.getActiveHand() != null) mob.swingHand(mob.getActiveHand());
        }
        else if(mob.isUsingItem()) if(BowItem.getPullProgress(mob.getItemUseTime()) < 1) {
            mob.setAttacking(true);
            if(mob.getTarget() != null) strafeAwayFromCurrentTarget(mob.getTarget());
            updateCountdownTicks++;
            ci.cancel();
        }
        else mob.stopUsingItem();
        attackInterval = -1;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void randomlyReposition(CallbackInfo ci) {
        if(attackInterval < 0) attackInterval = updateCountdownTicks;
        if(!MobAITweaks.getModConfigValue("ranged_mobs_reposition") || mob.getType() == EntityType.WITHER) return;
        if(!beginRepositioning && mob.getRandom().nextInt(minIntervalTicks) == 0) {
            if (mob.getTarget() != null) {
                LivingEntity target = mob.getTarget();
                double xOffset = Math.signum(target.getPos().getX() - mob.getPos().getX()) * mob.getRandom().nextBetween((int)maxShootRange / 2, (int)maxShootRange - 1);
                double zOffset = Math.signum(target.getPos().getZ() - mob.getPos().getZ()) * mob.getRandom().nextBetween((int)maxShootRange / 2, (int)maxShootRange - 1);
                if(mob.getRandom().nextInt(3) == 1) if(mob.getRandom().nextBoolean()) xOffset = -xOffset;
                else zOffset = -zOffset;
                int yOffset = 0;
                randomPosition = new Vec3d(target.getPos().getX() - xOffset, mob.getPos().getY(), target.getPos().getZ() - zOffset);
                for(int i = 1; i < mob.getSafeFallDistance(); i++) if(mob.getWorld().getBlockState(new BlockPos((int)randomPosition.getX(), (int)mob.getY() - i, (int)randomPosition.getZ())).canPathfindThrough(mob.getWorld(), new BlockPos((int)randomPosition.getX(), (int)mob.getY() - i, (int)randomPosition.getZ()), NavigationType.LAND)) yOffset--;
                randomPosition = new Vec3d(randomPosition.getX(), randomPosition.getY() + yOffset, randomPosition.getZ());
            }
            beginRepositioning = true;
        }
        if(beginRepositioning && ((!mob.isUsingItem() && ((mob.getMainHandStack() != null && mob.getMainHandStack().getItem() instanceof BowItem) || (mob.getOffHandStack() != null && mob.getOffHandStack().getItem() instanceof BowItem))) || updateCountdownTicks >= minIntervalTicks / 2) && updateCountdownTicks < attackInterval - 5) {
            Path path = mob.getNavigation().findPathTo(randomPosition.getX(), randomPosition.getY(), randomPosition.getZ(), 0);
            mob.getNavigation().startMovingAlong(path, mobSpeed);
            if((Math.abs(randomPosition.getX() - mob.getPos().getX()) < 1 && Math.abs(randomPosition.getZ() - mob.getPos().getZ()) < 1) || mob.getNavigation().isIdle()) beginRepositioning = false;
            if(!mob.isUsingItem() && !beginRepositioning) updateCountdownTicks = Math.max(1, updateCountdownTicks - attackInterval + 15);
        }
        if(!beginRepositioning && mob.getTarget() != null) strafeAwayFromCurrentTarget(mob.getTarget());
        boolean shouldAttackFaster = mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP);
        if(shouldAttackFaster && updateCountdownTicks > 1) updateCountdownTicks--;
        shouldAttackFaster |= !mob.isUsingItem() && mob.getWorld().getDifficulty().getId() > 2;
        if(shouldAttackFaster && !beginRepositioning && updateCountdownTicks > 1) updateCountdownTicks--;
    }
    @Unique private void strafeAwayFromCurrentTarget(LivingEntity target) {
        mob.lookAtEntity(target, 60f, 60f);
        mob.getLookControl().lookAt(target, 60f, 60f);
        if(target.distanceTo(mob) < maxShootRange / 3f && mob.getWorld().getDifficulty().getId() > 1) mob.getMoveControl().strafeTo(-(float)mobSpeed, 0f);
        else if(target.distanceTo(mob) > maxShootRange * 0.8F) mob.getMoveControl().strafeTo((float)mobSpeed, 0F);
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void stopAttacking(CallbackInfo ci) {
        attackInterval = -1;
    }
}