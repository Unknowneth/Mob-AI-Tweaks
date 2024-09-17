package com.notunanancyowen.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class VillagerEatItemToHealGoal extends Goal {
    private final MerchantEntity mob;
    public VillagerEatItemToHealGoal(MerchantEntity mob) {
        this.mob = mob;
    }
    @Override public boolean canStart() {
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) return mob.getHealth() < mob.getMaxHealth() && !mob.isUsingItem();
        return false;
    }
    @Override public boolean canStop() {
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) return mob.getHealth() >= mob.getMaxHealth() || !mob.isUsingItem();
        return true;
    }
    @Override public boolean shouldContinue() {
        return mob.getHealth() < mob.getMaxHealth();
    }
    @Override public void stop() {
        mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        mob.stopUsingItem();
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void tick() {
        if (mob.getStackInHand(Hand.MAIN_HAND).getItem() != null && mob.getStackInHand(Hand.MAIN_HAND).isFood()) {
            mob.setCurrentHand(Hand.MAIN_HAND);
            if(mob.getItemUseTimeLeft() > 1) return;
            var foodToEat = mob.getStackInHand(Hand.MAIN_HAND).getItem().getFoodComponent();
            if(foodToEat != null) mob.heal(foodToEat.getHunger());
            if(mob.getStackInHand(Hand.MAIN_HAND).getCount() == 1 && mob.hasCustomName() && mob.getCustomName() != null && mob.getServer() != null && mob.getServer().getPlayerManager() != null) mob.getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.villager_friend_has_no_food", mob.getCustomName().getString()), false);
            mob.eatFood(mob.getWorld(), mob.getStackInHand(Hand.MAIN_HAND));
            mob.stopUsingItem();
        }
        else for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).isFood()) {
            mob.setStackInHand(Hand.MAIN_HAND, mob.getInventory().getStack(i));
            break;
        }
    }
}
