package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkeletonEntity.class)
public abstract class WitherSkeletonEntityMixin extends AbstractSkeletonEntity implements SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> SKULL_TIME = DataTracker.registerData(WitherSkeletonEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    WitherSkeletonEntityMixin(EntityType<? extends WitherSkeletonEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("wither_skeleton_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, 200));
    }
    @Override protected void initDataTracker() {
        super.initDataTracker();
        getDataTracker().startTracking(SKULL_TIME, 0);
    }
    @SuppressWarnings("all")
    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    private void equipWithOtherGear(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        var whereevertfami = getServer().getWorld(getWorld().getRegistryKey());
        switch(random.nextBetween(1, 12)) {
            case 3:
                tryEquip(new ItemStack(MobAITweaks.getRandomBow(random), 1));
                enchantMainHandItem(random, localDifficulty.getLocalDifficulty());
                ci.cancel();
                break;
            case 7:
                tryEquip(new ItemStack(Items.GOLDEN_SWORD, 1));
                enchantMainHandItem(random, localDifficulty.getLocalDifficulty());
                ci.cancel();
                break;
            case 11:
                if(localDifficulty.getLocalDifficulty() < 5f) break;
                tryEquip(new ItemStack(Items.NETHERITE_SWORD, 1));
                enchantMainHandItem(random, localDifficulty.getLocalDifficulty());
                ci.cancel();
        }
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(SKULL_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(SKULL_TIME);
    }
}
