package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.BoggedEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoggedEntity.class)
public abstract class BoggedEntityMixin extends AbstractSkeletonEntity {
    BoggedEntityMixin(EntityType<? extends BoggedEntity> type, World world) {
        super(type, world);
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("bogged_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, MobAITweaks.getModConfigValue("bogged_special_attack_cooldown", 120)));
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 1f) equipStack(EquipmentSlot.MAINHAND, MobAITweaks.getRandomBow(random).getDefaultStack());
    }
}
