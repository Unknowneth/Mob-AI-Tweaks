package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.boss.dragon.phase.SittingAttackingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SittingAttackingPhase.class)
public abstract class EnderDragonSitAttackMixin {
    @Shadow private int ticks;
    @Inject(method = "serverTick", at = @At("HEAD"))
    private void fasterScan(CallbackInfo ci) {
        if (MobAITweaks.getModConfigValue("ender_dragon_rework")) ticks += 3;
    }
}
