package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TameableEntity.class)
public class TameableEntityMixin {
    @Inject(method = "canTeleportTo", at = @At("HEAD"), cancellable = true)
    private void teleportToOwnerThatIsFlying(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if((TameableEntity)(Object)this instanceof ParrotEntity p && !p.isOnGround()) cir.setReturnValue(p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(pos.subtract(p.getBlockPos()))));
    }
}
