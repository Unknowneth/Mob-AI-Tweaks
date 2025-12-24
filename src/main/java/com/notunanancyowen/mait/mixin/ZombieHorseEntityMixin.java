package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ZombieHorseEntity.class)
public abstract class ZombieHorseEntityMixin extends AbstractHorseEntity {
    ZombieHorseEntityMixin(EntityType<? extends AbstractHorseEntity> type, World world) {
        super(type, world);
    }
    @Unique private static final TrackedData<Boolean> JOCKEY = DataTracker.registerData(ZombieHorseEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(JOCKEY, false);
        super.initDataTracker(builder);
    }
    @Override protected boolean isAffectedByDaylight() {
        return getPassengerList().isEmpty() && MobAITweaks.getModConfigValue("undead_horses_burn_in_day") && !isSaddled() && super.isAffectedByDaylight();
    }
    @Override public void tickMovement() {
        if(isAffectedByDaylight()) setOnFireFor(8.0F);
        if(!isPersistent() && canImmediatelyDespawn(100)) if(++despawnCounter >= 1200) remove(RemovalReason.UNLOADED_TO_CHUNK);
        super.tickMovement();
    }
    @Override public boolean canImmediatelyDespawn(double distanceSquared) {
        return (getDataTracker().get(JOCKEY) && !isSaddled() && getPassengerList().isEmpty() && distanceSquared >= 100) || super.canImmediatelyDespawn(distanceSquared);
    }
    @Nullable @Override public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        if(spawnReason == SpawnReason.JOCKEY) getDataTracker().set(JOCKEY, true);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsNaturalJockey", getDataTracker().get(JOCKEY));
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        getDataTracker().set(JOCKEY, nbt.getBoolean("IsNaturalJockey"));
    }
}
