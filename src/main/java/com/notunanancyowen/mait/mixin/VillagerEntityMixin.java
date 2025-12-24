package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.VillagerEatItemToHealGoal;
import com.notunanancyowen.mait.goals.VillagerSpecificRoleGoal;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    @Shadow public abstract VillagerData getVillagerData();
    @Shadow private int experience;
    @Shadow protected abstract void sayNo();
    @Shadow public abstract int getReputation(PlayerEntity player);
    @Shadow protected abstract boolean canLevelUp();
    @Shadow private int levelUpTimer;
    @Shadow private boolean levelingUp;
    @Unique private int servicesDone = 0;
    VillagerEntityMixin(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("villagers_eat_food")) goalSelector.add(2, new VillagerEatItemToHealGoal(this));
        if(MobAITweaks.getModConfigValue("villagers_have_special_roles")) goalSelector.add(0, new VillagerSpecificRoleGoal((VillagerEntity)(MerchantEntity)this, 32));
    }
    @Override protected void onKilledBy(@Nullable LivingEntity adversary) {
        super.onKilledBy(adversary);
        if(MobAITweaks.getModConfigValue("pillagers_eat_food") && adversary instanceof PillagerEntity pillager) for(int i = 0; i < getInventory().size(); i++) pillager.getInventory().addStack(getInventory().getStack(i));
    }
    @Redirect(method = "mobTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/Brain;tick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;)V"))
    private void suppressBrainWhileUsingSpecificGoal(Brain<LivingEntity> instance, ServerWorld world, LivingEntity entity) {
        if(goalSelector.getGoals().stream().noneMatch(p -> p.getGoal() instanceof VillagerSpecificRoleGoal v && v.shouldRunEveryTick() && p.isRunning())) {
            instance.tick(world, entity);
            if(servicesDone < MobAITweaks.getModConfigValue("villagers_max_special_services", 12) && !getMainHandStack().isEmpty()) instance.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER).ifPresent(player -> {
                if(player.isSneaking() && !player.isCreative()) {
                    VillagerProfession prof = getVillagerData().getProfession();
                    if(prof == VillagerProfession.CLERIC && player.getHealth() < player.getMaxHealth()) {
                        ItemStack fakeItem = new ItemStack(Items.SPLASH_POTION, 1);
                        fakeItem.apply(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.HEALING), (PotionContentsComponent potionContent) -> potionContent.with(Potions.HEALING));
                        setStackInHand(Hand.MAIN_HAND, fakeItem);
                    }
                    else if(prof == VillagerProfession.ARMORER) {
                        for(var b : player.getArmorItems()) if(!b.isEmpty() && b.getDamage() > 0) {
                            setStackInHand(Hand.MAIN_HAND, Items.ANVIL.getDefaultStack());
                            break;
                        }
                    }
                    else if(prof == VillagerProfession.WEAPONSMITH && player.getOffHandStack().getDamage() > 0 && (player.getOffHandStack().isIn(ConventionalItemTags.RANGED_WEAPON_TOOLS) || player.getOffHandStack().isIn(ConventionalItemTags.SHIELD_TOOLS) || player.getOffHandStack().isIn(ConventionalItemTags.MACE_TOOLS) || player.getOffHandStack().isIn(ItemTags.SWORDS) || player.getOffHandStack().isIn(ItemTags.AXES))) setStackInHand(Hand.MAIN_HAND, Items.ANVIL.getDefaultStack());
                    else if(prof == VillagerProfession.TOOLSMITH && player.getOffHandStack().getDamage() > 0 && (player.getOffHandStack().isIn(ConventionalItemTags.TOOLS))) setStackInHand(Hand.MAIN_HAND, Items.ANVIL.getDefaultStack());
                    else if(prof == VillagerProfession.FLETCHER && player.getOffHandStack().isIn(ItemTags.ARROWS)) setStackInHand(Hand.MAIN_HAND, player.getOffHandStack());
                }
            });
        }
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readServiceData(NbtCompound nbt, CallbackInfo ci) {
        servicesDone = nbt.getInt("ServicesGiven");
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void saveServiceData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("ServicesGiven", servicesDone);
    }
    @Inject(method = "restock", at = @At("HEAD"))
    private void restockServices(CallbackInfo ci) {
        servicesDone = 0;
    }
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void newInteractionMechanics(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        int maxServices = MobAITweaks.getModConfigValue("villagers_max_special_services", 12);
        if(maxServices <= 0) return;
        ItemStack heldItem = player.getStackInHand(hand);
        if(player.isSneaking() && !player.isCreative() && heldItem.isOf(Items.EMERALD)) {
            if(servicesDone >= maxServices) {
                sayNo();
                if(MobAITweaks.getModConfigValue("villagers_announce_special_services") && !getWorld().isClient) player.sendMessage(Text.translatable("mob-ai-tweaks.villager_out_of_service", getDisplayName()), true);
                cir.setReturnValue(ActionResult.CONSUME_PARTIAL);
                return;
            }
            VillagerProfession prof = getVillagerData().getProfession();
            if(prof == VillagerProfession.CLERIC && player.getHealth() < player.getMaxHealth()) {
                int price = Math.clamp(8 - getReputation(player) / 8 , 1, 64);
                if(heldItem.getCount() < price || getTarget() != null) {
                    sayNo();
                    if(MobAITweaks.getModConfigValue("villagers_announce_special_services") && !getWorld().isClient) player.sendMessage(Text.translatable(getTarget() == null ? "mob-ai-tweaks.villager_denies_service" : "mob-ai-tweaks.villager_duped_service", getTarget() != null ? getDisplayName() : price), true);
                    cir.setReturnValue(ActionResult.CONSUME_PARTIAL);
                    return;
                }
                heldItem.decrementUnlessCreative(price, player);
                setHeadRollingTimeLeft(0);
                setTarget(player);
                ItemStack fakeItem = new ItemStack(Items.SPLASH_POTION, 1);
                fakeItem.apply(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.HEALING), (PotionContentsComponent potionContent) -> potionContent.with(Potions.HEALING));
                setStackInHand(Hand.MAIN_HAND, fakeItem);
                experience += 5;
                if(canLevelUp()) {
                    levelUpTimer = 40;
                    levelingUp = true;
                }
                else servicesDone++;
                playSoundIfNotSilent(SoundEvents.ENTITY_VILLAGER_WORK_CLERIC);
                getWorld().sendEntityStatus(this, EntityStatuses.ADD_VILLAGER_HAPPY_PARTICLES);
                cir.setReturnValue(ActionResult.success(getWorld().isClient));
            }
            else if(prof == VillagerProfession.ARMORER) {
                int price = 0;
                var a = player.getArmorItems();
                for(var b : a) if(!b.isEmpty() && b.getDamage() > 0) price += b.getDamage() / 20;
                if(price <= 0) return;
                price = Math.clamp(price - getReputation(player) / 12, 1, 64);
                if(heldItem.getCount() < price) {
                    sayNo();
                    if(MobAITweaks.getModConfigValue("villagers_announce_special_services") && !getWorld().isClient) player.sendMessage(Text.translatable("mob-ai-tweaks.villager_denies_service", price), true);
                    cir.setReturnValue(ActionResult.CONSUME_PARTIAL);
                    return;
                }
                heldItem.decrementUnlessCreative(price, player);
                setHeadRollingTimeLeft(0);
                for(var b : a) if(!b.isEmpty() && b.getDamage() > 0) b.setDamage(0);
                experience++;
                experience += price / 8;
                if(canLevelUp()) {
                    levelUpTimer = 40;
                    levelingUp = true;
                }
                else servicesDone++;
                playSoundIfNotSilent(SoundEvents.BLOCK_ANVIL_USE);
                getWorld().sendEntityStatus(this, EntityStatuses.ADD_VILLAGER_HAPPY_PARTICLES);
                cir.setReturnValue(ActionResult.success(getWorld().isClient));
            }
            else if(((prof == VillagerProfession.WEAPONSMITH && (player.getOffHandStack().isIn(ConventionalItemTags.RANGED_WEAPON_TOOLS) || player.getOffHandStack().isIn(ConventionalItemTags.SHIELD_TOOLS) || player.getOffHandStack().isIn(ConventionalItemTags.MACE_TOOLS) || player.getOffHandStack().isIn(ItemTags.SWORDS) || player.getOffHandStack().isIn(ItemTags.AXES))) || (prof == VillagerProfession.TOOLSMITH && (player.getOffHandStack().isIn(ConventionalItemTags.TOOLS)))) && player.getOffHandStack().getDamage() > 0) {
                int price = Math.clamp(player.getOffHandStack().getDamage() / 20, 1, 64);
                if(heldItem.getCount() < price) {
                    sayNo();
                    if(MobAITweaks.getModConfigValue("villagers_announce_special_services") && !getWorld().isClient) player.sendMessage(Text.translatable("mob-ai-tweaks.villager_denies_service", price), true);
                    cir.setReturnValue(ActionResult.CONSUME_PARTIAL);
                    return;
                }
                heldItem.decrementUnlessCreative(price, player);
                setHeadRollingTimeLeft(0);
                player.getOffHandStack().setDamage(0);
                experience++;
                experience += price / 8;
                if(canLevelUp()) {
                    levelUpTimer = 40;
                    levelingUp = true;
                }
                else servicesDone++;
                playSoundIfNotSilent(SoundEvents.BLOCK_ANVIL_USE);
                getWorld().sendEntityStatus(this, EntityStatuses.ADD_VILLAGER_HAPPY_PARTICLES);
                cir.setReturnValue(ActionResult.success(getWorld().isClient));
            }
            else if(prof == VillagerProfession.FLETCHER && player.getOffHandStack().isIn(ItemTags.ARROWS)) {
                heldItem.decrementUnlessCreative(1, player);
                player.giveItemStack(player.getOffHandStack().copyWithCount(8));
                setHeadRollingTimeLeft(0);
                experience++;
                if(canLevelUp()) {
                    levelUpTimer = 40;
                    levelingUp = true;
                }
                else servicesDone++;
                playSoundIfNotSilent(SoundEvents.ENTITY_VILLAGER_WORK_FLETCHER);
                getWorld().sendEntityStatus(this, EntityStatuses.ADD_VILLAGER_HAPPY_PARTICLES);
                cir.setReturnValue(ActionResult.success(getWorld().isClient));
            }
        }
    }
}
