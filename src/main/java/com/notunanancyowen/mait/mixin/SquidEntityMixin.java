package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SquidEntity.class)
public abstract class SquidEntityMixin {
    @Shadow protected abstract ParticleEffect getInkParticle();
    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SquidEntity;squirt()V"))
    private void blindAttacker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        int duration = MobAITweaks.getModConfigValue("squid_ink_debuff_duration", 100);
        if(duration > 0 && MobAITweaks.getModConfigValue("squid_ink_inflicts_debuffs") && source.isDirect() && source.getAttacker() instanceof LivingEntity l) l.addStatusEffect(new StatusEffectInstance(getInkParticle().equals(ParticleTypes.GLOW_SQUID_INK) ? StatusEffects.GLOWING : StatusEffects.BLINDNESS, duration));
    }
    @Inject(method = "squirt", at = @At("TAIL"))
    private void blindOtherNearbyMobs(CallbackInfo ci) {
        int radius = MobAITweaks.getModConfigValue("squid_ink_debuff_radius", 8);
        int duration = MobAITweaks.getModConfigValue("squid_ink_debuff_duration", 100);
        SquidEntity me = (SquidEntity)(Object)this;
        if(duration > 0) me.getWorld().getEntitiesByClass(LivingEntity.class, me.getBoundingBox().expand(radius, radius, radius), l -> !(l instanceof SquidEntity) && l.distanceTo(me) < radius).forEach(l -> l.addStatusEffect(new StatusEffectInstance(getInkParticle().equals(ParticleTypes.GLOW_SQUID_INK) ? StatusEffects.GLOWING : StatusEffects.BLINDNESS, (int)(duration - (l.distanceTo(me) / radius) * duration))));
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void giveNightVisionWhenStaredAt(CallbackInfo ci) {
        int duration = MobAITweaks.getModConfigValue("glow_squid_night_vision_on_stare_duration", 600);
        SquidEntity me = (SquidEntity)(Object)this;
        if(duration > 0 && getInkParticle().equals(ParticleTypes.GLOW_SQUID_INK) && me.age % 60 == 0) for(var player : me.getWorld().getPlayers()) {
            Vec3d vec3d = player.getRotationVec(1.0F).normalize();
            Vec3d vec3d2 = new Vec3d(me.getX() - player.getX(), me.getEyeY() - player.getEyeY(), me.getZ() - player.getZ());
            double d = vec3d2.length();
            vec3d2 = vec3d2.normalize();
            double e = vec3d.dotProduct(vec3d2);
            if(e > 1.0 - 0.025 / d && player.canSee(me)) player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration));
        }
    }
}
