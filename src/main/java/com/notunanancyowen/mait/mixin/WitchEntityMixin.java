package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitchEntity.class)
public abstract class WitchEntityMixin extends RaiderEntity {
    @Shadow public abstract void setDrinking(boolean drinking);
    protected WitchEntityMixin(EntityType<? extends RaiderEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextFloat()F", ordinal = 0))
    private void drinkJumpBoost(CallbackInfo ci, @Local(ordinal = 0) LocalRef<RegistryEntry<Potion>> potion) {
        if(getTarget() != null && getRandom().nextFloat() < 0.05F) if(MobAITweaks.getModConfigValue("witches_drink_jump_boost") && isOnGround() && getTarget().getY() > getY() + 1 && !hasStatusEffect(StatusEffects.JUMP_BOOST)) potion.set(Potions.LEAPING);
        else if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && !hasStatusEffect(StatusEffects.OOZING)) potion.set(Potions.OOZING);
    }
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/WitchEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V", ordinal = 1), cancellable = true)
    private void throwPotionsToHeal(CallbackInfo ci, @Local(ordinal = 0) RegistryEntry<Potion> potion) {
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && !Potions.HEALING.equals(potion)) {
            PotionEntity potionEntity = new PotionEntity(getWorld(), this);
            potionEntity.setItem(PotionContentsComponent.createStack(Items.SPLASH_POTION, potion));
            potionEntity.setPitch(potionEntity.getPitch() + 20.0F);
            potionEntity.setVelocity(-getVelocity().getX(), getVelocity().getY() - 0.2, -getVelocity().getZ(), 0.75F, 0.0F);
            getWorld().spawnEntity(potionEntity);
            ci.cancel();
        }
    }
    @Inject(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;"))
    private void attemptToReachHighTargets(LivingEntity target, float pullProgress, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("witches_drink_jump_boost") && hasStatusEffect(StatusEffects.JUMP_BOOST) && target.getY() > getY() + 1 && isOnGround() && (!MobAITweaks.getModConfigValue("witches_show_potion_before_throwing_it") || !getMainHandStack().isEmpty())) getJumpControl().setActive();
    }
    @Inject(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/WitchEntity;getWorld()Lnet/minecraft/world/World;", ordinal = 0), cancellable = true)
    private void infestPlayers(LivingEntity target, float pullProgress, CallbackInfo ci, @Local(ordinal = 0) LocalRef<RegistryEntry<Potion>> potion) {
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && !target.hasStatusEffect(StatusEffects.INFESTED) && getRandom().nextBoolean()) potion.set(Potions.INFESTED);
        if(!getMainHandStack().isEmpty() || !MobAITweaks.getModConfigValue("witches_show_potion_before_throwing_it")) return;
        setStackInHand(Hand.MAIN_HAND, PotionContentsComponent.createStack(getRandom().nextFloat() < (1F - getHealth() / getMaxHealth()) && getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? Items.LINGERING_POTION : Items.SPLASH_POTION, potion.get()));
        setDrinking(false);
        ci.cancel();
    }
    @ModifyArg(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/PotionContentsComponent;createStack(Lnet/minecraft/item/Item;Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/item/ItemStack;"), index = 0)
    private Item throwLingeringPotions(Item item) {
        if(!MobAITweaks.getModConfigValue("witches_show_potion_before_throwing_it") && getRandom().nextFloat() < (1F - getHealth() / getMaxHealth()) && getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return Items.LINGERING_POTION;
        return item;
    }
    @ModifyExpressionValue(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/PotionContentsComponent;createStack(Lnet/minecraft/item/Item;Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack changePotion(ItemStack original) {
        if(MobAITweaks.getModConfigValue("witches_show_potion_before_throwing_it")) {
            original = getMainHandStack();
            setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        }
        return original;
    }
}
