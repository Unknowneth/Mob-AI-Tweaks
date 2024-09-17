package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowAttackGoal.class)
public abstract class RangedCrossbowAttackGoalMixin<T extends HostileEntity & RangedAttackMob & CrossbowUser> {
    @Shadow @Final private T actor;
    @Shadow @Final private double speed;
    @Shadow private int chargedTicksLeft;
    @Shadow protected abstract boolean isUncharged();
    @Unique private ItemStack crossbow() {
        if(actor.getMainHandStack().getItem() instanceof CrossbowItem) return actor.getMainHandStack();
        else if(actor.getOffHandStack().getItem() instanceof CrossbowItem) return actor.getOffHandStack();
        return actor.getMainHandStack();
    }
    @ModifyReturnValue(method = "isEntityHoldingCrossbow", at = @At("RETURN"))
    private boolean isHoldingCrossbowFix(boolean original) {
        return original || actor.getMainHandStack().getItem() instanceof CrossbowItem || actor.getOffHandStack().getItem() instanceof CrossbowItem;
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getHandPossiblyHolding(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)Lnet/minecraft/util/Hand;", shift = At.Shift.AFTER))
    private void fixCrossbowUseBug(CallbackInfo ci) {
        actor.stopUsingItem();
        actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(actor, crossbow().getItem()));
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/RangedAttackMob;attack(Lnet/minecraft/entity/LivingEntity;F)V"))
    private void shootCrossbowCorrectly(CallbackInfo ci) {
        if(!actor.isHolding(Items.CROSSBOW)) CrossbowItem.shootAll(actor.getWorld(), actor, ProjectileUtil.getHandPossiblyHolding(actor, crossbow().getItem()), crossbow(), 1.0F, (float)(14 - actor.getWorld().getDifficulty().getId() * 4));
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void failSafeForCrashes(CallbackInfo ci) {
        if(isUncharged() && !actor.isUsingItem()) {
            if(--chargedTicksLeft < -6) {
                chargedTicksLeft = 0;
                actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(actor, crossbow().getItem()));
            }
            lookAtTarget(ci);
            ci.cancel();
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void lookAtTarget(CallbackInfo ci) {
        if (actor.getTarget() != null) {
            if (actor.getWorld().getDifficulty().getId() > 1) if (actor.getTarget().isBlocking() || !actor.canSee(actor.getTarget()) || (!actor.getActiveItem().isEmpty() && actor.getActiveItem().hasEnchantments() && EnchantmentHelper.getLevel(Enchantments.MULTISHOT, crossbow()) > 0) || actor.getTarget() instanceof PassiveEntity) {
                if (!actor.getNavigation().isFollowingPath() && actor.getTarget().distanceTo(actor) > 3) actor.getMoveControl().moveTo(actor.getTarget().getX(), actor.getTarget().getY(), actor.getTarget().getZ(), speed);
            }
            else if ((actor.getTarget().getVehicle() == null || actor.getTarget().getControllingVehicle() != null) && actor.getTarget().distanceTo(actor) < 6) {
                actor.getNavigation().stop();
                actor.getMoveControl().strafeTo(-0.75f, 0f);
                if (actor.isOnGround() && actor.hurtTime <= 0) actor.setVelocity(actor.getVelocity().getX() * speed, actor.getVelocity().getY(), actor.getVelocity().getZ() * speed);
            }
            actor.lookAtEntity(actor.getTarget(), 60f, 60f);
            actor.getLookControl().lookAt(actor.getTarget(), 60f, 60f);
        }
        if (actor.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && chargedTicksLeft > 4) chargedTicksLeft = 4;
    }
}
