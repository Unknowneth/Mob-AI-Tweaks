package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.TakeoffPhase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TakeoffPhase.class)
public abstract class EnderDragonTakeOffMixin extends AbstractPhase {
    EnderDragonTakeOffMixin(EnderDragonEntity dragon) {
        super(dragon);
    }
    @Override public void endPhase() {
        if(MobAITweaks.getModConfigValue("ender_dragon_rework") && dragon instanceof EnderDragonAttacksInterface attacks && attacks.isDPS()) attacks.endDPS();
        super.endPhase();
    }
}
