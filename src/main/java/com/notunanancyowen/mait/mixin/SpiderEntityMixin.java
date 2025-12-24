package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SpiderSpitAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderEntity.class)
public abstract class SpiderEntityMixin extends HostileEntity {
    SpiderEntityMixin(EntityType<? extends SpiderEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        goalSelector.add(2, new SpiderSpitAttackGoal((SpiderEntity)(HostileEntity)this));
        if(!MobAITweaks.getModConfigValue("spiders_attack_smaller_arthropods")) return;
        targetSelector.add(4, new ActiveTargetGoal<>(this, SilverfishEntity.class, false));
        targetSelector.add(4, new ActiveTargetGoal<>(this, EndermiteEntity.class, false));
    }
    @ModifyConstant(method = "initialize", constant = @Constant(floatValue = 0.1F))
    private float modifyEffectSpawnChance(float constant) {
        return MobAITweaks.getModConfigValue("hostile_mobs_spawn_with_effects_chance", 10) * 0.01F;
    }
}
