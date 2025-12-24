package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.PillagerEatItemToHealGoal;
import com.notunanancyowen.mait.goals.PillagerSwitchItemsGoal;
import com.notunanancyowen.mait.goals.RangedGunAttackGoal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PillagerEntity.class, priority = 1001)
public abstract class PillagerEntityMixin extends IllagerEntity implements CrossbowUser, InventoryOwner {
    @Shadow public abstract SimpleInventory getInventory();
    @Shadow public abstract boolean isCharging();
    @Shadow public abstract void postShoot();
    PillagerEntityMixin(EntityType<? extends PillagerEntity> type, World world) {
        super(type, world);
    }
    @ModifyConstant(method = "createPillagerAttributes", constant = @Constant(doubleValue = 5.0), require = 0)
    private static double nerfMeleeDamage(double constant) {
        return 1.0;
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof CrossbowAttackGoal)) {
            goalSelector.add(3, new CrossbowAttackGoal<>((PillagerEntity)(IllagerEntity)this,1.0, (float)MobAITweaks.getModConfigValue("pillager_crossbow_range", 15)));
            goalSelector.add(3, new BowAttackGoal<>(this, 0.65, 30,8f));
            if(MobAITweaks.getModConfigValue("ranged_mobs_use_guns")) goalSelector.add(3, new RangedGunAttackGoal(this, 12, 1.0, true));
        }
        goalSelector.add(1, new MeleeAttackGoal(this, MobAITweaks.getModConfigValue("pillager_melee_mode_speed_boost", 0) * 0.01 + 1.0, false) {
            private boolean isHoldingRangedWeapon() {
                return getMainHandStack().getItem() instanceof RangedWeaponItem || getOffHandStack().getItem() instanceof RangedWeaponItem || getMainHandStack().getItem().getClass().getSimpleName().contains("Gun") || getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun") || getOffHandStack().getItem().getClass().getSimpleName().contains("Gun") || getOffHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun");
            }
            @Override public boolean canStart() {
                return !isHoldingRangedWeapon() && getTarget() != null;
            }
            @Override public boolean shouldContinue() {
                return !isHoldingRangedWeapon() && getTarget() != null;
            }
            @Override public boolean canStop() {
                return isHoldingRangedWeapon() || getTarget() == null;
            }
            @Override public void start() {
                setAttacking(true);
            }
            @Override public void stop() {
                setAttacking(false);
            }
        });
        if(FabricLoader.getInstance().isModLoaded("musketmod")) try {
            //for the love of God, if you're some other coder reading this, NEVER DO THIS!!!!
            goalSelector.getGoals().stream().filter(g -> g.getGoal().getClass().getSuperclass().getSimpleName().equals("RangedGunAttackGoal")).findFirst().ifPresent(g -> goalSelector.add(0, new Goal() {
                private final Goal rangedGunAttackGoal = g;
                private int shootTime = 0;
                @Override public boolean canStart() {
                    return rangedGunAttackGoal.canStart();
                }
                @Override public boolean canStop() {
                    return rangedGunAttackGoal.canStop();
                }
                @Override public boolean shouldContinue() {
                    return rangedGunAttackGoal.shouldContinue();
                }
                @Override public boolean shouldRunEveryTick() {
                    return rangedGunAttackGoal.shouldRunEveryTick();
                }
                @Override public void tick() {
                    if(!handSwinging) if(shootTime > 0) shootTime--;
                    else rangedGunAttackGoal.tick();
                    if(!isUsingItem() && !handSwinging && getActiveHand() != null && !getWorld().getOtherEntities(PillagerEntityMixin.this, getBoundingBox(), e -> e.getClass().getSimpleName().contains("Bullet")).isEmpty()) swingHand(getActiveHand());
                    if(handSwinging) shootTime = 5;
                    if(getTarget() == null) return;
                    getLookControl().lookAt(getTarget(), 60F, 60F);
                    lookAtEntity(getTarget(), 60F, 60F);
                    if(distanceTo(getTarget()) < 7 && isUsingItem()) getMoveControl().strafeTo(-0.5F, 0F);
                }
                @Override public void start() {
                    if(getControls().isEmpty()) setControls(rangedGunAttackGoal.getControls());
                    rangedGunAttackGoal.start();
                }
                @Override public void stop() {
                    rangedGunAttackGoal.stop();
                }
            }));
            goalSelector.getGoals().removeIf(g -> g.getGoal().getClass().getSuperclass().getSimpleName().equals("RangedGunAttackGoal"));
        }
        catch (Throwable e) {
            goalSelector.add(0, new Goal() {
                @Override public boolean canStart() {
                    return getTarget() != null && (getMainHandStack().getItem().getClass().getSimpleName().contains("GunItem") || getOffHandStack().getItem().getClass().getSimpleName().contains("GunItem") || getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("GunItem") || getOffHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("GunItem"));
                }
                @Override public void tick() {
                    if(!isUsingItem() && !handSwinging && getActiveHand() != null && !getWorld().getOtherEntities(PillagerEntityMixin.this, getBoundingBox(), e -> e.getClass().getSimpleName().contains("Bullet")).isEmpty()) swingHand(getActiveHand());
                    if(getTarget() == null) return;
                    getLookControl().lookAt(getTarget(), 60F, 60F);
                    lookAtEntity(getTarget(), 60F, 60F);
                    if(distanceTo(getTarget()) < 6) getMoveControl().strafeTo(-1F, 0F);
                }
            });
        }
        if(MobAITweaks.getModConfigValue("pillagers_eat_food")) goalSelector.add(9, new PillagerEatItemToHealGoal(this));
        if(MobAITweaks.getModConfigValue("pillager_switch_to_melee_range", 3) > 0) goalSelector.add(9, new PillagerSwitchItemsGoal(this));
    }
    @Inject(method = "initEquipment", at = @At("TAIL"))
    private void addMeleeWeaponAndFood(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("illagers_use_boats") && ((getWorld().getBiome(getBlockPos()).isIn(BiomeTags.IS_OCEAN) && getVehicle() == null) || getRaid() != null)) getInventory().addStack(Items.DARK_OAK_BOAT.getDefaultStack());
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 2f) tryEquip(new ItemStack(MobAITweaks.getRandomCrossbow(random), 1));
        if(MobAITweaks.getModConfigValue("pillager_switch_to_melee_range", 3) > 0) {
            ItemStack stoneAxe = MobAITweaks.getSecondaryWeapon(getType());
            if(!stoneAxe.isEmpty() && getWorld() instanceof ServerWorld server && random.nextFloat() < 0.5F * localDifficulty.getClampedLocalDifficulty()) EnchantmentHelper.applyEnchantmentProvider(stoneAxe, server.getRegistryManager(), EnchantmentProviders.MOB_SPAWN_EQUIPMENT, localDifficulty, random);
            getInventory().addStack(stoneAxe);
        }
        if(!MobAITweaks.getModConfigValue("pillagers_eat_food") || localDifficulty.getLocalDifficulty() < 1.5f) return;
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.BREAD, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.APPLE, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.CARROT, random.nextBetween(1, 16)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.BAKED_POTATO, random.nextBetween(1, 16)));
        if(localDifficulty.getLocalDifficulty() < 4.5f) return;
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.GOLDEN_APPLE, random.nextBetween(1, 4)));
        if(random.nextBoolean()) getInventory().addStack(new ItemStack(Items.GOLDEN_CARROT, random.nextBetween(1, 4)));
    }
    @Override public void shootAt(LivingEntity target, float pullProgress) {
        try {
            if (getMainHandStack() != null && !getMainHandStack().isEmpty() && getMainHandStack().getItem() instanceof BowItem) {
                ArrowEntity arrow = new ArrowEntity(getWorld(), this, getOffHandStack() == null || getOffHandStack().isEmpty() ? new ItemStack(Items.ARROW, 1) : getOffHandStack(), getMainHandStack() != null && !getMainHandStack().isEmpty() ? getMainHandStack() : new ItemStack(Items.BOW, 1));
                arrow.setVelocity(target.getEyePos().subtract(getEyePos()).normalize().multiply(pullProgress));
                getWorld().spawnEntity(arrow);
                getMainHandStack().damage(1, this, getPreferredEquipmentSlot(getMainHandStack()));
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
        else if(getMainHandStack().getItem().getClass().getSimpleName().contains("Gun") || getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun")) return State.CROSSBOW_HOLD;
        else if(getMainHandStack().getItem() instanceof CrossbowItem) return isCharging() ? State.CROSSBOW_CHARGE : State.CROSSBOW_HOLD;
        else if(isAttacking()) if(getMainHandStack().getItem() instanceof BowItem) return State.BOW_AND_ARROW;
        else return State.ATTACKING;
        return isUsingItem() ? State.CROSSBOW_HOLD : State.NEUTRAL;
    }
    @Override public boolean canPickupItem(ItemStack stack) {
        if(stack.getComponents() != null && (stack.getComponents().get(DataComponentTypes.FOOD) != null || stack.getComponents().get(DataComponentTypes.TOOL) != null || stack.getComponents().get(DataComponentTypes.FIREWORKS) != null)) return true;
        return super.canPickupItem(stack);
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof CrossbowItem;
    }
}
