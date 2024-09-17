package com.notunanancyowen.goals;

import com.notunanancyowen.dataholders.WitherAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.EnumSet;

public class WitherChargeAttackGoal extends Goal {
    private final WitherEntity mob;
    private int chargeCounter = 0;
    private int chargeCooldown = 0;
    private int chargeTime = 0;
    private Vec2f dashDirection = new Vec2f(0,0);
    public WitherChargeAttackGoal(HostileEntity mob) {
        this.mob = (WitherEntity) mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    @Override public boolean canStart() {
        return mob.getHealth() < mob.getMaxHealth() * 0.5f && mob.getTarget() != null && mob.getInvulnerableTimer() <= 0;
    }
    @Override public boolean shouldContinue() {
        return ((WitherAttacksInterface)mob).getSlamTime() <= 0 && (mob.getHealth() < mob.getMaxHealth() * 0.5f && mob.getTarget() != null) || chargeTime > 0 || ((WitherAttacksInterface)mob).getChargeTime() > 0;
    }
    @Override public boolean canStop() {
        return (mob.getHealth() > mob.getMaxHealth() * 0.5f || mob.getTarget() == null) && chargeTime <= 0 && ((WitherAttacksInterface)mob).getChargeTime() <= 0;
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void start() {
        mob.setVelocity(0, -1d, 0d);
        mob.getNavigation().stop();
        chargeCooldown = 45;
        chargeCounter = 0;
        chargeTime = 0;
        ((WitherAttacksInterface)mob).setChargeTime(chargeTime);
    }
    @Override public void stop() {
        chargeCooldown = 275;
        chargeCounter = 0;
        chargeTime = 0;
        ((WitherAttacksInterface)mob).setChargeTime(chargeTime);
    }
    @Override public void tick() {
        if(((WitherAttacksInterface)mob).isEnraged()) chargeCooldown--;
        if(chargeCooldown > 0) chargeCooldown--;
        else if(mob.getTarget() != null) {
            mob.getNavigation().stop();
            chargeTime++;
            if(chargeTime < 20) {
                if(chargeTime == 1) {
                    mob.getWorld().playSoundFromEntity(null, mob, SoundEvents.ENTITY_WITHER_AMBIENT, mob.getSoundCategory(), 3f, mob.getSoundPitch());
                    dashDirection = new Vec2f((float)(mob.getTarget().getPos().getX() - mob.getTarget().getVelocity().getX() * 2.5d), (float)(mob.getTarget().getPos().getZ() - mob.getTarget().getVelocity().getZ() * 2.5d)).add(new Vec2f(-(float)mob.getPos().getX(), -(float)mob.getPos().getZ())).normalize();
                }
                mob.setVelocity(-dashDirection.x * 0.75d, (double)Math.signum((float)(mob.getTarget().getPos().getY() - mob.getPos().getY())) * 0.75d, -dashDirection.y * 0.75d);
                mob.prevYaw = mob.prevBodyYaw = mob.bodyYaw = (float)Math.atan2(dashDirection.y, dashDirection.x);
            }
            else if(chargeTime < 25) {
                if(chargeTime == 20) mob.getWorld().playSoundFromEntity(null, mob, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, mob.getSoundCategory(), 3f, mob.getSoundPitch());
                mob.getWorld().createExplosion(mob, mob.getDamageSources().explosion(mob, mob), new ExplosionBehavior(), mob.getPos(), 2f, false, World.ExplosionSourceType.MOB);
                mob.setVelocity(dashDirection.x * 4.5d, 0d, dashDirection.y * 4.5d);
                mob.prevYaw = mob.prevBodyYaw = mob.bodyYaw = (float)Math.atan2(dashDirection.y, dashDirection.x);
                if(mob.distanceTo(mob.getTarget()) <= 4) mob.getTarget().damage(mob.getDamageSources().mobAttack(mob), 15f);
            }
            else {
                mob.setVelocity(0, 0, 0);
                if(mob.getWorld().getDifficulty().getId() < 3 || mob.getHealth() > mob.getMaxHealth() * 0.3333f || ++chargeCounter % 2 == 0) chargeCooldown = 275;
                chargeTime = chargeCooldown > 0 ? 0 : 10;
                dashDirection = new Vec2f((float)(mob.getTarget().getPos().getX() - mob.getTarget().getVelocity().getX() * 2.5d), (float)(mob.getTarget().getPos().getZ() - mob.getTarget().getVelocity().getZ() * 2.5d)).add(new Vec2f(-(float)mob.getPos().getX(), -(float)mob.getPos().getZ())).normalize();
            }
            mob.getLookControl().lookAt(mob.getPos().getX() + dashDirection.x, mob.getPos().getY(), mob.getPos().getZ() + dashDirection.y, 180, 180);
        }
        else chargeCounter = 0;
        ((WitherAttacksInterface)mob).setChargeTime(mob.getTarget() == null ? 0 : chargeTime);
    }
}
