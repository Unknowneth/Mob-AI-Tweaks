package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin extends Item {
    public FlintAndSteelItemMixin(Settings settings) {
        super(settings);
    }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack currentItem = user.getStackInHand(hand);
        if(currentItem.getEnchantments().getEnchantments().stream().noneMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "after_burner")))) return super.use(world, user, hand);
        var afterBurner = currentItem.getEnchantments().getEnchantments().stream().filter(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "after_burner"))).findFirst();
        int afterBurnerLvl = 0;
        if(afterBurner.isPresent()) afterBurnerLvl = currentItem.getEnchantments().getLevel(afterBurner.get()) - 1;
        world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5f, world.getRandom().nextFloat() * 0.4f + 0.8f);
        if(!world.isClient()) {
            if(afterBurnerLvl > 0) {
                FireballEntity fireball = new FireballEntity(world, user, user.getRotationVector(), afterBurnerLvl);
                fireball.setPosition(user.getEyePos());
                ItemStack item = Items.FIRE_CHARGE.getDefaultStack();
                item.addEnchantment(afterBurner.get(), afterBurnerLvl + 1);
                fireball.setItem(item);
                world.spawnEntity(fireball);
            }
            else {
                SmallFireballEntity smallFireball = new SmallFireballEntity(world, user.getX(), user.getEyeY(), user.getZ(), user.getRotationVector());
                smallFireball.setOwner(user);
                ItemStack item = Items.FIRE_CHARGE.getDefaultStack();
                afterBurner.ifPresent(e -> item.addEnchantment(e, 1));
                smallFireball.setItem(item);
                world.spawnEntity(smallFireball);
            }
            if(!user.getAbilities().creativeMode) currentItem.damage(1, user, LivingEntity.getSlotForHand(hand));
        }
        user.getItemCooldownManager().set(currentItem.getItem(), 10 + afterBurnerLvl * 2);
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(currentItem, world.isClient());
    }
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void removeUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if(context.getStack().getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "after_burner")))) cir.setReturnValue(ActionResult.PASS);
    }
}
