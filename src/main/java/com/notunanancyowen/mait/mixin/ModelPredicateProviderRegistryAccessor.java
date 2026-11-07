package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
    private static void replaceFishingRodAndCrossbow(Item item, Identifier id, ClampedModelPredicateProvider provider, CallbackInfo ci) {
        if(item instanceof CrossbowItem || item instanceof FishingRodItem) if(id.equals(Identifier.ofVanilla("pull")) || id.equals(Identifier.ofVanilla("pulling")) || id.equals(Identifier.ofVanilla("cast"))) {
            var provided = ITEM_SPECIFIC.get(item);
            var orig = provided.get(id);
            boolean crossbow = item instanceof CrossbowItem;
            if(orig != null) if(id.equals(Identifier.ofVanilla("pulling")) && crossbow) provided.replace(id, (stack, world, entity, seed) -> stack.contains(MobAITweaks.AUTO_RELOAD_TIME) && !CrossbowItem.isCharged(stack) ? 1.0F : orig.call(stack, world, entity, seed));
            else if(id.equals(Identifier.ofVanilla("pull")) && crossbow) provided.replace(id, (stack, world, entity, seed) -> stack.contains(MobAITweaks.AUTO_RELOAD_TIME)  ? (float)(stack.get(MobAITweaks.AUTO_RELOAD_TIME) instanceof Integer i ? i : 0) / (float)(entity != null ? CrossbowItem.getPullTime(stack, entity) : MathHelper.floor(1.25F * 20.0F)) : orig.call(stack, world, entity, seed));
            else if(id.equals(Identifier.ofVanilla("cast")) && !crossbow) provided.replace(id, (stack, world, entity, seed) -> entity instanceof PathAwareEntity m && m.isUsingItem() && m.getActiveItem() == stack ? 1.0F : orig.call(stack, world, entity, seed));
        }
    }
}
