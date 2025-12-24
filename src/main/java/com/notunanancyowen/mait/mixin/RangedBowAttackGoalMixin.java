package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.*;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.Arrays;

@Mixin(BowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin<T extends  HostileEntity & RangedAttackMob> {
	@Shadow @Final private T actor;
	@Shadow private int cooldown;
	@Shadow private int attackInterval;
	@ModifyReturnValue(method = "isHoldingBow", at = @At("RETURN"))
	private boolean isHoldingBowFix(boolean original) {
		return original || actor.getMainHandStack().getItem() instanceof BowItem || actor.getOffHandStack().getItem() instanceof BowItem;
	}
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getHandPossiblyHolding(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)Lnet/minecraft/util/Hand;"))
	private Hand fixBowUseBug(Hand original) {
		return ProjectileUtil.getHandPossiblyHolding(actor, actor.getStackInHand(actor.getMainHandStack().getItem() instanceof BowItem ? Hand.MAIN_HAND : Hand.OFF_HAND).getItem());
	}
	@ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.5F))
	private float moveFaster1(float constant) {
		if (!MobAITweaks.getModConfigValue("skeleton_strafe_speed_buff")) return constant;
		if (actor.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return 1F;
		if (actor.getWorld().getDifficulty().getId() > 1) return 0.75F;
		return constant;
	}
	@ModifyConstant(method = "tick", constant = @Constant(floatValue = -0.5F))
	private float moveFaster2(float constant) {
		if (!MobAITweaks.getModConfigValue("skeleton_strafe_speed_buff")) return constant;
		if (actor.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return -1F;
		if (actor.getWorld().getDifficulty().getId() > 1) return -0.75F;
		return constant;
	}
	@Inject(method = "tick", at = @At("TAIL"))
	private void lookAtTarget(CallbackInfo ci) {
		if (actor.hasVehicle() && actor.getControllingVehicle() instanceof LivingEntity ride) ride.setBodyYaw(actor.getHeadYaw());
		if (actor.getTarget() instanceof LivingEntity target) actor.getLookControl().lookAt(target, 60f, 60f);
		if (MobAITweaks.getModConfigValue("skeletons_swing_off_hand_on_draw") && !actor.getType().isIn(MobAITweaks.SKELETONS_THAT_DONT_SWING_HANDS_ON_PULL) && !actor.isUsingItem() && actor.getActiveHand() != null && cooldown == 1) if(actor.getActiveHand() == Hand.MAIN_HAND) actor.swingHand(Hand.OFF_HAND);
		else actor.swingHand(Hand.MAIN_HAND);
		if (!actor.isAttacking()) cooldown = Math.max(attackInterval, cooldown + 1);
		else if (actor.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && actor.isAttacking() && cooldown > 2) cooldown = 2;
	}
	@Inject(method = "start", at = @At("TAIL"))
	private void startAttacking(CallbackInfo ci) {
		if (!actor.isUsingItem() && actor.getActiveHand() != null) if (actor.getActiveHand() == Hand.MAIN_HAND) actor.swingHand(Hand.OFF_HAND);
		else actor.swingHand(Hand.MAIN_HAND);
		int[] cooldowns = new int[3];
		Arrays.fill(cooldowns, attackInterval);
		attackInterval = MobAITweaks.getRangedAttackCooldown(actor, cooldowns);
		cooldown = 0;
	}
	@Inject(method = "stop", at = @At("TAIL"))
	private void stopAttacking(CallbackInfo ci) {
		cooldown = 0;
		actor.setSprinting(false);
	}
}