package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SkeletonEntity.class)
public abstract class SkeletonEntityMixin extends AbstractSkeletonEntity {
    SkeletonEntityMixin(EntityType<? extends SkeletonEntity> type, World world) {
        super(type, world);
    }
    @Nullable @Override public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData);
        if(MobAITweaks.getModConfigValue("skeleton_horsemen") && spawnReason.equals(SpawnReason.NATURAL) && getVehicle() == null && world.getRandom().nextInt(50) == 25 && difficulty.getLocalDifficulty() > 1.5f) if(isBaby()) {
            ChickenEntity chicken = EntityType.CHICKEN.create(getWorld());
            if(chicken != null) {
                chicken.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                chicken.initialize(world, difficulty, SpawnReason.JOCKEY,null);
                chicken.setHasJockey(true);
                chicken.setBaby(false);
                world.spawnEntity(chicken);
                startRiding(chicken, true);
            }
        }
        else {
            SkeletonHorseEntity horse = EntityType.SKELETON_HORSE.create(getWorld());
            if(horse != null) {
                horse.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                horse.initialize(world, difficulty, SpawnReason.JOCKEY,null);
                horse.setBaby(false);
                horse.setTame(true);
                world.spawnEntity(horse);
                startRiding(horse, true);
            }
        }
        return entityData;
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("skeleton_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, MobAITweaks.getModConfigValue("skeleton_special_attack_cooldown", 200)));
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 2f) equipStack(EquipmentSlot.MAINHAND, MobAITweaks.getRandomBow(random).getDefaultStack());
    }
    @SuppressWarnings("all")
    @Override public void onDeath(DamageSource damageSource) {
        if(MobAITweaks.getModConfigValue("skeletons_can_convert_to_wither") && (hasStatusEffect(StatusEffects.WITHER) || damageSource.getAttacker() instanceof WitherEntity  || damageSource.getAttacker() instanceof WitherSkeletonEntity) && !getWorld().isClient()) convertTo(EntityType.WITHER_SKELETON, true).getAttributes().getCustomInstance(EntityAttributes.GENERIC_SCALE).setBaseValue(1.0d / 1.2d);
        else super.onDeath(damageSource);
    }
}
