package com.notunanancyowen.goals;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.WitherAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Vec2f;

import java.util.EnumSet;

public class WitherStrafeShootGoal extends Goal {
    private final WitherEntity mob;
    private int strafingTime = 0;
    public WitherStrafeShootGoal(HostileEntity mob) {
        this.mob = (WitherEntity) mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    @Override public boolean canStart() {
        return ((WitherAttacksInterface)mob).getSlamTime() <= 0 && mob.getHealth() > mob.getMaxHealth() * 0.5f && mob.getTarget() != null && mob.getInvulnerableTimer() <= 0;
    }
    @Override public boolean shouldContinue() {
        return ((WitherAttacksInterface)mob).getSlamTime() <= 0 && mob.getHealth() > mob.getMaxHealth() * 0.5f && mob.getTarget() != null;
    }
    @Override public boolean canStop() {
        return ((WitherAttacksInterface)mob).getSlamTime() > 0 || mob.getHealth() <= mob.getMaxHealth() * 0.5f || mob.getTarget() == null;
    }
    @Override public void stop() {
        mob.setVelocity(0, -1d, 0d);
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void tick() {
        if(mob.getHealth() > mob.getMaxHealth() * 0.5f && mob.getTarget() != null) {
            mob.getNavigation().stop();
            mob.lookAtEntity(mob.getTarget(), 60f, 60f);
            mob.getLookControl().lookAt(mob.getTarget(), 90f, 90f);
            mob.getMoveControl().strafeTo(1f, 0f);
            Vec2f strafeMovement = new Vec2f((float)mob.getTarget().getPos().getX(), (float)mob.getTarget().getPos().getZ()).add(new Vec2f(-(float)mob.getPos().getX(), -(float)mob.getPos().getZ())).normalize().multiply(0.4f);
            mob.prevYaw = mob.prevBodyYaw = mob.bodyYaw = (float)Math.atan2(strafeMovement.y, strafeMovement.x);
            if(mob.getTarget().distanceTo(mob) < 12) strafeMovement = strafeMovement.multiply(-1f);
            else if(strafingTime != 0 && ((mob.getWorld().getDifficulty().getId() > 1 && mob.getHealth() < mob.getMaxHealth() * 0.6666f) || ((WitherAttacksInterface)mob).isEnraged()) || mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
                if(strafingTime > 0) strafingTime--;
                else strafingTime++;
                double rotationAmount = Math.signum(strafingTime) * Math.PI / 2;
                strafeMovement = new Vec2f(strafeMovement.x * (float)Math.cos(rotationAmount) - strafeMovement.y * (float)Math.sin(rotationAmount), strafeMovement.x * (float)Math.sin(rotationAmount) + strafeMovement.y * (float)Math.cos(rotationAmount));
            }
            else if(mob.getRandom().nextBoolean()) strafingTime = mob.getRandom().nextBetween(-100, 100);
            mob.setVelocity(strafeMovement.x, Math.signum(mob.getTarget().getPos().getY() + 6 - mob.getPos().getY()) * 0.4d, strafeMovement.y);
        }
        else strafingTime = 0;
    }
}
