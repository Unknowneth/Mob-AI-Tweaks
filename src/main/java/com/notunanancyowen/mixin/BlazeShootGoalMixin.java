package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.BlazeEntity$ShootFireballGoal")
public abstract class BlazeShootGoalMixin {
    @Shadow @Final private BlazeEntity blaze;
    @Shadow protected abstract double getFollowRange();
    @Unique private boolean canStrafe = false;
    @Unique private boolean strafeLeft = false;
    @Unique private Vec3d reposition = Vec3d.ZERO;
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/BlazeEntity;getLookControl()Lnet/minecraft/entity/ai/control/LookControl;"))
    private void strafeWhileShooting(CallbackInfo ci) {
        if(blaze.getTarget() != null) if(canStrafe) {
            if(blaze.getWorld() instanceof ServerWorld server) server.spawnParticles(blaze.age % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, blaze.getX(), blaze.getEyeY(), blaze.getZ(), 1, blaze.getWidth() * 0.25d, blaze.getHeight() * 0.25d, blaze.getWidth() * 0.25d, 0.05d);
            reposition = blaze.getPos();
            blaze.lookAtEntity(blaze.getTarget(), 10F, 10F);
            double movementSpeed = blaze.getMovementSpeed() * (blaze.isOnGround() ? 0.16d : 0.08d);
            if(strafeLeft) movementSpeed *= -1d;
            blaze.addVelocity(Math.cos(Math.toRadians(blaze.getYaw())) * movementSpeed, 0d, Math.sin(Math.toRadians(blaze.getYaw())) * movementSpeed);
            blaze.setVelocity(blaze.getVelocity().multiply(0.97d, 1d + (blaze.getVelocity().getY() > 0 ? 0.1d : -0.2d), 0.97d));
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
            blaze.lookAtEntity(blaze.getTarget(), 10F, 10F);
        }
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 100))
    private int crazyFastCooldown(int constant) {
        return blaze.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 20 : constant;
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
        canStrafe = false;
        reposition = Vec3d.ZERO;
    }
}