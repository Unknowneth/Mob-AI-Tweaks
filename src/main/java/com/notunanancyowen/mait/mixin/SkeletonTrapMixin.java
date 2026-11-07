package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.SkeletonHorseTrapTriggerGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.server.ServerTask;
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
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void spawningRework(CallbackInfo ci) {
        ServerWorld serverlevel = (ServerWorld)this.skeletonHorse.getWorld();
        serverlevel.getServer().send(new ServerTask(serverlevel.getServer().getTicks(), () -> {
            if (this.skeletonHorse.isAlive()) {
                addToTeam(skeletonHorse);
                LocalDifficulty difficultyinstance = serverlevel.getLocalDifficulty(this.skeletonHorse.getBlockPos());
                this.skeletonHorse.setTrapped(false);
                this.skeletonHorse.setTame(true);
                this.skeletonHorse.setBreedingAge(0);
                LightningEntity lightningbolt = EntityType.LIGHTNING_BOLT.create(serverlevel);
                if (lightningbolt != null) {
                    lightningbolt.refreshPositionAfterTeleport(this.skeletonHorse.getX(), this.skeletonHorse.getY(), this.skeletonHorse.getZ());
                    lightningbolt.setCosmetic(true);
                    serverlevel.spawnEntity(lightningbolt);
                    AbstractSkeletonEntity skeleton = getSkeletonNew(difficultyinstance, this.skeletonHorse, 3);
                    if (skeleton != null) {
                        skeleton.startRiding(this.skeletonHorse);
                        var attribute = skeleton.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
                        if(attribute != null) attribute.setBaseValue(64d);
                        skeleton.setBaby(false);
                        addToTeam(skeleton);
                        serverlevel.spawnEntityAndPassengers(skeleton);
                        for (int i = 0; i < 3; i++) {
                            AbstractHorseEntity abstracthorse = this.getHorse(difficultyinstance);
                            if (abstracthorse != null) {
                                AbstractSkeletonEntity skeleton1 = getSkeletonNew(difficultyinstance, abstracthorse, i);
                                if (skeleton1 != null) {
                                    skeleton1.startRiding(abstracthorse);
                                    var attribute3 = skeleton1.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
                                    if(attribute3 != null) attribute3.setBaseValue(64d);
                                    skeleton1.setBaby(false);
                                    addToTeam(skeleton1);
                                    abstracthorse.addVelocity(skeletonHorse.getRandom().nextTriangular(0.0, 1.1485), 0.0, skeletonHorse.getRandom().nextTriangular(0.0, 1.1485));
                                    serverlevel.spawnEntityAndPassengers(abstracthorse);
                                    var attribute2 = abstracthorse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                                    if(attribute2 != null) attribute2.setBaseValue(0.5d);
                                }
                                addToTeam(abstracthorse);
                            }
                        }
                    }
                }
            }
        }));
        var attribute = skeletonHorse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(attribute != null) attribute.setBaseValue(0.5d);
        ci.cancel();
    }
    @Unique private AbstractSkeletonEntity getSkeletonNew(LocalDifficulty localDifficulty, AbstractHorseEntity vehicle, int variation) {
        AbstractSkeletonEntity skeletonEntity;
        if (variation == 0) skeletonEntity = EntityType.WITHER_SKELETON.create(vehicle.getWorld());
        else if (variation == 1) skeletonEntity = EntityType.STRAY.create(vehicle.getWorld());
        else if (variation == 2) skeletonEntity = EntityType.BOGGED.create(vehicle.getWorld());
        else skeletonEntity = EntityType.SKELETON.create(vehicle.getWorld());
        if (skeletonEntity != null) {
            addToTeam(skeletonEntity);
            skeletonEntity.initialize((ServerWorld)vehicle.getWorld(), localDifficulty, SpawnReason.TRIGGERED, null);
            skeletonEntity.setBaby(false);
            skeletonEntity.getLootTable();
            skeletonEntity.setPosition(vehicle.getX(), vehicle.getY(), vehicle.getZ());
            skeletonEntity.timeUntilRegen = 60;
            skeletonEntity.setPersistent();
            if (skeletonEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) skeletonEntity.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            enchantEquipmentNew(skeletonEntity, EquipmentSlot.MAINHAND, localDifficulty);
            enchantEquipmentNew(skeletonEntity, EquipmentSlot.HEAD, localDifficulty);
        }
        return skeletonEntity;
    }
    @Unique private void enchantEquipmentNew(AbstractSkeletonEntity rider, EquipmentSlot slot, LocalDifficulty localDifficulty) {
        ItemStack itemStack = rider.getEquippedStack(slot);
        itemStack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        EnchantmentHelper.applyEnchantmentProvider(itemStack, rider.getWorld().getRegistryManager(), EnchantmentProviders.MOB_SPAWN_EQUIPMENT, localDifficulty, rider.getRandom());
        rider.equipStack(slot, itemStack);
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
        if(entity instanceof ScoreHolder scorer) scores.addScoreHolderToTeam(scorer.getNameForScoreboard(), scores.getTeam(teamName));
    }
}
