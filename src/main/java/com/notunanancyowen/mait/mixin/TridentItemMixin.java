package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @ModifyConstant(method = "onStoppedUsing", constant = @Constant(intValue = 10, ordinal = 0))
    private int kickBackCooldownReducer(int constant, @Local(index = 1, argsOnly = true) ItemStack stack, @Local(index = 3, argsOnly = true) LivingEntity user) {
        if(user instanceof PlayerEntity player && !player.isOnGround() && player.currentExplosionImpactPos != null && player.currentExplosionImpactPos.getY() < player.getY() && stack.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "kick_back")))) constant = 0;
        return constant;
    }
}
