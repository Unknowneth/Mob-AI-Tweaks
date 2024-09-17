package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedEntity.class)
public abstract class DrownedEntityMixin extends ZombieEntity implements RangedAttackMob {
    DrownedEntityMixin(EntityType<? extends DrownedEntity> type, World world) {
        super(type, world);
    }
    @Override public Box getBoundingBox(EntityPose pose) {
        if(isSwimming()) {
            double x = (getBoundingBox().minY + getBoundingBox().maxY) * 0.5;
            return new Box(-x, 0.6, -x, x, 1.0, x).expand(getScaleFactor());
        }
        return super.getBoundingBox(pose);
    }
    @Inject(method = "updateSwimming", at = @At("TAIL"))
    private void trySwimming(CallbackInfo ci) {
        if(!isSwimming()) return;
        if(getTarget() != null && distanceTo(getTarget()) < 2 && !getTarget().isSubmergedInWater() && isSubmergedInWater()) addVelocity(getRotationVector().getX() * 0.04, 0.06, getRotationVector().getZ() * 0.04);
    }
    @ModifyReturnValue(method = "isInSwimmingPose", at = @At("TAIL"))
    private boolean swim(boolean original) {
        if(original) setPose(EntityPose.SWIMMING);
        else setPose(EntityPose.STANDING);
        return original;
    }
}
