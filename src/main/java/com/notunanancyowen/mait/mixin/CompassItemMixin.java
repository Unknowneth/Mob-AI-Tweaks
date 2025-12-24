package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Objects;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {
    public CompassItemMixin(Settings settings) {
        super(settings);
    }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var currentItem = user.getStackInHand(hand);
        if(currentItem.getEnchantments().getEnchantments().stream().noneMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "recall")))) return super.use(world, user, hand);
        if(user instanceof ServerPlayerEntity player) if(currentItem.contains(DataComponentTypes.LODESTONE_TRACKER) && Objects.requireNonNull(currentItem.get(DataComponentTypes.LODESTONE_TRACKER)).target().isPresent() && player.getServer() != null) Objects.requireNonNull(currentItem.get(DataComponentTypes.LODESTONE_TRACKER)).target().ifPresent(p -> player.teleportTo(new TeleportTarget(player.getServer().getWorld(p.dimension()), p.pos().toBottomCenterPos(), player.getVelocity(), player.getYaw(), player.getPitch(), e -> e.playSound(SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, 0.8F, 0.2F))));
        else player.teleportTo(player.getRespawnTarget(user.isAlive(), e -> e.playSound(SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, 0.8F, 0.2F)));
        return TypedActionResult.success(currentItem, world.isClient());
    }
}
