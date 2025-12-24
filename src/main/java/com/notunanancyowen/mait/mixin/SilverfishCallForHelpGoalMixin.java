package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(targets = "net.minecraft.entity.mob.SilverfishEntity$CallForHelpGoal")
public abstract class SilverfishCallForHelpGoalMixin extends Goal {
    @Shadow @Final private SilverfishEntity silverfish;
    @Shadow private int delay;
    @Unique private int lastSilverfishCount;
    @Override public void start() {
        lastSilverfishCount = 0;
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void makeThisAMoveGoal(SilverfishEntity silverfish, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("silverfish_stop_to_call_help")) setControls(EnumSet.of(Control.MOVE));
    }
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void doNotStartYet(CallbackInfoReturnable<Boolean> cir) {
        if(silverfish instanceof SpecialAttacksInterface special && special.getSpecialCooldown() != 0) {
            delay = 0;
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextBoolean()Z"))
    private void countSilverfish(CallbackInfo ci) {
        lastSilverfishCount++;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void emitParticlesWhenCallingAllies(CallbackInfo ci) {
        if(lastSilverfishCount > 0 && silverfish.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.INFESTED, silverfish.getX(), silverfish.getY(), silverfish.getZ(), lastSilverfishCount, 0.1, 0.1, 0.1, 0.1);
    }
}
