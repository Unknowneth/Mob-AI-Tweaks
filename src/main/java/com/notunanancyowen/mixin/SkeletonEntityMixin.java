package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
    @Nullable
    @Override public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if(MobAITweaks.getModConfigValue("skeleton_horsemen") && spawnReason.equals(SpawnReason.NATURAL) && getVehicle() == null && world.getRandom().nextInt(50) == 25 && difficulty.getLocalDifficulty() > 1.5f) if(isBaby()) {
            ChickenEntity chicken = EntityType.CHICKEN.create(getWorld());
            if(chicken != null) {
                chicken.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
                chicken.initialize(world, difficulty,spawnReason,null, null);
                chicken.setHasJockey(true);
                chicken.setBaby(false);
                world.spawnEntity(chicken);
                startRiding(chicken, true);
            }
        }
        else {
            SkeletonHorseEntity horse = EntityType.SKELETON_HORSE.create(getWorld());
            if(horse != null) {
                horse.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
                horse.initialize(world, difficulty,spawnReason,null, null);
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
        if(MobAITweaks.getModConfigValue("skeleton_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, 200));
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 2f) equipStack(EquipmentSlot.MAINHAND, new ItemStack(MobAITweaks.getRandomBow(random), 1));
    }
    @SuppressWarnings("all")
    @Override public void onDeath(DamageSource damageSource) {
        if ((hasStatusEffect(StatusEffects.WITHER) || damageSource.getAttacker() instanceof WitherEntity  || damageSource.getAttacker() instanceof WitherSkeletonEntity) && !getWorld().isClient()) convertTo(EntityType.WITHER_SKELETON, true);
        else super.onDeath(damageSource);
    }
}
