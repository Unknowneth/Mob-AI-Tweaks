package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
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
    @Inject(method = "updateFight", at = @At("TAIL"))
    private void fixBossBar(CallbackInfo ci) {
        if(world.getEntity(dragonUuid) instanceof EnderDragonAttacksInterface attacks) {
            boolean healingRework = MobAITweaks.getModConfigValue("ender_dragon_healing_rework");
            if(healingRework && endCrystalsAlive <= 2) attacks.endDPS();
            bossBar.setName(bossBar.getName().copy().formatted(attacks.isEnraged() ? Formatting.RED : attacks.isDPS() || !healingRework ? Formatting.WHITE : Formatting.YELLOW));
        }
    }
}
