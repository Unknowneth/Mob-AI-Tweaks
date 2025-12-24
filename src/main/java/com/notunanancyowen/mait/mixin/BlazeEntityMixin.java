package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(BlazeEntity.class)
public abstract class BlazeEntityMixin extends HostileEntity {
    @Unique private final ArrayList<Integer> fireballCounter = new ArrayList<>();
    BlazeEntityMixin(EntityType<? extends BlazeEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void randomlyFlyAround(CallbackInfo ci) {
        goalSelector.add(6, new FlyGoal(this, 1.0d));
    }
    @Override protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if(passenger instanceof SmallFireballEntity fireball) {
            int difficulty = getWorld().getDifficulty().getId();
            switch(difficulty) {
                case 1 -> difficulty = MobAITweaks.getModConfigValue("blaze_fireball_count_easy", 2);
                case 2 -> difficulty = MobAITweaks.getModConfigValue("blaze_fireball_count_normal", 3);
                case 3 -> difficulty = MobAITweaks.getModConfigValue("blaze_fireball_count_hard", 4);
            }
            difficulty--;
            if(!fireballCounter.contains(passenger.getId()) && fireballCounter.add(passenger.getId())) {
                positionUpdater.accept(passenger, getX(), getEyeY(), getZ());
                return;
            }
            int i = fireballCounter.indexOf(passenger.getId()) % (difficulty + 1);
            Vec3d v = getEyePos().add(getRotationVector(0F, getHeadYaw() + (isLeftHanded() ? -90 : 90)).multiply(Math.cos(Math.PI * (float)i / (float)difficulty)).add(0, Math.sin(Math.PI * (float)i / (float)difficulty), 0).multiply(Math.clamp(passenger.age / 15F, 0, 1)));
            positionUpdater.accept(passenger, v.getX(), v.getY(), v.getZ());
            if(passenger.age < 40) return;
            passenger.stopRiding();
            passenger.refreshPositionAndAngles(v, getYaw(), getPitch());
            if(passenger.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SMALL_FLAME, v.getX(), v.getY(), v.getZ(), difficulty + 1, 0.1, 0.1, 0.1, 0.5);
            if(passenger.isSprinting()) {
                if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLAME, getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), fireballCounter.size(), 0.2, 0.2, 0.2, 0.5);
                fireballCounter.clear();
                passenger.setSprinting(false);
            }
            if(!isSilent()) playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 5F, 2F);
            if(getTarget() != null) {
                Vec3d toTarget = getTarget().getEyePos().subtract(passenger.getPos()).normalize();
                fireball.setVelocity(toTarget.getX(), toTarget.getY(), toTarget.getZ(), 0.1F ,5F - difficulty);
            }
            else fireball.setVelocity(this, getPitch(), getYaw(), 0F, 0.1F, 5F - difficulty);
        }
    }
    @Override public int getMaxHeadRotation() {
        return 180;
    }
    @Override protected void updatePostDeath() {
        getPassengerList().forEach(e -> {
            if(e instanceof SmallFireballEntity s) {
                if(e.getWorld() instanceof ServerWorld server) {
                    dropItem(s.getStack().getItem());
                    server.spawnParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getEyeY(), e.getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                }
                e.discard();
            }
        });
        super.updatePostDeath();
    }
    @Override public void onRemoved() {
        super.onRemoved();
        getPassengerList().forEach(e -> {
            if(e instanceof SmallFireballEntity) e.discard();
        });
    }
}