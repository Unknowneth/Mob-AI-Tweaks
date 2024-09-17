package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.HostileMobRandomlySitDownGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class AbstractZombieEntityMixin extends HostileEntity {
    @Shadow public abstract boolean isBaby();
    AbstractZombieEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("hostile_mobs_can_sit")) goalSelector.add(10, new HostileMobRandomlySitDownGoal(this));
    }
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void disableAIWhenSitting(CallbackInfo ci) {
        if(getVehicle() instanceof AreaEffectCloudEntity aoe && aoe.getCommandTags().contains(MobAITweaks.MOD_ID + ":" + getName().getString() + "'s seat")) ci.cancel();
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void onSpawned(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt, CallbackInfoReturnable<EntityData> cir) {
        if(MobAITweaks.getModConfigValue("zombie_dads_from_bedrock") && !isBaby() && world.getRandom().nextInt(100) == 1 && spawnReason == SpawnReason.NATURAL && difficulty.getLocalDifficulty() > 1.5F) if(getType() == EntityType.ZOMBIE) {
            if(world.getRandom().nextBoolean()) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(getWorld());
                if(zombie == null) return;
                zombie.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
                zombie.setBaby(true);
                zombie.initialize(world, difficulty, spawnReason, null, null);
                world.spawnEntity(zombie);
                zombie.startRiding(this);
            }
            else {
                SkeletonEntity skeleton = EntityType.SKELETON.create(getWorld());
                if(skeleton == null) return;
                skeleton.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
                skeleton.setBaby(true);
                skeleton.initialize(world, difficulty, spawnReason, null, null);
                skeleton.tryEquip(MobAITweaks.getRandomBow(world.getRandom()).getDefaultStack());
                world.spawnEntity(skeleton);
                skeleton.startRiding(this);
            }
        }
        else if(getType() == EntityType.HUSK) {
            HuskEntity zombie = EntityType.HUSK.create(getWorld());
            if(zombie == null) return;
            zombie.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
            zombie.setBaby(true);
            zombie.initialize(world, difficulty, spawnReason, null, null);
            world.spawnEntity(zombie);
            zombie.startRiding(this);
        }
        else if(getType() == EntityType.ZOMBIE_VILLAGER) {
            ZombieVillagerEntity zombie = EntityType.ZOMBIE_VILLAGER.create(getWorld());
            if(zombie == null) return;
            zombie.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
            zombie.setBaby(true);
            zombie.initialize(world, difficulty, spawnReason, null, null);
            zombie.tryEquip(MobAITweaks.getRandomCrossbow(world.getRandom()).getDefaultStack());
            world.spawnEntity(zombie);
            zombie.startRiding(this);
        }
        else if(getType() == EntityType.ZOMBIFIED_PIGLIN) {
            ZombifiedPiglinEntity zombie = EntityType.ZOMBIFIED_PIGLIN.create(getWorld());
            if(zombie == null) return;
            zombie.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0F);
            zombie.setBaby(true);
            zombie.initialize(world, difficulty, spawnReason, null, null);
            zombie.tryEquip(MobAITweaks.getRandomCrossbow(world.getRandom()).getDefaultStack());
            world.spawnEntity(zombie);
            zombie.startRiding(this);
        }
    }
}
