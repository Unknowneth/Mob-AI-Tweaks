package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntity {
    ChickenEntityMixin(EntityType<? extends ChickenEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addNewBehaviors(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("chickens_flee_from_mobs")) return;
        goalSelector.add(5, new FleeEntityGoal<>(this, PlayerEntity.class, 8f, 0.8d, 1.2d));
        goalSelector.add(6, new FleeEntityGoal<>(this, HostileEntity.class, 6f, 0.8d, 1.2d));
    }
    @Override public void onDamaged(DamageSource damageSource) {
        if(MobAITweaks.getModConfigValue("chickens_shed_feathers") && getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT) && getRandom().nextBoolean() && !getWorld().isClient() && !isBaby()) {
            ItemEntity feathers = dropItem(Items.FEATHER);
            if(feathers != null) {
                if(isOnFire()) feathers.setFireTicks(getFireTicks());
                feathers.setVelocity(getRandom().nextGaussian() - 0.5d, getRandom().nextGaussian(), getRandom().nextGaussian() - 0.5d);
            }
            emitGameEvent(GameEvent.ENTITY_PLACE);
        }
        super.onDamaged(damageSource);
    }
}
