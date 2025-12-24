package com.notunanancyowen.mait.mixin;

import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelPredicateProviderRegistry.class)
public abstract class ModelPredicateProviderRegistryAccessor {
    @SuppressWarnings("deprecation") @Shadow @Final private static Map<Item, Map<Identifier, ModelPredicateProvider>> ITEM_SPECIFIC;
    @Inject(method = "register(Lnet/minecraft/item/Item;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/item/ClampedModelPredicateProvider;)V", at = @At("TAIL"))
    private static void replaceFishingRod(Item item, Identifier id, ClampedModelPredicateProvider provider, CallbackInfo ci) {
        if(id.equals(Identifier.ofVanilla("cast")) && item.equals(Items.FISHING_ROD)) ITEM_SPECIFIC.get(item).replace(id, (stack, world, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            } else {
                boolean bl = entity.getMainHandStack() == stack;
                boolean bl2 = entity.getOffHandStack() == stack;
                if (entity.getMainHandStack().getItem() instanceof FishingRodItem) bl2 = false;
                return (bl || bl2) && ((entity instanceof PlayerEntity p && p.fishHook != null) || (entity instanceof PathAwareEntity m && m.isUsingItem())) ? 1.0F : 0.0F;
            }
        });
    }
}
