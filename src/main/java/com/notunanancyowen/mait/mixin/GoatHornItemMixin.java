package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GoatHornItem;
import net.minecraft.item.Instrument;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoatHornItem.class)
public abstract class GoatHornItemMixin {
    @Inject(method = "playSound", at = @At("TAIL"))
    private static void tpPetsToSelf(World world, PlayerEntity user, Instrument instrument, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("goat_horns_draw_aggro")) world.getEntitiesByClass(HostileEntity.class, user.getBoundingBox().expand(128), h -> ((h.getTarget() instanceof PlayerEntity p && !p.getUuid().equals(user.getUuid())) || h.getTarget() == null) && h.canTarget(user)).forEach(h -> h.setTarget(user));
    }
}
