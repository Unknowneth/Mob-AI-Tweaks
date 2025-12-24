package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void useWhenMounted(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (MobAITweaks.getModConfigValue("fireworks_boost_mounts") && user.getVehicle() instanceof LivingEntity mount) {
            ItemStack itemStack = user.getStackInHand(hand);
            if (!world.isClient) {
                var d = itemStack.getComponents().get(DataComponentTypes.FIREWORKS);
                if(d != null) mount.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, d.flightDuration() * 20 - 10, 5, false, false));
                FireworkRocketEntity fireworkRocketEntity = new FireworkRocketEntity(world, itemStack, mount);
                world.spawnEntity(fireworkRocketEntity);
                itemStack.decrementUnlessCreative(1, user);
                user.incrementStat(Stats.USED.getOrCreateStat((FireworkRocketItem)(Object)this));
            }
            cir.setReturnValue(TypedActionResult.success(user.getStackInHand(hand), world.isClient()));
        }
    }
}
