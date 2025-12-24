package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VexEntity.class)
public abstract class VexEntityMixin extends HostileEntity {
    @Shadow public abstract void setCharging(boolean charging);
    @Shadow public abstract boolean isCharging();
    @Unique private Vec3d realChargeDir = Vec3d.ZERO;
    @Unique private int chargeTime = 0;
    VexEntityMixin(EntityType<? extends VexEntity> type, World world) {
        super(type, world);
        experiencePoints = 10;
    }
    @Override public boolean damage(DamageSource source, float amount) {
        float damageMultiplier = MobAITweaks.getModConfigValue("vex_projectile_damage_boost", 200) * 0.01F + 1F;
        if(damageMultiplier <= 0F) return false;
        if(source.isIn(DamageTypeTags.IS_PROJECTILE)) if(amount > 0f) amount *= damageMultiplier;
        else amount += damageMultiplier;
        return super.damage(source, amount);
    }
    @Inject(method = "setCharging", at = @At("HEAD"), cancellable = true)
    private void chargingRework(boolean charging, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("vex_rework")) return;
        if(getTarget() != null && charging) {
            if(distanceTo(getTarget()) < MobAITweaks.getModConfigValue("vex_charge_attack_min_distance", 4)) {
                setVelocity(Vec3d.ZERO);
                ci.cancel();
                return;
            }
            realChargeDir = getTarget().getEyePos().subtract(getEyePos());
            chargeTime = (int)(realChargeDir.length() * 1.5);
            realChargeDir = realChargeDir.normalize();
        }
        else {
            realChargeDir = Vec3d.ZERO;
            chargeTime = 0;
            setVelocity(Vec3d.ZERO);
            getMoveControl().moveTo(getX(), getEyeY() + 0.1d, getZ(), 0.1);
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void chargeAttackFix(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("vex_rework")) return;
        if(isCharging() && chargeTime > 0) {
            getNavigation().stop();
            getMoveControl().moveTo(realChargeDir.getX(), realChargeDir.getY(), realChargeDir.getZ(), 1.0);
            setVelocity(realChargeDir);
            if(--chargeTime == 0) setCharging(false);
        }
    }
}
