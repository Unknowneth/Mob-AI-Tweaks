package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.boss.dragon.phase.SittingFlamingPhase;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SittingFlamingPhase.class)
public abstract class EnderDragonFlamingMixin {
    @Shadow private int ticks;
    @Shadow private int timesRun;
    @Shadow private @Nullable AreaEffectCloudEntity dragonBreathEntity;
    @Unique private Vec3d moveBreathTo = Vec3d.ZERO;
    @Inject(method = "serverTick", at = @At("HEAD"))
    private void fasterFlaming(CallbackInfo ci) {
        if (MobAITweaks.getModConfigValue("ender_dragon_rework")) ticks += 4;
    }
    @ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;normalize()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d movementLocation(Vec3d original) {
        moveBreathTo = original;
        return moveBreathTo;
    }
    @Inject(method = "serverTick", at = @At("TAIL"))
    private void movingHarmingPools(CallbackInfo ci) {
        if (MobAITweaks.getModConfigValue("ender_dragon_rework") && dragonBreathEntity != null && !dragonBreathEntity.isWaiting()) {
            if(!dragonBreathEntity.isWaiting()) dragonBreathEntity.setPosition(dragonBreathEntity.getPos().add(moveBreathTo.multiply(0.8d)));
            dragonBreathEntity.setRadius(dragonBreathEntity.getRadius() + 0.2F);
        }
    }
    @Inject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void betterHarmingPools(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ender_dragon_rework") && dragonBreathEntity != null) dragonBreathEntity.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 2));
    }
    @Inject(method = "reset", at = @At("TAIL"))
    private void longerDPS(CallbackInfo ci){
        if (MobAITweaks.getModConfigValue("ender_dragon_rework")) timesRun = -2;
    }
}
