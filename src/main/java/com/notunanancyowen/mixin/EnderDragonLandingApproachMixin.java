package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.LandingApproachPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LandingApproachPhase.class)
public abstract class EnderDragonLandingApproachMixin extends AbstractPhase {
    EnderDragonLandingApproachMixin(EnderDragonEntity dragon) {
        super(dragon);
    }
    @Inject(method = "beginPhase", at = @At("TAIL"))
    private void checkDPS(CallbackInfo ci) {
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) return;
        if(!((EnderDragonAttacksInterface)dragon).isDPS()) dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
        else dragon.getPhaseManager().setPhase(PhaseType.LANDING);
    }
}
