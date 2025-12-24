package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "org.confluence.terraentity.entity.monster.Decayeder")
public abstract class DecayederMixin extends AbstractSkeletonEntity implements SpecialAttacksInterface {
    protected DecayederMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("bogged_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, MobAITweaks.getModConfigValue("bogged_special_attack_cooldown", 120)));
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "DODGE";
    }
}
