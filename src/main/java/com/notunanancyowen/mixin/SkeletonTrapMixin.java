package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.SkeletonHorseTrapTriggerGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonHorseTrapTriggerGoal.class)
public abstract class SkeletonTrapMixin {
    @Shadow @Final private SkeletonHorseEntity skeletonHorse;
    @Shadow @Nullable protected abstract AbstractHorseEntity getHorse(LocalDifficulty localDifficulty);
    @Shadow protected abstract ItemStack removeEnchantments(ItemStack stack);
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void doTheThing(CallbackInfo ci) {
        addToTeam(skeletonHorse);
        LocalDifficulty localDifficulty = skeletonHorse.getWorld().getLocalDifficulty(skeletonHorse.getBlockPos());
        if(skeletonHorse.getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) {
            AbstractHorseEntity abstractHorseEntity = getHorse(localDifficulty);
            if (abstractHorseEntity != null) {
                AbstractSkeletonEntity skeletonEntity = getSkeletonNew(localDifficulty, abstractHorseEntity, i);
                if (skeletonEntity != null) {
                    skeletonEntity.startRiding(abstractHorseEntity);
                    var attribute3 = skeletonEntity.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
                    if(attribute3 != null) attribute3.setBaseValue(64d);
                    abstractHorseEntity.addVelocity(skeletonHorse.getRandom().nextTriangular(0.0, 1.1485), 0.0, skeletonHorse.getRandom().nextTriangular(0.0, 1.1485));
                    server.spawnEntityAndPassengers(abstractHorseEntity);
                    var attribute2 = abstractHorseEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                    if(attribute2 != null) attribute2.setBaseValue(0.5d);
                }
                addToTeam(abstractHorseEntity);
            }
        }
        var attribute = skeletonHorse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(attribute != null) attribute.setBaseValue(0.5d);
        ci.cancel();
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V", ordinal = 0))
    private Entity spawnThisGuyAndAddEmToTheTeam(Entity par1) {
        addToTeam(par1);
        if(par1 instanceof AbstractSkeletonEntity skeletonEntity) {
            var attribute = skeletonEntity.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            if(attribute != null) attribute.setBaseValue(64d);
            skeletonEntity.setBaby(false);
        }
        return par1;
    }
    @Unique private AbstractSkeletonEntity getSkeletonNew(LocalDifficulty localDifficulty, AbstractHorseEntity vehicle, int variation) {
        AbstractSkeletonEntity skeletonEntity;
        if (variation == 0) skeletonEntity = EntityType.WITHER_SKELETON.create(vehicle.getWorld());
        else if (variation > 0) skeletonEntity = EntityType.STRAY.create(vehicle.getWorld());
        else skeletonEntity = EntityType.SKELETON.create(vehicle.getWorld());
        if (skeletonEntity != null) {
            addToTeam(skeletonEntity);
            skeletonEntity.initialize((ServerWorld)vehicle.getWorld(), localDifficulty, SpawnReason.TRIGGERED, null, null);
            skeletonEntity.setBaby(false);
            skeletonEntity.setPosition(vehicle.getX(), vehicle.getY(), vehicle.getZ());
            skeletonEntity.timeUntilRegen = 60;
            skeletonEntity.setPersistent();
            if (skeletonEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) skeletonEntity.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            skeletonEntity.equipStack(EquipmentSlot.MAINHAND, EnchantmentHelper.enchant(skeletonEntity.getRandom(), removeEnchantments(skeletonEntity.getMainHandStack()), (int)(5.0F + localDifficulty.getClampedLocalDifficulty() * (float)skeletonEntity.getRandom().nextInt(18)), false));
            skeletonEntity.equipStack(EquipmentSlot.HEAD, EnchantmentHelper.enchant(skeletonEntity.getRandom(), removeEnchantments(skeletonEntity.getEquippedStack(EquipmentSlot.HEAD)), (int)(5.0F + localDifficulty.getClampedLocalDifficulty() * (float)skeletonEntity.getRandom().nextInt(18)), false));
        }
        return skeletonEntity;
    }
    @SuppressWarnings("all")
    @Unique private void addToTeam(Entity entity) {
        if(entity.getServer() == null) return;
        var scores = entity.getServer().getScoreboard();
        String teamName = MobAITweaks.MOD_ID + ":four_horsemen";
        if(scores.getTeam(teamName) == null) {
            scores.addTeam(teamName);
            scores.getTeam(teamName).setFriendlyFireAllowed(false);
        }
        scores.addPlayerToTeam(entity.getUuidAsString(), scores.getTeam(teamName));
    }
}
