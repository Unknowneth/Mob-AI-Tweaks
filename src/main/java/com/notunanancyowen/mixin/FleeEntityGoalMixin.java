package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FleeEntityGoal.class)
public abstract class FleeEntityGoalMixin<T extends LivingEntity> {
    @Shadow @Final protected PathAwareEntity mob;
    @Shadow @Nullable protected T targetEntity;
    @ModifyReturnValue(method = "canStart", at = @At("TAIL"))
    private boolean dontFlee(boolean original) {
        original &= mob.getControllingPassenger() == null;
        if(mob instanceof AnimalEntity && targetEntity instanceof PlayerEntity player) original &= !player.isSneaking() || !MobAITweaks.getModConfigValue("sneak_to_approach_animals");
        return original;
    }
}
