package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.RaiderUseBoatGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntity.class)
public abstract class IllagerEntityMixin extends RaiderEntity {
    IllagerEntityMixin(EntityType<? extends IllagerEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("illagers_use_boats")) goalSelector.add(14, new RaiderUseBoatGoal(this));
        if(MobAITweaks.getModConfigValue("illagers_and_zombie_villagers_fight")) targetSelector.add(5, new ActiveTargetGoal<>(this, ZombieVillagerEntity.class, true));

    }
}
