package com.notunanancyowen.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class PillagerEatItemToHealGoal extends Goal {
    private final PillagerEntity mob;
    private int offCombatTimer = 0;
    private ItemStack lastHeldItem = ItemStack.EMPTY;
    public PillagerEatItemToHealGoal(IllagerEntity mob) {
        this.mob = (PillagerEntity)mob;
    }
    @Override public boolean canStart() {
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) return mob.getHealth() < mob.getMaxHealth() && !mob.isUsingItem() && mob.getTarget() == null;
        return false;
    }
    @Override public boolean canStop() {
        if(offCombatTimer < 0) return true;
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) return mob.getHealth() >= mob.getMaxHealth() || mob.getTarget() != null || !mob.isUsingItem();
        return true;
    }
    @Override public boolean shouldContinue() {
        return mob.getHealth() < mob.getMaxHealth() && mob.getTarget() == null && offCombatTimer > -1;
    }
    @Override public void start() {
        offCombatTimer = 0;
    }
    @Override public void stop() {
        mob.setStackInHand(mob.getMainHandStack().isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND, lastHeldItem);
        lastHeldItem = ItemStack.EMPTY;
        if(!mob.getMainHandStack().isEmpty()) mob.setCurrentHand(Hand.MAIN_HAND);
        else if(!mob.getOffHandStack().isEmpty()) mob.setCurrentHand(Hand.OFF_HAND);
        mob.stopUsingItem();
        offCombatTimer = 0;
    }
    @Override public boolean shouldRunEveryTick() {
        return mob.getTarget() == null;
    }
    @SuppressWarnings("all")
    @Override public void tick() {
        if(offCombatTimer < 60) {
            offCombatTimer++;
            return;
        }
        Hand whichHand = mob.getMainHandStack().isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (mob.getStackInHand(whichHand).getItem() != null && mob.getStackInHand(whichHand).isFood()) {
            mob.setCurrentHand(whichHand);
            if(mob.getItemUseTimeLeft() == 1) {
                var foodToEat = mob.getStackInHand(whichHand).getItem().getFoodComponent();
                if(foodToEat != null) mob.heal(foodToEat.getHunger());
                mob.eatFood(mob.getWorld(), mob.getActiveItem());
                mob.stopUsingItem();
                offCombatTimer = -1;
            }
            if(!mob.handSwinging) mob.swingHand(whichHand);
        }
        else for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) {
            if(!mob.getStackInHand(whichHand).equals(ItemStack.EMPTY)) lastHeldItem = mob.getStackInHand(whichHand);
            mob.setStackInHand(whichHand, mob.getInventory().getStack(i));
            break;
        }
    }
}
