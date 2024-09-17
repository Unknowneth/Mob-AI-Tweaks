package com.notunanancyowen.goals;

import com.notunanancyowen.dataholders.WitherAttacksInterface;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.EnumSet;

public class WitherSlamAttackGoal extends Goal {
    private final WitherEntity mob;
    private int slamCooldown = 0;
    private int slamTime = 0;
    private Vec3d slamTo = Vec3d.ZERO;
    public WitherSlamAttackGoal(HostileEntity mob) {
        this.mob = (WitherEntity) mob;
    }
    @Override public boolean canStart() {
        return ((WitherAttacksInterface)mob).getChargeTime() <= 0 && mob.getHealth() < mob.getMaxHealth() * 0.8333f && mob.getTarget() != null && mob.getInvulnerableTimer() <= 0;
    }
    @Override public boolean shouldContinue() {
        return ((WitherAttacksInterface)mob).getChargeTime() <= 0 && (mob.getHealth() < mob.getMaxHealth() * 0.8333f && mob.getTarget() != null) || slamTime > 0 || ((WitherAttacksInterface)mob).getSlamTime() > 0;
    }
    @Override public boolean canStop() {
        return ((WitherAttacksInterface)mob).getChargeTime() > 0 && ((mob.getHealth() > mob.getMaxHealth() * 0.8333f || mob.getTarget() == null) && slamTime <= 0 && ((WitherAttacksInterface)mob).getSlamTime() <= 0);
    }
    @Override public void start() {
        mob.getNavigation().stop();
        slamCooldown = mob.getHealth() < mob.getMaxHealth() * 0.1666f ? 35 : 105;
        slamTime = 0;
        ((WitherAttacksInterface)mob).setSlamTime(slamTime);
    }
    @Override public void stop() {
        slamCooldown = 165;
        slamTime = 0;
        ((WitherAttacksInterface)mob).setSlamTime(slamTime);
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void tick() {
        if(((WitherAttacksInterface)mob).isEnraged()) slamCooldown--;
        if(slamCooldown > 0) slamCooldown--;
        else if(mob.getTarget() != null) {
            mob.getNavigation().stop();
            slamTime++;
            if(slamTime < 20) {
                if(slamTime == 1) {
                    mob.getWorld().playSoundFromEntity(null, mob, SoundEvents.ENTITY_TNT_PRIMED, mob.getSoundCategory(), 3f, mob.getSoundPitch());
                    setControls(EnumSet.of(Control.MOVE, Control.LOOK));
                }
                mob.setVelocity(-mob.getVelocity().getX(), Math.min(mob.getVelocity().getY(), 0) + (mob.getHealth() < mob.getMaxHealth() * 0.5f ? 0.8d : 0.5d), -mob.getVelocity().getZ());
                mob.getLookControl().lookAt(mob.getTarget());
            }
            else if(!mob.isOnGround() && slamTime < 50) {
                if(slamTime == 20) slamTo = (mob.getTarget().getPos().subtract(mob.getTarget().getVelocity().multiply(4d))).subtract(mob.getPos()).normalize().multiply(2.5d);
                mob.setVelocity(slamTo);
                mob.prevYaw = mob.prevBodyYaw = mob.bodyYaw = (float)Math.atan2(slamTo.getZ(), slamTo.getX());
            }
            else if(slamTime > 100) {
                slamTime = 0;
                slamCooldown = 165;
                getControls().clear();
            }
            else if(slamTime > 50) mob.setVelocity(0d, -0.2d, 0d);
            else {
                mob.getWorld().createExplosion(mob, mob.getDamageSources().explosion(mob, mob), new ExplosionBehavior(), mob.getPos(), 3f, false, World.ExplosionSourceType.MOB);
                if(mob.getWorld().getDifficulty().getId() > 1 || mob.getHealth() < mob.getMaxHealth() * 0.3333f) {
                    AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getPos().getX(), mob.getPos().getY(), mob.getPos().getZ());
                    aoe.setOwner(mob);
                    aoe.setDuration(60);
                    aoe.setParticleType(ParticleTypes.SMOKE);
                    aoe.setRadius(0.5f);
                    aoe.setPosition(mob.getPos());
                    aoe.setRadiusGrowth(0.1f);
                    aoe.setRadiusOnUse(0.1f);
                    aoe.setPotion(Potions.HARMING);
                    mob.getWorld().spawnEntity(aoe);
                }
                if(mob.getHealth() < mob.getMaxHealth() * 0.1666f) {
                    slamTime = 0;
                    slamCooldown = 155;
                    getControls().clear();
                }
                else slamTime = 50;
            }
        }
        ((WitherAttacksInterface)mob).setSlamTime(mob.getTarget() == null ? 0 : slamTime);
    }
}
