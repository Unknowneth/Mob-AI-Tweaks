package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.*;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.Arrays;

@Mixin(CrossbowAttackGoal.class)
public abstract class RangedCrossbowAttackGoalMixin<T extends HostileEntity & RangedAttackMob & CrossbowUser> {
    @Shadow @Final private T actor;
    @Shadow @Final private double speed;
    @Shadow private int chargedTicksLeft;
    @Shadow protected abstract boolean isUncharged();
    @Unique private ItemStack crossbow() {
        if(actor.getMainHandStack().getItem() instanceof CrossbowItem) return actor.getMainHandStack();
        else if(actor.getOffHandStack().getItem() instanceof CrossbowItem) return actor.getOffHandStack();
        return actor.getWeaponStack();
    }
    @ModifyReturnValue(method = "isEntityHoldingCrossbow", at = @At("RETURN"))
    private boolean isHoldingCrossbowFix(boolean original) {
        return original || actor.getMainHandStack().getItem() instanceof CrossbowItem || actor.getOffHandStack().getItem() instanceof CrossbowItem;
    }
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getHandPossiblyHolding(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)Lnet/minecraft/util/Hand;"))
    private Hand fixCrossbowUseBug(Hand original) {
        return ProjectileUtil.getHandPossiblyHolding(actor, crossbow().getItem());
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20), require = 0)
    private int changeDefaultRangedAttack(int constant) {
        int[] cooldowns = new int[3];
        Arrays.fill(cooldowns, constant);
        return MobAITweaks.getRangedAttackCooldown(actor, cooldowns);
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
        if (actor.getTarget() instanceof LivingEntity target) {
            if (actor.getWorld().getDifficulty().getId() > 1) if (!actor.isOnGround() || !actor.canSee(target) || (!actor.getActiveItem().isEmpty() && actor.getActiveItem().hasEnchantments() && EnchantmentHelper.hasAnyEnchantmentsWith(actor.getActiveItem(), EnchantmentEffectComponentTypes.PROJECTILE_COUNT)) || target instanceof PassiveEntity) {
                if (!actor.getNavigation().isFollowingPath() && target.distanceTo(actor) > 3) actor.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), actor.isUsingItem() ? speed * 0.5 : speed);
            }
            else if ((target.getVehicle() == null || target.getControllingVehicle() != null) && target.distanceTo(actor) < 6) {
                actor.getNavigation().stop();
                actor.getMoveControl().strafeTo(actor.isUsingItem() ? -0.4f : -0.8f, 0f);
                if (actor.isOnGround()) actor.setVelocity(actor.getVelocity().getX() * speed, actor.getVelocity().getY(), actor.getVelocity().getZ() * speed);
            }
            actor.lookAtEntity(target, 60f, 60f);
            actor.getLookControl().lookAt(target, 60f, 60f);
        }
        if (actor.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && chargedTicksLeft > 4) chargedTicksLeft = 4;
    }
}
