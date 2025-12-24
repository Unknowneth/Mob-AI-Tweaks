package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayEntity.class)
public abstract class DisplayEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void despawnDoor(CallbackInfo ci) {
        if((DisplayEntity)(Object)this instanceof DisplayEntity.BlockDisplayEntity me && me.getCommandTags().contains("door_zombie") && me.getVehicle() == null) {
            me.getPassengerList().forEach(e -> {
                if(e instanceof DisplayEntity.BlockDisplayEntity) e.discard();
            });
            me.discard();
            ci.cancel();
        }
    }
}
