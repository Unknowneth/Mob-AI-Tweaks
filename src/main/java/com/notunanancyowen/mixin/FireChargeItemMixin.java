package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public abstract class FireChargeItemMixin extends Item {
    public FireChargeItemMixin(Settings settings) {
        super(settings);
    }

    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack currentItem = user.getStackInHand(hand);
        if(!MobAITweaks.getModConfigValue("throwable_fire_charges")) return TypedActionResult.pass(currentItem);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.5f, world.getRandom().nextFloat() * 0.4f + 0.8f);
        if(!world.isClient()) {
            SmallFireballEntity smallFireball = new SmallFireballEntity(world, user, user.getRotationVector().x, user.getRotationVector().y, user.getRotationVector().z);
            smallFireball.setOwner(user);
            smallFireball.setPosition(user.getEyePos());
            smallFireball.setItem(currentItem);
            world.spawnEntity(smallFireball);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if(!user.getAbilities().creativeMode) currentItem.decrement(1);
        return TypedActionResult.success(currentItem, world.isClient());
    }
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void useOnBlockWhileCrouching(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if(MobAITweaks.getModConfigValue("throwable_fire_charges") && context.getPlayer() != null && !context.getPlayer().isSneaking()) cir.setReturnValue(ActionResult.PASS);
    }
}
