package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SkeletonSpecificGoal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkeletonEntity.class)
public abstract class WitherSkeletonEntityMixin extends AbstractSkeletonEntity {
    WitherSkeletonEntityMixin(EntityType<? extends WitherSkeletonEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("wither_skeleton_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this,  MobAITweaks.getModConfigValue("wither_skeleton_special_attack_cooldown", 200)));
    }
    @SuppressWarnings("all")
    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    private void equipWithOtherGear(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0) == 0 && random.nextInt(100) < MobAITweaks.getModConfigValue("wither_skeletons_with_bows_chance", 10) && localDifficulty.getLocalDifficulty() > MobAITweaks.getModConfigValue("wither_skeletons_with_bows_local_difficulty", 250) * 0.01F) {
            var i = MobAITweaks.getRandomBow(random);
            if(FabricLoader.getInstance().isModLoaded("bows") && (i == Items.AIR || i == Items.BOW)) i = Registries.ITEM.get(Identifier.of("bows", "stone_bow"));
            if(i == null || i == Items.AIR) i = Items.BOW;
            tryEquip(i.getDefaultStack());
            if(getWorld() instanceof ServerWorld server) enchantMainHandItem(server, random, localDifficulty);
            ci.cancel();
        }
    }
}
