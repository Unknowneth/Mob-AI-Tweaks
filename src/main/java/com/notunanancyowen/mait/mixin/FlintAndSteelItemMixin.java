package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void removeUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if(MobAITweaks.getModConfigValue("enchantments_from_mod") && context.getStack().getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "after_burner")))) cir.setReturnValue(ActionResult.PASS);
    }
}
