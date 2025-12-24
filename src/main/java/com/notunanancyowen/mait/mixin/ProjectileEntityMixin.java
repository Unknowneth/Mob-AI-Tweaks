package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @ModifyArg(method = "setVelocity(DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;calculateVelocity(DDDFF)Lnet/minecraft/util/math/Vec3d;"), index = 4)
    private float becomeInaccurateWhenNauseous(float uncertainty) {
        ProjectileEntity me = (ProjectileEntity)(Object)this;
        if(me.getOwner() instanceof MobEntity m && m.hasStatusEffect(StatusEffects.NAUSEA)) uncertainty += (float)Math.sqrt(m.getStatusEffect(StatusEffects.NAUSEA).getAmplifier() / 255F) * 122.5F + 10F;
        return uncertainty;
    }
}
