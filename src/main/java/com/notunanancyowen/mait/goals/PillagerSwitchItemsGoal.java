package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class PillagerSwitchItemsGoal extends Goal {
    private final PillagerEntity mob;
    public PillagerSwitchItemsGoal(IllagerEntity mob) {
        this.mob = (PillagerEntity)mob;
    }
    @Override public boolean canStart() {
        return canUseGoal();
    }
    @Override public boolean shouldContinue() {
        return canUseGoal();
    }
    @Override public boolean canStop() {
        return !canUseGoal();
    }
    @Override public boolean shouldRunEveryTick() {
        return canUseGoal();
    }
    @Override public void tick() {
        int meleeRange = MobAITweaks.getModConfigValue("pillager_switch_to_melee_range", 3);
        if(mob.getTarget() instanceof LivingEntity target) if(!holdingRangedWeapon() && mob.distanceTo(target) > meleeRange) {
            addHeldItemToStack();
            for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getItem() instanceof RangedWeaponItem) {
                moveItemFromInventory(i);
                break;
            }
            mob.stopUsingItem();
            if(mob.getMainHandStack().getComponents().contains(DataComponentTypes.TOOL) && mob.getOffHandStack().getItem() instanceof RangedWeaponItem) {
                mob.getInventory().addStack(mob.getMainHandStack());
                mob.setStackInHand(Hand.MAIN_HAND, mob.getOffHandStack());
                mob.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            }
            mob.addVelocity(mob.getRotationVector().multiply(-0.6d).getX(), -0.1d, mob.getRotationVector().multiply(-0.6d).getZ());
        }
        else if(holdingRangedWeapon() && mob.distanceTo(target) < meleeRange) {
            addHeldItemToStack();
            for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getComponents().contains(DataComponentTypes.TOOL)) {
                moveItemFromInventory(i);
                break;
            }
            mob.stopUsingItem();
            mob.setCharging(false);
            mob.addVelocity(mob.getRotationVector().multiply(0.2d).getX(), 0.2d, mob.getRotationVector().multiply(0.2d).getZ());
        }
        else if(target.isBlocking() && target.getItemUseTime() > MobAITweaks.getModConfigValue("pillager_shield_break_cooldown", 60)) mob.getNavigation().startMovingTo(target, mob.isUsingItem() ? 0.5 : 1.0);
    }
    private void addHeldItemToStack() {
        if(mob.getActiveHand() == Hand.OFF_HAND) mob.getInventory().addStack(mob.getOffHandStack());
        else mob.getInventory().addStack(mob.getMainHandStack());
    }
    private void moveItemFromInventory(int i) {
        mob.setStackInHand(mob.getActiveHand(), mob.getInventory().getStack(i));
        mob.getInventory().setStack(i, ItemStack.EMPTY);
    }
    private boolean canUseGoal() {
        boolean hasMelee = !holdingRangedWeapon();
        boolean hasRanged = holdingRangedWeapon();
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getComponents().contains(DataComponentTypes.TOOL)) hasMelee = true;
        else if(mob.getInventory().getStack(i).getItem() instanceof RangedWeaponItem) hasRanged = true;
        else if(hasMelee && hasRanged) break;
        return mob.getTarget() != null && !mob.isUsingItem() && !mob.handSwinging && !mob.isCharging() && hasMelee && hasRanged;
    }
    private boolean holdingRangedWeapon() {
        return (mob.getActiveHand() == Hand.OFF_HAND ? mob.getOffHandStack() : mob.getMainHandStack()).getItem() instanceof RangedWeaponItem;
    }
}
