package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {
    @WrapMethod(method = "tick")
    private void crashProtection(Operation<Void> original) {
        try {
            original.call();
        }
        catch (Throwable t) {

            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
    }
}
