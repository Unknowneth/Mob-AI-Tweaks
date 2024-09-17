package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.goals.RavagerChargeAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if(MobAITweaks.getModConfigValue("ravagers_are_minibosses")) goalSelector.add(3, new RavagerChargeAttackGoal(this, 180));
    }
    @Override protected void updatePostDeath() {
        super.updatePostDeath();
        bossBar.setPercent(0f);
        bossBar.clearPlayers();
        bossBar.setVisible(false);
    }
    @Override protected void mobTick() {
        super.mobTick();
        if(!MobAITweaks.getModConfigValue("ravagers_are_minibosses")) return;
        if(getTarget() instanceof ServerPlayerEntity target && distanceTo(target) < 64) bossBar.addPlayer(target);
        if(getLastAttacker() instanceof ServerPlayerEntity attacker && distanceTo(attacker) < 64) bossBar.addPlayer(attacker);
        for(var player : bossBar.getPlayers()) if(distanceTo(player) >= 64) bossBar.removePlayer(player);
        bossBar.setPercent(getHealth() / getMaxHealth());
        if(hasCustomName()) bossBar.setName(getDisplayName());
    }

    @Override public void remove(RemovalReason reason) {
        if(reason == RemovalReason.KILLED) if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
        super.remove(reason);
    }
    @SuppressWarnings("all")
    @Override public void forceSpecialAttack() {
        roar();
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        stunTick = i;
    }
}
