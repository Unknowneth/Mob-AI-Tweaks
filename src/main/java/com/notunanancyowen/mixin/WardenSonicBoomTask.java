package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.WardenAttacksInterface;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.SonicBoomTask;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(SonicBoomTask.class)
public abstract class WardenSonicBoomTask {
    @Unique ServerWorld serverWorld;
    @Unique WardenEntity wardenEntity;
    @Inject(method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/WardenEntity;J)V", at = @At("HEAD"))
    private void getWardenAndServer(ServerWorld serverWorld, WardenEntity wardenEntity, long l, CallbackInfo ci) {
        this.serverWorld = serverWorld;
        this.wardenEntity = wardenEntity;
    }
    @ModifyArg(method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/WardenEntity;J)V", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", ordinal = 1))
    private Consumer<LivingEntity> ringAnyBellsThatWereHit(Consumer<LivingEntity> action) {
        if(!MobAITweaks.getModConfigValue("wardens_get_stunned_by_bells")) return action;
        try {
            return target -> {
                Vec3d vec3d = wardenEntity.getPos().add(0d, 1.6d, 0d);
                Vec3d vec3d2 = target.getEyePos().subtract(vec3d);
                Vec3d vec3d3 = vec3d2.normalize();
                int i = MathHelper.floor(vec3d2.length()) + 7;
                for (int j = 1; j < i; j++) {
                    Vec3d vec3d4 = vec3d.add(vec3d3.multiply(j));
                    for(int x = -1; x <= 1; x++) for(int y = -1; y <= 1; y++) for(int z = -1; z <= 1; z++) if(serverWorld.getBlockEntity(new BlockPos((int) vec3d4.x + x, (int) vec3d4.y + y, (int) vec3d4.z + z)) instanceof BellBlockEntity bell) {
                        bell.activate(bell.lastSideHit == null ? Direction.getFacing(vec3d3.x, 0d, vec3d3.z) : bell.lastSideHit);
                        if(wardenEntity instanceof WardenAttacksInterface attacks) attacks.setBell(bell.getPos());
                        serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, vec3d4.x, vec3d4.y, vec3d4.z, 1, 0.0, 0.0, 0.0, 0.0);
                        Vec3d vec3d5 = bell.getPos().toCenterPos().subtract(wardenEntity.getEyePos());
                        Vec3d vec3d6 = vec3d5.normalize();
                        int k = MathHelper.floor(vec3d5.length());
                        for (int l = 0; l <= k; l++) {
                            Vec3d vec3d7 = vec3d5.add(vec3d6.multiply(l));
                            serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, vec3d7.x, vec3d7.y, vec3d7.z, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                        wardenEntity.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3.0F, 1.0F);
                        wardenEntity.damage(serverWorld.getDamageSources().sonicBoom(target), 20.0F);
                        return;
                    }
                    serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, vec3d4.x, vec3d4.y, vec3d4.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                wardenEntity.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3.0F, 1.0F);
                if (target.damage(serverWorld.getDamageSources().sonicBoom(wardenEntity), 10.0F)) {
                    double d = 0.5 * (1.0 - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                    double e = 2.5 * (1.0 - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                    target.addVelocity(vec3d3.getX() * e, vec3d3.getY() * d, vec3d3.getZ() * e);
                }
            };
        }
        catch (Throwable ignore) {
            return action;
        }
    }
}
