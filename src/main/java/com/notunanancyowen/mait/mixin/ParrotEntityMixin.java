package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParrotEntity.class)
public abstract class ParrotEntityMixin extends TameableEntity {
    ParrotEntityMixin(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void flyWithPlayer(CallbackInfo ci) {
        goalSelector.add(3, new Goal() {
            @Override public boolean canStart() {
                return getOwner() != null && getOwner().isFallFlying();
            }
            @Override public void tick() {
                if(getOwner() != null) getNavigation().startMovingTo(getOwner(), 1.0);
            }
        });
    }
}
