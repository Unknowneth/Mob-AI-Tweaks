package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

import java.util.EnumSet;

public class RavagerChargeAttackGoal extends Goal {
    private final RavagerEntity mob;
    private final int interval;
    private int cooldown = 0;
    private int chargeTime = 0;
    private Vec2f chargeDirection = Vec2f.ZERO;
    public RavagerChargeAttackGoal(RavagerEntity mob, int interval) {
        this.mob = mob;
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
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
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
            if(mob.getWorld() instanceof ServerWorld server) {
                Vec3d smokePos = mob.getEyePos().add(mob.getRotationVector().multiply(1, 0, 1).normalize().multiply((mob.getBoundingBox().getLengthZ() + mob.getBoundingBox().getLengthX()) * 0.25F));
                Vec3d smokeDir = smokePos.add(mob.getRotationVector().multiply(1, 0, 1).normalize().rotateY((float)Math.PI / 2F).multiply(0.6));
                server.spawnParticles(ParticleTypes.SMOKE, smokeDir.getX(), smokePos.getY(), smokeDir.getZ(), 1, 0, 0, 0, 0);
                smokeDir = smokePos.subtract(mob.getRotationVector().multiply(1, 0, 1).normalize().rotateY((float)Math.PI / 2F).multiply(0.6));
                server.spawnParticles(ParticleTypes.SMOKE, smokeDir.getX(), smokePos.getY(), smokeDir.getZ(), 1, 0, 0, 0, 0);
            }
            if(mob.getAttackTick() > 0) chargeTime = 50 - timeOffset / 2;
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
