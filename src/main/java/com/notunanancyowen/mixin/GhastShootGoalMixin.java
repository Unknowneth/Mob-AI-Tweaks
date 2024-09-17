package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.GhastEntity$ShootFireballGoal")
public abstract class GhastShootGoalMixin {
    @Shadow public int cooldown;
    @Shadow @Final private GhastEntity ghast;
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/GhastEntity;getBodyY(D)D"))
    private double fixFireballOffset(double original) {
        return ghast.getY() - ghast.getHeight() * 0.25F;
    }
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 4.0))
    private double adjustFireballShootPosition(double constant) {
        return --constant;
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int makeFireballCooldownLonger(int constant) {
        constant *= 2;
        var rotationVecBasically = ghast.getRotationVec(1.0F).multiply(adjustFireballShootPosition(0)).add(ghast.getPos());
        if(cooldown == constant && ghast.getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, rotationVecBasically.getX(), fixFireballOffset(0), rotationVecBasically.getZ(), 3, 0.1d, 0.1d, 0.1d, 0.1d);
        return constant;
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/GhastEntity;setShooting(Z)V"))
    private boolean dontShootJustYet(boolean shooting) {
        return cooldown > 35 || (shooting && cooldown < 20) || cooldown < -35;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void registerAttackTimeToInterface(CallbackInfo ci) {
        if(ghast instanceof SpecialAttacksInterface attacks) {
            if(ghast.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
                if(attacks.getSpecialCooldown() > cooldown) cooldown--;
                else if(attacks.getSpecialCooldown() < cooldown) cooldown++;
                cooldown = Math.min(cooldown, 39);
            }
            attacks.setSpecialCooldown(cooldown);
        }
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void removeAttackTimeFromInterface(CallbackInfo ci) {
        if(ghast instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(0);
    }
}
