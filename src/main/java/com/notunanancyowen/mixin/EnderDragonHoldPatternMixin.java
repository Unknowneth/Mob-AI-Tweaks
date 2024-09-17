package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.HoldingPatternPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoldingPatternPhase.class)
public abstract class EnderDragonHoldPatternMixin extends AbstractPhase {
    EnderDragonHoldPatternMixin(EnderDragonEntity dragon) {
        super(dragon);
    }
    @Shadow protected abstract void strafePlayer(PlayerEntity player);
    @Unique private PlayerEntity targetedPlayer = null;
    @Inject(method = "tickInRange", at = @At("HEAD"), cancellable = true)
    private void forceLand(CallbackInfo ci) {
        if (MobAITweaks.getModConfigValue("ender_dragon_rework") && ((EnderDragonAttacksInterface)dragon).isDPS()) {
            dragon.getPhaseManager().setPhase(PhaseType.LANDING_APPROACH);
            ci.cancel();
        }
    }
    @ModifyExpressionValue(method = "tickInRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/player/PlayerEntity;"))
    private PlayerEntity getTargetedPlayer(PlayerEntity original) {
        targetedPlayer = original;
        return targetedPlayer;
    }
    @Inject(method = "tickInRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/phase/HoldingPatternPhase;followPath()V"))
    private void strafePlayerIsNotRandom(CallbackInfo ci) {
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) {
            targetedPlayer = null;
            return;
        }
        if(targetedPlayer == null || ((EnderDragonAttacksInterface)dragon).isDPS()) return;
        else if(dragon.getRandom().nextBoolean() && dragon.distanceTo(targetedPlayer) > 32) {
            dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
            if(dragon.getPhaseManager().getCurrent() instanceof ChargingPlayerPhase charge) charge.setPathTarget(targetedPlayer.getPos());
        }
        else strafePlayer(targetedPlayer);
        targetedPlayer = null;
    }
}
