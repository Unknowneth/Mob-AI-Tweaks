package com.notunanancyowen.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeEntity.class)
public abstract class BlazeEntityMixin extends HostileEntity {
    BlazeEntityMixin(EntityType<? extends BlazeEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void randomlyFlyAround(CallbackInfo ci) {
        goalSelector.add(6, new FlyGoal(this, 1.0d));
    }
}
/*@Mixin(BlazeEntity.class)
public abstract class BlazeEntityMixin extends HostileEntity {
    @Shadow protected abstract boolean isFireActive();
    @Unique private boolean chargingUp = false;
    @Unique private boolean movingToLeft = false;
    BlazeEntityMixin(EntityType<? extends BlazeEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void flyAroundRandomly(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("blazes_strafe_when_shooting")) return;
        if(getTarget() instanceof LivingEntity target) {
            lookAtEntity(target, 60f, 60f);
            getLookControl().lookAt(target, 60f, 60f);
        }
        if(isFireActive() != chargingUp) {
            if(getRandom().nextBoolean()) movingToLeft = !movingToLeft;
            chargingUp = isFireActive();
        }
        else if(getWorld().getDifficulty() != Difficulty.EASY && isFireActive() && chargingUp) {
            float strafeSpeed = isOnGround() ? 0.75f : 2f;
            if(getTarget() instanceof LivingEntity target) if(isOnGround() || distanceTo(target) > 15) getMoveControl().strafeTo(distanceTo(target) < 8 ? strafeSpeed * -0.75f : 0f, movingToLeft ? -strafeSpeed : strafeSpeed);
        }
    }
}*/
