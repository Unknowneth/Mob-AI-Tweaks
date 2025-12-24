package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.mait.MobAITweaks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {
    @Shadow @Final protected PathAwareEntity mob;
    @Shadow @Final  private double speed;
    @Shadow private Path path;
    @Shadow private int cooldown;
    @Shadow protected abstract void resetCooldown();
    @ModifyReturnValue(method="shouldContinue()Z", at = @At(value = "RETURN", ordinal = 2))
    public boolean keepAttacking(boolean canContinue) {
        if (!canContinue && mob.distanceTo(mob.getTarget()) < 10) {
            if(FabricLoader.getInstance().isModLoaded("spears") && mob.getMainHandStack().contains(Registries.DATA_COMPONENT_TYPE.get(Identifier.ofVanilla("kinetic_weapon")))) {
                Entity entity = mob.getVehicle();
                Box box3;
                if (entity != null) {
                    Box box = entity.getBoundingBox();
                    Box box2 = mob.getBoundingBox();
                    box3 = new Box(Math.min(box2.minX, box.minX), box2.minY, Math.min(box2.minZ, box.minZ), Math.max(box2.maxX, box.maxX), box2.maxY, Math.max(box2.maxZ, box.maxZ));
                }
                else box3 = mob.getBoundingBox();
                double atk = Math.sqrt(2.04F) - 0.6F;
                box3.expand(atk, 0.0, atk);
                Box box4 = mob.getBoundingBox();
                if(mob.getTarget() != null) {
                    box4 = mob.getTarget().getBoundingBox();
                    if(entity != null) {
                        Vec3d vec3d = entity.getPassengerRidingPos(mob);
                        box4 = box4.withMinY(Math.max(vec3d.y, box4.minY));
                    }
                }
                if(!mob.isUsingItem()) path = mob.getNavigation().findPathTo(mob.getTarget(), 0);
                else if(!mob.getNavigation().isFollowingPath()) mob.getMoveControl().strafeTo(1F, 0F);
                if(box3.intersects(box4) && !mob.isInAttackRange(mob.getTarget())) {
                    Vec3d vec3d = NoPenaltyTargeting.findFrom(mob, 16, 7, mob.getTarget().getPos());
                    if (vec3d != null && mob.getTarget().squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) >= mob.getTarget().squaredDistanceTo(this.mob)) path = mob.getNavigation().findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
                    if(!mob.handSwinging) mob.setCurrentHand(Hand.MAIN_HAND);
                }
                if(mob.handSwinging) mob.stopUsingItem();
            }
            else path = mob.getNavigation().findPathTo(mob.getTarget(), 0);
            mob.getNavigation().startMovingAlong(path, speed);
            return true;
        }
        return canContinue;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void jumpToReachTarget(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("melee_mobs_jump_to_reach") && cooldown == 0 && mob.getWorld().getDifficulty().getId() > 2 && !mob.handSwinging && mob.isOnGround() && mob.getBoundingBox().getLengthX() < 0.8d && mob.getBoundingBox().getLengthZ() < 0.8d && mob.getBoundingBox().getLengthY() > 1d && mob.getTarget() != null && mob.squaredDistanceTo(mob.getTarget().getX(), mob.getY(), mob.getTarget().getZ()) < Math.pow(mob.getBoundingBox().getLengthX() + mob.getBoundingBox().getLengthZ(), 2) && mob.canSee(mob.getTarget())) {
            double yDist = mob.getTarget().getY() - mob.getEyeY();
            double maxDistForJump = 0.9d * mob.getScale() * mob.getScaleFactor() + mob.getJumpBoostVelocityModifier();
            if(yDist > 0.2d && yDist < maxDistForJump) mob.getJumpControl().setActive();
        }
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void stopAttacking(CallbackInfo ci) {
        if(FabricLoader.getInstance().isModLoaded("spears") && mob.getMainHandStack().contains(Registries.DATA_COMPONENT_TYPE.get(Identifier.ofVanilla("kinetic_weapon")))) mob.stopUsingItem();
    }
    @Inject(method = "start", at = @At("TAIL"))
    private void resetAttackCooldownProperly(CallbackInfo ci) {
        resetCooldown();
    }
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.05F))
    private float makePositionRecheckMoreFrequent(float constant) {
        return mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 1F : constant;
    }
    @Inject(method = "resetCooldown", at = @At("TAIL"))
    private void resetAttack(CallbackInfo ci) {
        if(mob.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            var fatigueLevel = mob.getStatusEffect(StatusEffects.MINING_FATIGUE);
            if(fatigueLevel != null) cooldown += (fatigueLevel.getAmplifier() + 1) * 2;
        }
        if(mob.hasStatusEffect(StatusEffects.HASTE)) {
            var hasteLevel = mob.getStatusEffect(StatusEffects.HASTE);
            if(hasteLevel != null) cooldown -= (hasteLevel.getAmplifier() + 1) * 2;
            if(cooldown < 6) cooldown = 6;
        }
        if(mob.isOnGround() && mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) mob.addVelocity(mob.getRotationVector().multiply(speed, 0d, speed).multiply(mob.getMovementSpeed() + 0.2F));
    }
}
