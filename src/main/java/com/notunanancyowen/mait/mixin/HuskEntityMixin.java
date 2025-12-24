package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.HuskBurrowGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HuskEntity.class)
public abstract class HuskEntityMixin extends ZombieEntity implements SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> BURROW_TIME = DataTracker.registerData(HuskEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    HuskEntityMixin(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }
    @Override public boolean damage(DamageSource source, float amount) {
        if(MobAITweaks.getModConfigValue("husks_can_burrow") && source.isOf(DamageTypes.IN_WALL) && (getWorld().getBlockState(getBlockPos()).isIn(BlockTags.SAND) || (getWorld().getBlockState(getBlockPos().up()).isIn(BlockTags.SAND) && !isBaby()))) return false;
        if(getFirstPassenger() != null) getFirstPassenger().setInvulnerable(false);
        return super.damage(source, amount);
    }
    @Override protected void initCustomGoals() {
        if(MobAITweaks.getModConfigValue("husks_can_burrow")) goalSelector.add(1, new HuskBurrowGoal(this));
        super.initCustomGoals();
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BURROW_TIME, 200);
        super.initDataTracker(builder);
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(BURROW_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(BURROW_TIME);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "BURROW";
    }
}
