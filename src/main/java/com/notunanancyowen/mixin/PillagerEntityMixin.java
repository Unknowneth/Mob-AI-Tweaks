package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.PillagerEatItemToHealGoal;
import com.notunanancyowen.goals.PillagerSwitchItemsGoal;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PillagerEntity.class)
public abstract class PillagerEntityMixin extends IllagerEntity implements CrossbowUser, InventoryOwner {
    @Shadow public abstract SimpleInventory getInventory();
    @Shadow public abstract boolean isCharging();
    @Shadow public abstract void postShoot();
    PillagerEntityMixin(EntityType<? extends PillagerEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof CrossbowAttackGoal)) {
            goalSelector.add(3, new CrossbowAttackGoal<>((PillagerEntity)(IllagerEntity)this,1.0, 15f));
            goalSelector.add(3, new BowAttackGoal<>(this, 0.65, 30,8f));
        }
        goalSelector.add(1, new MeleeAttackGoal(this, 1d, false) {
            @Override public boolean canStart() {
                return !(getMainHandStack().getItem() instanceof RangedWeaponItem) && !(getMainHandStack().getItem() instanceof RangedWeaponItem) && getTarget() != null;
            }
            @Override public boolean shouldContinue() {
                return !(getMainHandStack().getItem() instanceof RangedWeaponItem) && !(getMainHandStack().getItem() instanceof RangedWeaponItem) && getTarget() != null;
            }
            @Override public boolean canStop() {
                return getMainHandStack().getItem() instanceof RangedWeaponItem || getMainHandStack().getItem() instanceof RangedWeaponItem || getTarget() == null;
            }
            @Override public void start() {
                setAttacking(true);
            }
            @Override public void stop() {
                setAttacking(false);
            }
        });
        if(MobAITweaks.getModConfigValue("pillagers_eat_food")) goalSelector.add(9, new PillagerEatItemToHealGoal(this));
        if(MobAITweaks.getModConfigValue("pillagers_can_melee_attack")) goalSelector.add(9, new PillagerSwitchItemsGoal(this));
    }
    @Inject(method = "initEquipment", at = @At("TAIL"))
    private void addMeleeWeaponAndFood(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue ("illagers_use_boats") && ((getWorld().getBiome(getBlockPos()).isIn(BiomeTags.IS_OCEAN) && getVehicle() == null) || getRaid() != null)) getInventory().addStack(Items.DARK_OAK_BOAT.getDefaultStack());
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 2f) tryEquip(new ItemStack(MobAITweaks.getRandomCrossbow(random), 1));
        if(MobAITweaks.getModConfigValue("pillagers_can_melee_attack")) getInventory().addStack(new ItemStack(Items.STONE_AXE, 1));
        if(!MobAITweaks.getModConfigValue("pillagers_eat_food") || localDifficulty.getLocalDifficulty() < 1.5f) return;
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.BREAD, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.APPLE, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.CARROT, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.BAKED_POTATO, random.nextBetween(1, 16)));
        if(localDifficulty.getLocalDifficulty() < 4.5f) return;
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.GOLDEN_APPLE, random.nextBetween(1, 4)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.GOLDEN_CARROT, random.nextBetween(1, 4)));
    }
    @Override public void attack(LivingEntity target, float pullProgress) {
        try {
            if (getMainHandStack() != null && !getMainHandStack().isEmpty() && getMainHandStack().getItem() instanceof BowItem) {
                ArrowEntity arrow = new ArrowEntity(getWorld(), this);
                arrow.setVelocity(target.getEyePos().subtract(getEyePos()).normalize().multiply(pullProgress));
                getWorld().spawnEntity(arrow);
                getMainHandStack().damage(1, this, s -> s.handSwinging = false);
            }
            else {
                shoot(this, 1.6f);
                if (getActiveHand() != null) swingHand(getActiveHand());
            }
        }
        catch (IllegalStateException | NullPointerException e) {
            MobAITweaks.LOGGER.info("Silently caught \"" + e.getMessage() + "\" error at PillagerEntityMixin.shoot(LivingEntity, float), ignore this");
        }
    }
    @Override public State getState() {
        if(isCelebrating()) return State.CELEBRATING;
        else if(getMainHandStack().getItem() instanceof CrossbowItem) return isCharging() ? State.CROSSBOW_CHARGE : State.CROSSBOW_HOLD;
        else if(isAttacking()) if(getMainHandStack().getItem() instanceof BowItem) return State.BOW_AND_ARROW;
        else return State.ATTACKING;
        return isUsingItem() ? State.CROSSBOW_HOLD : State.NEUTRAL;
    }
    @Override public boolean canPickupItem(ItemStack stack) {
        if(stack.isFood() || stack.getItem() instanceof AxeItem || stack.getItem() instanceof FireworkRocketItem) return true;
        return super.canPickupItem(stack);
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof CrossbowItem;
    }
}
