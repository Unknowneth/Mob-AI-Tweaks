package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.WardenAttacksInterface;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WardenEntity.class)
public abstract class WardenEntityMixin extends HostileEntity implements WardenAttacksInterface {
    @Unique private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
    @Unique private static final TrackedData<BlockPos> LAST_BELL = DataTracker.registerData(WardenEntityMixin.class, TrackedDataHandlerRegistry.BLOCK_POS);
    @Unique private int stunDuration = 0;
    WardenEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(CallbackInfo ci) {
        getDataTracker().startTracking(LAST_BELL, BlockPos.ORIGIN);
    }
    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void tickStunDuration(CallbackInfo ci) {
        bossBar.setPercent(getHealth() / getMaxHealth());
        if(hasCustomName()) bossBar.setName(getDisplayName());
        if(stunDuration > 0) {
            var lookDown = getRotationVector(0F, getYaw()).multiply(1d, 0d, 1d).add(getPos());
            getLookControl().lookAt(lookDown.x, lookDown.y, lookDown.z, 0F, 80F);
            BlockPos bellPos = getDataTracker().get(LAST_BELL);
            if(getWorld().getBlockEntity(bellPos) instanceof BellBlockEntity bell && !bell.ringing) bell.activate(bell.lastSideHit);
            stunDuration--;
            bossBar.setVisible(true);
            if (getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 3, 0.4d, 0.4d, 0.4d, 0.1d);
            ci.cancel();
        }
        else bossBar.setVisible(false);
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void addStunToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("StunTick", stunDuration);
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readStunToNbt(NbtCompound nbt, CallbackInfo ci) {
        if(nbt.contains("StunTick")) stunDuration = nbt.getInt("StunTick");
    }
    @Override protected void updatePostDeath() {
        super.updatePostDeath();
        bossBar.setPercent(0f);
        bossBar.clearPlayers();
        bossBar.setVisible(false);
    }
    @Override public void remove(RemovalReason reason) {
        if(reason == RemovalReason.KILLED) if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
        super.remove(reason);
    }
    @SuppressWarnings("all")
    @Override public void setBell(BlockPos blockPos) {
        stunDuration = 120;
        for(ServerPlayerEntity players : getServer().getPlayerManager().getPlayerList()) bossBar.addPlayer(players);
        getDataTracker().set(LAST_BELL, blockPos);
    }
}
