package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(targets = "net.minecraft.entity.mob.BlazeEntity$ShootFireballGoal")
public abstract class BlazeShootGoalMixin {
    @Shadow @Final private BlazeEntity blaze;
    @Shadow protected abstract double getFollowRange();
    @Shadow private int fireballsFired;
    @Unique private boolean canStrafe = false;
    @Unique private boolean strafeLeft = false;
    @Unique private Vec3d reposition = Vec3d.ZERO;
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private Entity newFireball(Entity par1) {
        if(!MobAITweaks.getModConfigValue("blaze_attack_rework")) return par1;
        par1.startRiding(blaze, true);
        if(fireballsFired == moreFireballs(4)) par1.setSprinting(true); //use this otherwise unused flag (for projectiles) to mark the final fireball
        return par1;
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/BlazeEntity;getLookControl()Lnet/minecraft/entity/ai/control/LookControl;"))
    private void strafeWhileShooting(CallbackInfo ci) {
        if(blaze.getTarget() != null) if(canStrafe) {
            if(blaze.getWorld() instanceof ServerWorld server) server.spawnParticles(blaze.age % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, blaze.getX(), blaze.getEyeY(), blaze.getZ(), 1, blaze.getWidth() * 0.25d, blaze.getHeight() * 0.25d, blaze.getWidth() * 0.25d, 0.05d);
            reposition = blaze.getPos();
            blaze.lookAtEntity(blaze.getTarget(), 10F, 10F);
            blaze.getMoveControl().strafeTo(!blaze.isOnGround() && blaze.distanceTo(blaze.getTarget()) < 6d ? -0.3F : 0F, strafeLeft ? -0.6F : 0.6F);
        }
        else if(MobAITweaks.getModConfigValue("ranged_mobs_reposition")) if(blaze.getPos().distanceTo(reposition) < 1)  {
            double xOffset = Math.signum(blaze.getTarget().getPos().getX() - blaze.getPos().getX()) * blaze.getRandom().nextBetween((int)getFollowRange() / 2, (int)getFollowRange() - 1);
            double zOffset = Math.signum(blaze.getTarget().getPos().getZ() - blaze.getPos().getZ()) * blaze.getRandom().nextBetween((int)getFollowRange() / 2, (int)getFollowRange() - 1);
            if(blaze.getRandom().nextInt(3) == 1) if(blaze.getRandom().nextBoolean()) xOffset = -xOffset;
            else zOffset = -zOffset;
            reposition = new Vec3d(blaze.getX() + xOffset, blaze.getY(), blaze.getZ() + zOffset);
        }
        else if(!reposition.equals(Vec3d.ZERO)) {
            blaze.getMoveControl().moveTo(reposition.getX(), reposition.getY(), reposition.getZ(), blaze.isOnGround() ? 1.0d : 1.5d);
            blaze.lookAtEntity(blaze.getTarget(), 180F, 10F);
        }
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/control/LookControl;lookAt(Lnet/minecraft/entity/Entity;FF)V"), index = 1)
    private float uncapMaxHeadTurn(float maxYawChange) {
        return maxYawChange * 18F;
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 4))
    private int moreFireballs(int constant) {
        switch(blaze.getWorld().getDifficulty().getId()) {
            case 1 -> constant = MobAITweaks.getModConfigValue("blaze_fireball_count_easy", 2);
            case 2 -> constant = MobAITweaks.getModConfigValue("blaze_fireball_count_normal", 3);
            case 3 -> constant = MobAITweaks.getModConfigValue("blaze_fireball_count_hard", 4);
        }
        if(constant < 0) constant = 0;
        else if(blaze.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) constant *= 2;
        return constant + 1;
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 100))
    private int crazyFastCooldown(int constant) {
        int[] cooldowns = new int[3];
        Arrays.fill(cooldowns, constant);
        constant = MobAITweaks.getRangedAttackCooldown(blaze, cooldowns);
        if(blaze.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) constant /= 5;
        if(constant < 1) constant = 1;
        return constant;
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/BlazeEntity;setFireActive(Z)V"))
    private boolean recordThatSelfIsBurning(boolean fireActive) {
        if(blaze.getTarget() != null && fireActive) {
            blaze.lookAtEntity(blaze.getTarget(), 90F, 10F);
            Vec3d toTarget = blaze.getPos().subtract(blaze.getTarget().getPos());
            double angleFromTarget = Math.atan2(toTarget.getZ(), toTarget.getX());
            toTarget = blaze.getPos().add(blaze.getVelocity()).subtract(blaze.getTarget().getPos());
            double angleWhileMoving = Math.atan2(toTarget.getZ(), toTarget.getX());
            strafeLeft = blaze.isLeftHanded() ? angleWhileMoving > angleFromTarget : angleWhileMoving < angleFromTarget;
        }
        canStrafe = fireActive && MobAITweaks.getModConfigValue("blazes_strafe_when_shooting");
        return fireActive;
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void stopAttacking(CallbackInfo ci) {
        blaze.getPassengerList().forEach(e -> {
            if(e instanceof SmallFireballEntity) {
                if(e.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getEyeY(), e.getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                e.discard();
            }
        });
        canStrafe = false;
        reposition = Vec3d.ZERO;
        fireballsFired = 0;
    }
}
