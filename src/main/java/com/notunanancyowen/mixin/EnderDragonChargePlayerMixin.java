package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChargingPlayerPhase.class)
public abstract class EnderDragonChargePlayerMixin extends AbstractPhase {
    @Shadow private @Nullable Vec3d pathTarget;
    @Shadow private int chargingTicks;
    EnderDragonChargePlayerMixin(EnderDragonEntity dragon) {
        super(dragon);
    }
    @Override public void clientTick() {
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) return;
        Vec3d vec3d = dragon.getRotationVectorFromPhase(1.0F).normalize();
        vec3d.rotateY((float) (-Math.PI / 4));
        double d = dragon.head.getX();
        double e = dragon.head.getBodyY(0.5);
        double f = dragon.head.getZ();
        for (int i = 0; i < 8; i++) {
            double g = d + dragon.getRandom().nextGaussian() / 2.0;
            double h = e + dragon.getRandom().nextGaussian() / 2.0;
            double j = f + dragon.getRandom().nextGaussian() / 2.0;
            for (int k = 0; k < 6; k++) dragon.getWorld().addParticle(ParticleTypes.DRAGON_BREATH, g, h, j, -vec3d.x * 0.18F * (double)k, -vec3d.y * 0.6F, -vec3d.z * 0.18F * (double)k);
            vec3d.rotateY((float) (Math.PI / 16));
        }
    }
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void changeBehavior(CallbackInfo ci) {
        if(pathTarget == null) {
            dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
            ci.cancel();
            return;
        }
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) return;
        chargingTicks++;
        Vec3d d = dragon.getVelocity();
        Vec3d e = pathTarget.subtract(dragon.getPos()).normalize().multiply(d.length());
        dragon.setVelocity(d.lerp(e, (float)Math.min(chargingTicks, 30) / 30f));
        boolean closeEnough = dragon.squaredDistanceTo(pathTarget) < 16;
        if(closeEnough || chargingTicks > 120) {
            dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
            if(closeEnough) {
                dragon.getWorld().createExplosion(dragon, dragon.getX(), dragon.getY(), dragon.getZ(), 6f, World.ExplosionSourceType.NONE);
                dragon.getWorld().createExplosion(dragon, pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), 6f, World.ExplosionSourceType.NONE);
                dragon.setVelocity(dragon.getVelocity().x, -dragon.getVelocity().y, dragon.getVelocity().z);
            }
            pathTarget = null;
        }
        if(dragon instanceof EnderDragonAttacksInterface attacks) attacks.forceMove();
        ci.cancel();
    }
}
