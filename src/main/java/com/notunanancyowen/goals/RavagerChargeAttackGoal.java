package com.notunanancyowen.goals;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.Difficulty;

import java.util.EnumSet;

public class RavagerChargeAttackGoal extends Goal {
    private final RavagerEntity mob;
    private final int interval;
    private int cooldown = 0;
    private int chargeTime = 0;
    private Vec2f chargeDirection = Vec2f.ZERO;
    public RavagerChargeAttackGoal(RaiderEntity mob, int interval) {
        this.mob = (RavagerEntity)mob;
        this.interval = interval;
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null && mob.getStunTick() <= 0;
    }
    @Override public boolean shouldContinue() {
        return mob.getTarget() != null && mob.getStunTick() <= 0;
    }
    @Override public boolean canStop() {
        return mob.getTarget() == null || mob.getStunTick() > 0;
    }
    @Override public boolean shouldRunEveryTick() {
        return mob.getTarget() != null;
    }
    @Override public void start() {
        cooldown = interval;
        if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) cooldown /= 3;
        chargeTime = 0;
        mob.setSprinting(false);
        getChargeDirection();
    }
    @Override public void stop() {
        cooldown = 0;
        chargeTime = 0;
        mob.setSprinting(false);
        getChargeDirection();
    }
    @Override public void tick() {
        if(mob.getTarget() == null) return;
        if(mob.getAttackTick() > 0) getChargeDirection();
        if(cooldown > 0) {
            cooldown--;
            return;
        }
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        if(chargeTime == 0) ((SpecialAttacksInterface)mob).forceSpecialAttack();
        chargeTime++;
        int timeOffset = mob.getWorld().getDifficulty() == Difficulty.HARD ? 20 : 0;
        mob.getNavigation().stop();
        if(chargeTime < 40 - timeOffset) {
            getChargeDirection();
            mob.lookAtEntity(mob.getTarget(), mob.getMaxLookYawChange(), mob.getMaxLookPitchChange());
            mob.getLookControl().lookAt(mob.getTarget());
            mob.getNavigation().stop();
        }
        else if(chargeTime < 50 - timeOffset / 2) {
            mob.getMoveControl().moveTo(mob.getX() + chargeDirection.x, mob.getY(), mob.getZ() + chargeDirection.y, 2.5d);
            if(mob.horizontalCollision) {
                ((SpecialAttacksInterface)mob).setSpecialCooldown(40);
                chargeTime = 0;
            }
            mob.setSprinting(true);
        }
        else {
            mob.setSprinting(false);
            chargeTime = 0;
            cooldown = interval;
            getControls().clear();
        }
    }
    private void getChargeDirection() {
        if(mob.getTarget() != null) chargeDirection = new Vec2f((float)mob.getTarget().getX(), (float)mob.getTarget().getZ()).add(new Vec2f((float)mob.getX(), (float)mob.getZ()).multiply(-1f)).normalize();
    }
}
