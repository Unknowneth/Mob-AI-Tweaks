package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.VillagerEatItemToHealGoal;
import com.notunanancyowen.goals.VillagerSpecificRoleGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    VillagerEntityMixin(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("villagers_eat_food")) goalSelector.add(2, new VillagerEatItemToHealGoal(this));
        if(MobAITweaks.getModConfigValue("villagers_have_special_roles")) goalSelector.add(1, new VillagerSpecificRoleGoal(this, 32));
    }
    @Override protected void onKilledBy(@Nullable LivingEntity adversary) {
        super.onKilledBy(adversary);
        if(MobAITweaks.getModConfigValue("pillagers_eat_food") && adversary instanceof PillagerEntity pillager) for(int i = 0; i < getInventory().size(); i++) pillager.getInventory().addStack(getInventory().getStack(i));
    }
}
