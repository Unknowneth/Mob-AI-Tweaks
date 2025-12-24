package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.GuardianEntity$FireBeamGoal")
public abstract class GuardianBeamAttackGoalMixin {
    @Shadow @Final private GuardianEntity guardian;
    @Shadow @Final private boolean elder;
    @Shadow private int beamTicks;
    @Unique private ArmorStandEntity beamEnd = null;
    @Inject(method = "start", at = @At("HEAD"))
    private void startOverride(CallbackInfo ci) {
        stopOverride(ci);
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void stopOverride(CallbackInfo ci) {
        if(beamEnd != null) {
            beamEnd.discard();
            beamEnd = null;
        }
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/GuardianEntity;setBeamTarget(I)V"))
    private int changeTargetForBeam(int entityId) {
        //genuinely really dumb, but it works and that's more important
        if(!MobAITweaks.getModConfigValue("elder_guardians_are_bosses")) return entityId;
        if(guardian.getTarget() != null && beamEnd == null && elder) {
            beamEnd = EntityType.ARMOR_STAND.create(guardian.getWorld());
            if(beamEnd == null) return entityId;
            if(guardian.getTarget() != null) beamEnd.updatePositionAndAngles(guardian.getTarget().getX() - guardian.getTarget().getVelocity().getX() * 16, guardian.getTarget().getEyeY() - guardian.getTarget().getVelocity().getY() * 16, guardian.getTarget().getZ() - guardian.getTarget().getVelocity().getZ() * 16, guardian.getYaw(), guardian.getPitch());
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("Small", true);
            nbt.putBoolean("Marker", true);
            nbt.putBoolean("Invisible", true);
            nbt.putBoolean("Invulnerable", true);
            beamEnd.readCustomDataFromNbt(nbt);
            guardian.getWorld().spawnEntity(beamEnd);
            return beamEnd.getId();
        }
        if(beamEnd != null) return beamEnd.getId();
        return entityId;
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/Goal;tick()V"))
    private void sweepingRay(CallbackInfo ci) {
        if(beamEnd == null) return;
        if(guardian.getTarget() != null) {
            Vec3d toBeam = beamEnd.getPos().subtract(guardian.getEyePos()).normalize();
            int i;
            for(i = 0; i < Math.min(guardian.getWarmupTime() * 4, 32); i++) if(guardian.getWorld().getBlockState(new BlockPos((int)(toBeam.getX() * i), (int)(toBeam.getY() * i), (int)(toBeam.getZ() * i)).add(guardian.getBlockPos())).isOpaque()) break;
            Vec3d toTarget = guardian.getEyePos().add(toBeam.multiply(i));
            beamEnd.updatePositionAndAngles(toTarget.getX(), toTarget.getY(), toTarget.getZ(), guardian.getYaw(), guardian.getPitch());
            beamEnd.move(MovementType.SELF, guardian.getTarget().getEyePos().subtract(beamEnd.getPos()).normalize());
            if(beamTicks > guardian.getWarmupTime() / 4) for(var c : guardian.getWorld().getOtherEntities(guardian, guardian.getBoundingBox().expand(32), c -> {
                if(c.distanceTo(guardian) < 32) {
                    Vec3d toTarget2 = c.getEyePos().subtract(guardian.getEyePos());
                    double toBeamYaw = Math.atan2(toBeam.getZ(), toBeam.getX());
                    double toTargetYaw = Math.atan2(toTarget2.getZ(), toTarget2.getX());
                    double toBeamPitch = Math.atan2(toBeam.getY(), toBeam.horizontalLength());
                    double toTargetPitch = Math.atan2(toTarget2.getY(), toTarget2.horizontalLength());
                    double distanceAdjust = 1d / Math.max(toTarget2.length(), 1);
                    return Math.abs(toBeamYaw - toTargetYaw) < distanceAdjust && Math.abs(toBeamPitch - toTargetPitch) < distanceAdjust && c.canBeHitByProjectile();
                }
                return false;
            })) c.damage(guardian.getDamageSources().indirectMagic(guardian, guardian), 0.5f);
        }
        if(beamTicks > guardian.getWarmupTime() / 4 && guardian.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.createExplosion(guardian, beamEnd.getX(), beamEnd.getEyeY(), beamEnd.getZ(), 1f, World.ExplosionSourceType.MOB);
            serverWorld.spawnParticles(ParticleTypes.FLASH, beamEnd.getX(), beamEnd.getEyeY(), beamEnd.getZ(), 1, 0, 0, 0, 0);
        }
        guardian.getLookControl().lookAt(beamEnd, 90f, 90f);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), cancellable = true)
    private void beamCheck(CallbackInfo ci) {
        if(beamEnd != null) {
            guardian.setTarget(null);
            ci.cancel();
        }
    }
}
