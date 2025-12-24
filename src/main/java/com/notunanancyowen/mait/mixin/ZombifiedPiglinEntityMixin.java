package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(ZombifiedPiglinEntity.class)
public abstract class ZombifiedPiglinEntityMixin extends ZombieEntity implements CrossbowUser {
    ZombifiedPiglinEntityMixin(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }
    @Unique private boolean hasCrossbow() {
        return (getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof CrossbowItem);
    }
    @Inject(method = "initCustomGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("zombie_pigmen_can_use_crossbows")) return;
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof ZombieAttackGoal)) {
            goalSelector.add(2, new ZombieAttackGoal(this, 1.0d, false) {
                @Override public boolean canStart() {
                    return super.canStart() && !hasCrossbow();
                }
            });
            goalSelector.add(2, new CrossbowAttackGoal<>(this, 1.0d, 8F) {
                private int crossbowCooldownTime = 0;
                private int getCrossbowCooldownTime() {
                    int[] cooldowns = new int[3];
                    Arrays.fill(cooldowns, 40);
                    return MobAITweaks.getRangedAttackCooldown(ZombifiedPiglinEntityMixin.this, cooldowns) / 2;
                }
                @Override public void stop() {
                    setAttacking(false);
                    setTarget(null);
                    stopUsingItem();
                }
                @Override public void tick() {
                    Hand realHand = getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    if(getTarget() != null) {
                        if(CrossbowItem.isCharged(getStackInHand(realHand))) {
                            if(squaredDistanceTo(getTarget().getPos()) > 64F) getNavigation().startMovingTo(getTarget(), 1.0d);
                            else if(++crossbowCooldownTime > getCrossbowCooldownTime() && getStackInHand(realHand).getItem() instanceof CrossbowItem crossbowItem) crossbowItem.shootAll(getWorld(), ZombifiedPiglinEntityMixin.this, realHand, getStackInHand(realHand), 0.9F, (float)(14 - getWorld().getDifficulty().getId() * 4), getTarget());
                            if(!CrossbowItem.isCharged(getStackInHand(realHand))) swingHand(realHand);
                            if(!getNavigation().isFollowingPath()) getMoveControl().strafeTo(-0.5F, 0F);
                        }
                        else if(crossbowCooldownTime > 0) {
                            crossbowCooldownTime--;
                            getMoveControl().strafeTo(0.5F, 0F);
                        }
                        else if(!isUsingItem()) setCurrentHand(realHand);
                        else if(getItemUseTimeLeft() == 0) stopUsingItem();
                        getLookControl().lookAt(getTarget(), 60F, 60F);
                        lookAtEntity(getTarget(), 60F, 60F);
                        if(getControllingVehicle() != null) getControllingVehicle().setYaw(getYaw());
                    }
                }
            });
        }
        if(targetSelector.getGoals().removeIf(target -> target.getGoal() instanceof RevengeGoal)) targetSelector.add(1, new RevengeGoal(this, ZombifiedPiglinEntity.class).setGroupRevenge());
    }
    @Inject(method = "initEquipment", at = @At("TAIL"))
    private void spawnWithCrossbow(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(random.nextInt(100) < MobAITweaks.getModConfigValue("zombie_pigmen_with_crossbows_chance", 20) && localDifficulty.getLocalDifficulty() > MobAITweaks.getModConfigValue("zombie_pigmen_with_crossbows_local_difficulty", 250) * 0.01F) {
            var i = MobAITweaks.getRandomCrossbow(random);
            if(FabricLoader.getInstance().isModLoaded("bows") && (i == Items.AIR || i == Items.CROSSBOW)) i = Registries.ITEM.get(Identifier.of("bows", "golden_crossbow"));
            if(i == null || i == Items.AIR) i = Items.CROSSBOW;
            equipStack(EquipmentSlot.MAINHAND, i.getDefaultStack());
            if(getWorld() instanceof ServerWorld server) enchantMainHandItem(server, random, localDifficulty);
        }
    }
}
