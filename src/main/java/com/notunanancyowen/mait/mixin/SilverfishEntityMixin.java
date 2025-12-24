package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.EnumSet;

@Mixin(SilverfishEntity.class)
public abstract class SilverfishEntityMixin extends HostileEntity implements SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> SCARED = DataTracker.registerData(SilverfishEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private boolean canGoInvis = false;
    @Unique private int hideCooldown;
    protected SilverfishEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SCARED, 0);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickDigTime(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("silverfish_can_hide")) return;
        int i = getSpecialCooldown();
        if(i < 0) setSpecialCooldown(i + 1);
        else if(i > 0) if(canGoInvis) {
            if(i < 8) setSpecialCooldown(i + 1);
        }
        else if(i < 4) setSpecialCooldown(i + 1);
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("silverfish_can_hide")) {
            var HideGoal = new Goal() {
                private boolean strafeLeft;
                void init() {
                    setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                }
                @Override public boolean shouldRunEveryTick() {
                    return true;
                }
                @Override public boolean canStart() {
                    if(getLastAttacker() == null || --hideCooldown > 0) return false;
                    int i = getSpecialCooldown();
                    int j = age - getLastAttackedTime();
                    return i >= 0 && j > 20 && j < 20 + MobAITweaks.getModConfigValue("silverfish_hide_duration", 200) && (isOnGround() || (i > 0 && (i < 8 || !isSprinting())));
                }
                @Override public void start() {
                    canGoInvis = false;
                    strafeLeft = getRandom().nextBoolean();
                    getJumpControl().setActive();
                    getMoveControl().strafeTo(1F, 0F);
                    getNavigation().stop();
                    hideCooldown = 0;
                }
                @Override public void stop() {
                    setYaw(getYaw() + (strafeLeft ? 90 : -90));
                    setBodyYaw(getBodyYaw() + (strafeLeft ? 90 : -90));
                    setSpecialCooldown(-12);
                    setInvisible(false);
                    setSprinting(false);
                    canGoInvis = false;
                    getJumpControl().setActive();
                    hideCooldown = MobAITweaks.getModConfigValue("silverfish_hide_cooldown", 100);
                }
                @Override public void tick() {
                    if(!canGoInvis && getVelocity().getY() < 0 && !isOnGround()) canGoInvis = true;
                    if(canGoInvis && isOnGround()) {
                        setInvisible(true);
                        setSprinting(true);
                        getNavigation().stop();
                        if(getLastAttacker() != null) lookAtEntity(getLastAttacker(), 30F, 30F);
                        getMoveControl().strafeTo(0F, strafeLeft ? -1F : 1F);
                    }
                    if(getSpecialCooldown() == 0 && !isOnGround()) {
                        getMoveControl().strafeTo(1F, 0F);
                        setSpecialCooldown(1);
                    }
                }
            };
            HideGoal.init();
            goalSelector.add(0, HideGoal);
        }
    }
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void damageImmune(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("silverfish_can_hide") && getSpecialCooldown() > 0 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) cir.setReturnValue(false);
    }
    @ModifyExpressionValue(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;"))
    private Entity suppressCallForHelp(Entity original) {
        if(hideCooldown == 0) return null;
        return original;
    }
    @ModifyExpressionValue(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean suppressCallForHelp(boolean original) {
        if(hideCooldown == 0) return false;
        return original;
    }
    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/HostileEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void takeIncreasedDamageFromShovels(Args args) {
        if(args.get(0) instanceof DamageSource d && d.getAttacker() != null && d.getAttacker().getWeaponStack() != null && d.getAttacker().getWeaponStack().isIn(ItemTags.SHOVELS) && args.get(1) instanceof Float f) args.set(1, (MobAITweaks.getModConfigValue("silverfish_shovel_damage_boost", 100) * 0.01F + 1F) * f);
    }
    @Override protected void updatePostDeath() {
        int i = getSpecialCooldown();
        if(MobAITweaks.getModConfigValue("silverfish_can_hide") && i > 0) {
            setSpecialCooldown(-12);
            setInvisible(false);
            setSprinting(false);
            canGoInvis = false;
        }
        else if(i < 0) setSpecialCooldown(i + 1);
        super.updatePostDeath();
    }
    @Override public boolean isGlowing() {
        return super.isGlowing() && (getSpecialCooldown() == 0 || !isInvisible());
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(SCARED, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(SCARED);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "HIDE";
    }
}
