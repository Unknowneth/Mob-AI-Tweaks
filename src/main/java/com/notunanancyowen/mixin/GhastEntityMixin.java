package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.item.Items;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("all")
@Mixin(GhastEntity.class)
public abstract class GhastEntityMixin extends FlyingEntity implements SpecialAttacksInterface {
    @Unique private final static TrackedData<Integer> SHOOT_TIME = DataTracker.registerData(GhastEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    GhastEntityMixin(EntityType<? extends FlyingEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(CallbackInfo ci) {
        getDataTracker().startTracking(SHOOT_TIME, 0);
    }
    @Override protected void mobTick() {
        if(MobAITweaks.getModConfigValue("ghasts_cry_tears") && getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT) && getRandom().nextBoolean() && getTarget() == null && age % 60 == 0) {
            dropItem(Items.GHAST_TEAR);
            emitGameEvent(GameEvent.ENTITY_PLACE);
            setSpecialCooldown(0);
        }
        if(getTarget() == null) {
            if(age % 60 == 0) for(var fireworks : getWorld().getEntitiesByType(EntityType.FIREWORK_ROCKET, getBoundingBox().expand(32), firework -> distanceTo(firework) < 32)) if(distanceTo(fireworks) < 32 && fireworks.getOwner() instanceof LivingEntity owner && !owner.isInvulnerable() && !owner.isSpectator()) {
                setTarget(owner);
                break;
            }
            if(getAttacker() != null && !getAttacker().isInvulnerable() && !getAttacker().isSpectator()) setTarget(getAttacker());
        }
        else if(getTarget().isInvulnerable() || getTarget().isSpectator()) setTarget(null);
        super.mobTick();
    }
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(SHOOT_TIME, i);
    }
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(SHOOT_TIME);
    }
}
