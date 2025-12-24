package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class HuskBurrowGoal extends Goal {
    private final ZombieEntity mob;
    private Vec3d lastTargetPos = null;
    private int digTime = 40;
    public HuskBurrowGoal(ZombieEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    @Override public boolean canStart() {
        if(mob.isBaby() || (mob.hasStatusEffect(StatusEffects.MINING_FATIGUE) && !mob.hasStatusEffect(StatusEffects.HASTE))) return false;
        if(mob instanceof SpecialAttacksInterface special) {
            int cooldown = special.getSpecialCooldown();
            if(cooldown <= 0) return mob.getTarget() instanceof LivingEntity target && target.distanceTo(mob) > MobAITweaks.getModConfigValue("husk_burrow_min_distance", 12) && target.getSteppingBlockState().isIn(BlockTags.SAND) && mob.getSteppingBlockState().isIn(BlockTags.SAND);
            if(mob.isOnGround() && mob.getTarget() != null && mob.getVehicle() == null) special.setSpecialCooldown(--cooldown);
        }
        return false;
    }
    @Override public boolean shouldContinue() {
        return mob instanceof SpecialAttacksInterface special && special.getSpecialCooldown() <= 0 && digTime < 40;
    }
    @Override public void start() {
        mob.setAttacking(false); //disables aggro animation
        mob.setNoGravity(true);
        mob.setSprinting(true);
        if(mob.getTarget() instanceof LivingEntity target) lastTargetPos = target.getPos();
        if(mob.getFirstPassenger() != null) mob.getFirstPassenger().setInvulnerable(true);
        digTime = 0;
    }
    @Override public void stop() {
        mob.setAttacking(false);
        mob.setNoGravity(false);
        mob.setSprinting(false);
        lastTargetPos = null;
        if(mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(MobAITweaks.getModConfigValue("husk_burrow_cooldown", 200) / (mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 3 : 1));
        if(mob.getFirstPassenger() != null) mob.getFirstPassenger().setInvulnerable(false);
        digTime = 40;
    }
    @Override public void tick() {
        mob.stopMovement();
        mob.setVelocity(Vec3d.ZERO);
        mob.swingHand(mob.preferredHand != null ? mob.preferredHand : Hand.MAIN_HAND);
        if(digTime == 20) mob.setAttacking(mob.getTarget() != null);
        mob.getLookControl().lookAt(mob.getX(), -Integer.MAX_VALUE, mob.getZ(), 30f,  digTime > 20 ? -mob.getMaxLookPitchChange() : 90f);
        float size = mob.getScale() * mob.getScaleFactor();
        for(int i = 0; i < (mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 2 : 1); i++) if(++digTime >= 40) stop();
        else if(digTime < 20) mob.setPos(mob.getX(), mob.getY() - 0.16d * size, mob.getZ());
        else if(digTime > 20) mob.setPos(mob.getX(), mob.getY() + 0.16d * size, mob.getZ());
        else mob.updatePositionAndAngles(lastTargetPos.getX(), lastTargetPos.getY() - 2.9d * size, lastTargetPos.getZ(), mob.getYaw(), 90f);
    }
}
