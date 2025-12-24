package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class VillagerEatItemToHealGoal extends Goal {
    private final MerchantEntity mob;
    private static final EntityAttributeModifier EATING_SPEED_PENALTY_MODIFIER = new EntityAttributeModifier(
            Identifier.of(MobAITweaks.MOD_ID, "villager_use_item"), -0.25, EntityAttributeModifier.Operation.ADD_VALUE
    );
    public VillagerEatItemToHealGoal(MerchantEntity mob) {
        this.mob = mob;
    }
    @Override public boolean canStart() {
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) return mob.getHealth() < mob.getMaxHealth() && !mob.isUsingItem();
        return false;
    }
    @Override public boolean canStop() {
        for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) return mob.getHealth() >= mob.getMaxHealth() || !mob.isUsingItem();
        return true;
    }
    @Override public boolean shouldContinue() {
        return mob.getHealth() < mob.getMaxHealth();
    }
    @Override public void stop() {
        mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        mob.stopUsingItem();
        var a = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(a != null) a.removeModifier(EATING_SPEED_PENALTY_MODIFIER);
    }
    @Override public void start() {
        var a = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(a != null) a.addTemporaryModifier(EATING_SPEED_PENALTY_MODIFIER);
    }
    @Override public boolean shouldRunEveryTick() {
        return mob.isUsingItem();
    }
    @Override public void tick() {
        if (mob.getStackInHand(Hand.MAIN_HAND).getItem() != null && mob.getStackInHand(Hand.MAIN_HAND).getComponents().contains(DataComponentTypes.FOOD)) {
            mob.setCurrentHand(Hand.MAIN_HAND);
            if(mob.getItemUseTimeLeft() > 1) return;
            var foodToEat = mob.getStackInHand(Hand.MAIN_HAND).get(DataComponentTypes.FOOD);
            if(foodToEat != null) {
                mob.heal(foodToEat.nutrition());
                if(mob.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.HEART, mob.getX(), mob.getEyeY(), mob.getZ(), foodToEat.nutrition(), 0.1, 0.1, 0.1, 0.1);
            }
            if(mob.getStackInHand(Hand.MAIN_HAND).getCount() == 1 && mob.hasCustomName() && mob.getCustomName() != null && mob.getServer() != null && mob.getServer().getPlayerManager() != null) mob.getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.villager_friend_has_no_food", mob.getCustomName().getString()), false);
            mob.tryEatFood(mob.getWorld(), mob.getStackInHand(Hand.MAIN_HAND));
            mob.stopUsingItem();
        }
        else for (int i = 0; i < mob.getInventory().size(); i++) if(mob.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) {
            mob.setStackInHand(Hand.MAIN_HAND, mob.getInventory().getStack(i));
            break;
        }
    }
}
