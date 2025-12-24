package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends PassiveEntity {
    protected AnimalEntityMixin(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "interactMob", at = @At("TAIL"), cancellable = true)
    private void feedChorusFruit(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(!MobAITweaks.getModConfigValue("animals_can_be_fed_chorus_fruit")) return;
        ItemStack item = player.getStackInHand(hand);
        if(item.getItem().equals(Items.CHORUS_FRUIT)) if(!getWorld().isClient) {
            item.finishUsing(getWorld(), this);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
        else cir.setReturnValue(ActionResult.CONSUME);
    }
}
