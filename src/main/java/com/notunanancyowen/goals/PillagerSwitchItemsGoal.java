package com.notunanancyowen.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
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
        if(mob.getTarget() != null) if(!holdingRangedWeapon() && mob.distanceTo(mob.getTarget()) > 3) {
            addHeldItemToStack();
            for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getItem() instanceof RangedWeaponItem) {
                moveItemFromInventory(i);
                break;
            }
            mob.stopUsingItem();
            mob.addVelocity(mob.getRotationVector().multiply(-0.6d).getX(), -0.1d, mob.getRotationVector().multiply(-0.6d).getZ());
        }
        else if(holdingRangedWeapon() && mob.distanceTo(mob.getTarget()) < 3) {
            addHeldItemToStack();
            for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getItem() instanceof AxeItem) {
                moveItemFromInventory(i);
                break;
            }
            mob.stopUsingItem();
            mob.setCharging(false);
            mob.addVelocity(mob.getRotationVector().multiply(0.2d).getX(), 0.2d, mob.getRotationVector().multiply(0.2d).getZ());
        }
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
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getItem() instanceof AxeItem) hasMelee = true;
        else if(mob.getInventory().getStack(i).getItem() instanceof RangedWeaponItem) hasRanged = true;
        else if(hasMelee && hasRanged) break;
        return mob.getTarget() != null && !mob.isUsingItem() && !mob.handSwinging && !mob.isCharging() && hasMelee && hasRanged;
    }
    private boolean holdingRangedWeapon() {
        return (mob.getActiveHand() == Hand.OFF_HAND ? mob.getOffHandStack() : mob.getMainHandStack()).getItem() instanceof RangedWeaponItem;
    }
}
