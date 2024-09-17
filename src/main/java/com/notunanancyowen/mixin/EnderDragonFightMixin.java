package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
    @Shadow @Final private ServerWorld world;
    @Shadow private @Nullable UUID dragonUuid;
    @Shadow @Final private ServerBossBar bossBar;
    @Shadow private int endCrystalsAlive;

    @Inject(method = "setSpawnState", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/boss/ServerBossBar;getPlayers()Ljava/util/Collection;", shift = At.Shift.BEFORE))
    private void spawnWithText(CallbackInfo ci) {
        if(world.getServer().getPlayerManager() != null) world.getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_summoned", Text.translatable("entity.minecraft.ender_dragon").getString()).formatted(Formatting.DARK_PURPLE), false);
    }
    @Inject(method = "updateFight", at = @At("TAIL"))
    private void fixBossBar(CallbackInfo ci) {
        if(world.getEntity(dragonUuid) instanceof EnderDragonAttacksInterface attacks) {
            if(endCrystalsAlive <= 2) attacks.endDPS();
            bossBar.setName(bossBar.getName().copy().formatted(attacks.isEnraged() ? Formatting.RED : attacks.isDPS() ? Formatting.WHITE : Formatting.YELLOW));
        }
    }
    @Inject(method = "dragonKilled", at = @At("TAIL"))
    private void dragonDied(EnderDragonEntity dragon, CallbackInfo ci) {
        if(world.getServer().getPlayerManager() != null) world.getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", dragon.getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
    }
}
