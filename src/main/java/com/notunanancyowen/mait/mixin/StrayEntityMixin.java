package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StrayEntity.class)
public abstract class StrayEntityMixin extends AbstractSkeletonEntity {
    StrayEntityMixin(EntityType<? extends StrayEntity> type, World world) {
        super(type, world);
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 1.5f) equipStack(EquipmentSlot.MAINHAND, new ItemStack(MobAITweaks.getRandomBow(random), 1));
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("stray_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, MobAITweaks.getModConfigValue("stray_special_attack_cooldown", 120)));
    }
}
