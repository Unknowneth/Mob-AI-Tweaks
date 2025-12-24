package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.RavagerChargeAttackGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RavagerEntity.class)
public abstract class RavagerEntityMixin extends RaiderEntity implements SpecialAttacksInterface {
    @Shadow protected abstract void roar();
    @Shadow private int stunTick;
    @Unique private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);
    RavagerEntityMixin(EntityType<? extends RavagerEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ravager_special_attacks")) goalSelector.add(3, new RavagerChargeAttackGoal((RavagerEntity)(RaiderEntity)this, MobAITweaks.getModConfigValue("ravager_special_attack_cooldown", 180)));
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void updateHealthBar(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("ravagers_are_minibosses")) return;
        if(getTarget() instanceof ServerPlayerEntity target && distanceTo(target) < 64) bossBar.addPlayer(target);
        if(getLastAttacker() instanceof ServerPlayerEntity attacker && distanceTo(attacker) < 64) bossBar.addPlayer(attacker);
        for(var player : bossBar.getPlayers()) if(distanceTo(player) >= 64) bossBar.removePlayer(player);
        bossBar.setPercent(getHealth() / getMaxHealth());
        if(hasCustomName()) bossBar.setName(getDisplayName());
    }
    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void stopSpecialAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("ravager_special_attacks") && cir.getReturnValue()) goalSelector.getGoals().stream().filter(p -> p.getGoal() instanceof RavagerChargeAttackGoal).findFirst().ifPresent(p -> p.getGoal().start());
    }
    @Override protected void updatePostDeath() {
        super.updatePostDeath();
        bossBar.setPercent(0f);
        bossBar.clearPlayers();
        bossBar.setVisible(false);
    }
    @Override protected void onRemoval(RemovalReason reason) {
        if(MobAITweaks.getModConfigValue("ravagers_are_minibosses")) {
            bossBar.clearPlayers();
            bossBar.setVisible(false);
        }
        super.onRemoval(reason);
    }
    @SuppressWarnings("all")
    @Override public void forceSpecialAttack() {
        roar();
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        stunTick = i;
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "DASH";
    }
}
