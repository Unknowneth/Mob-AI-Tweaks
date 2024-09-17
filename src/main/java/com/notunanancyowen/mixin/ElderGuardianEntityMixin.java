package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.goals.ElderGuardianDashAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;

@Mixin(ElderGuardianEntity.class)
public abstract class ElderGuardianEntityMixin extends GuardianEntity implements SpecialAttacksInterface {
    @Unique private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
    @Unique private int dashCooldown = 0;
    ElderGuardianEntityMixin(EntityType<? extends ElderGuardianEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void bossHealthBar(CallbackInfo ci) {
        if(dashCooldown > 0) dashCooldown--;
        if(!MobAITweaks.getModConfigValue("elder_guardians_are_bosses")) return;
        if(getTarget() instanceof ServerPlayerEntity target && distanceTo(target) < 64) bossBar.addPlayer(target);
        if(getLastAttacker() instanceof ServerPlayerEntity attacker && distanceTo(attacker) < 64) bossBar.addPlayer(attacker);
        for(var player : bossBar.getPlayers()) if(distanceTo(player) >= 64) bossBar.removePlayer(player);
        bossBar.setPercent(getHealth() / getMaxHealth());
        if(hasCustomName()) bossBar.setName(getDisplayName());
    }
    @ModifyReturnValue(method = "createElderGuardianAttributes", at = @At("TAIL"))
    private static DefaultAttributeContainer.Builder attribution(DefaultAttributeContainer.Builder original) {
        if(!MobAITweaks.getModConfigValue("elder_guardians_are_bosses")) return original;
        return original.add(EntityAttributes.GENERIC_ARMOR, 8.0d).add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0d);
    }
    @Override protected void initGoals() {
        if(MobAITweaks.getModConfigValue("elder_guardians_are_bosses")) goalSelector.add(2, new ElderGuardianDashAttackGoal(this));
        super.initGoals();
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("dashCooldown", getSpecialCooldown());
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setSpecialCooldown(nbt.getInt("dashCooldown"));
    }
    @Override public boolean damage(DamageSource source, float amount) {
        if(source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.FALL) || source.getAttacker() == this) return false;
        return super.damage(source, amount);
    }
    @Override protected void updatePostDeath() {
        bossBar.clearPlayers();
        bossBar.setVisible(false);
        for(var goal : goalSelector.getGoals()) goal.stop();
        for(var target : targetSelector.getGoals()) target.stop();
        if(getWorld().isClient()) {
            hurtTime = getRandom().nextBetween(1, 9);
            getWorld().addParticle(ParticleTypes.EXPLOSION, getX() + getRandom().nextBetween(-3, 3), MathHelper.lerp(0.5d, getEyeY(), getY()) + getRandom().nextBetween(-3, 3), getZ() + getRandom().nextBetween(-3, 3), 0d, 0d, 0d);
            return;
        }
        deathTime++;
        int totalDeathTime = DEATH_TICKS * 4;
        if(deathTime == totalDeathTime - 1) {
            if(getDeathSound() != null) playSound(getDeathSound(), getSoundVolume(), getSoundPitch());
        }
        else if(!isRemoved() && deathTime >= totalDeathTime) {
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
            getWorld().emitGameEvent(this, GameEvent.ENTITY_DIE, getPos());
            remove(RemovalReason.KILLED);
        }
    }
    @Override public boolean disablesShield() {
        return true;
    }
    @SuppressWarnings("all")
    @Override public void forceSpecialAttack() {
        try {
            goalSelector.getGoals().stream().filter(goal -> !(goal.getGoal() instanceof ElderGuardianDashAttackGoal) && !(goal.getGoal().getControls().contains(Goal.Control.TARGET))).forEach(goal -> goal.getGoal().stop());
        }
        catch (Throwable ignore) {
        }
        wanderGoal.stop();
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        dashCooldown = i;
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return dashCooldown;
    }
}
