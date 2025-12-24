package com.notunanancyowen.mait.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AttackCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("attack")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("attacker", EntityArgumentType.entity())
                        .then(CommandManager.literal("fake_melee")
                                .executes(context -> swingHand(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), false))
                                .then(CommandManager.argument("otherHand", BoolArgumentType.bool())
                                        .executes(context -> swingHand(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), BoolArgumentType.getBool(context,"otherHand")))))
                        .then(CommandManager.literal("custom_projectile")
                                .then(CommandManager.argument("projectile", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), null, 1.0F, 0.0F, new NbtCompound()))
                                        .then(CommandManager.argument("power", FloatArgumentType.floatArg(0.0F))
                                                .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), null, FloatArgumentType.getFloat(context, "power"), 0.0F, new NbtCompound()))
                                                .then(CommandManager.argument("uncertainty", FloatArgumentType.floatArg(0.0F))
                                                        .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), null, FloatArgumentType.getFloat(context, "power"), FloatArgumentType.getFloat(context, "uncertainty"), new NbtCompound()))
                                                        .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                                                .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), null, FloatArgumentType.getFloat(context, "power"), FloatArgumentType.getFloat(context, "uncertainty"), NbtCompoundArgumentType.getNbtCompound(context, "nbt"))))))))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .then(CommandManager.literal("with_melee")
                                        .executes(context -> tryAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), EntityArgumentType.getEntity(context, "target") , false))
                                        .then(CommandManager.argument("otherHand", BoolArgumentType.bool())
                                                .executes(context -> tryAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), EntityArgumentType.getEntity(context, "target"), BoolArgumentType.getBool(context,"otherHand")))))
                                .then(CommandManager.literal("from_range")
                                        .executes(context -> tryShoot(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), EntityArgumentType.getEntity(context, "target") , 1.0F))
                                        .then(CommandManager.argument("simulatedItemDrawTimeAmount", FloatArgumentType.floatArg(0.0F))
                                                .executes(context -> tryShoot(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), EntityArgumentType.getEntity(context, "target"), FloatArgumentType.getFloat(context, "simulatedItemDrawTimeAmount")))))
                                .then(CommandManager.literal("custom_projectile")
                                        .then(CommandManager.argument("projectile", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), EntityArgumentType.getEntity(context, "target"), 0.0F, 0.0F, new NbtCompound()))
                                                .then(CommandManager.argument("power", FloatArgumentType.floatArg(0.0F))
                                                        .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), EntityArgumentType.getEntity(context, "target"), FloatArgumentType.getFloat(context, "power"), 0.0F, new NbtCompound()))
                                                        .then(CommandManager.argument("uncertainty", FloatArgumentType.floatArg(0.0F))
                                                                .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), EntityArgumentType.getEntity(context, "target"), FloatArgumentType.getFloat(context, "power"), FloatArgumentType.getFloat(context, "uncertainty"), new NbtCompound()))
                                                                .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                                                        .executes(context -> customProjectileAttack(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), RegistryEntryReferenceArgumentType.getSummonableEntityType(context, "projectile"), EntityArgumentType.getEntity(context, "target"), FloatArgumentType.getFloat(context, "power"), FloatArgumentType.getFloat(context, "uncertainty"), NbtCompoundArgumentType.getNbtCompound(context, "nbt")))))))))));



    }
    private static int swingHand(ServerCommandSource source, Entity entity, boolean otherHand) throws CommandSyntaxException {
        if(entity instanceof LivingEntity l) {
            l.swingHand(otherHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
            source.sendFeedback(() -> Text.translatable("commands.attack.success", entity.getName(), ""), true);
            return l.handSwinging ? 1 : 0;
        }
        throw new SimpleCommandExceptionType(Text.translatable("commands.attack.invalidAttacker", entity.getName())).create();
    }
    private static int tryAttack(ServerCommandSource source, Entity entity, Entity target, boolean otherHand) throws CommandSyntaxException {
        if(entity instanceof LivingEntity l) {
            l.swingHand(otherHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
            source.sendFeedback(() -> Text.translatable("commands.attack.success", entity.getName(), target.getName()), true);
            return l.tryAttack(target) ? 1 : 0;
        }
        throw new SimpleCommandExceptionType(Text.translatable("commands.attack.invalidAttacker", entity.getName())).create();
    }
    private static int tryShoot(ServerCommandSource source, Entity entity, Entity target, float power) throws CommandSyntaxException {
        if(entity instanceof RangedAttackMob r) {
            if(target instanceof LivingEntity l) {
                r.shootAt(l, power);
                source.sendFeedback(() -> Text.translatable("commands.attack.success", entity.getName(), target.getName()), true);
                return 1;
            }
            throw new SimpleCommandExceptionType(Text.translatable("commands.attack.invalidTarget", entity.getName())).create();
        }
        throw new SimpleCommandExceptionType(Text.translatable("commands.attack.invalidShooter", entity.getName())).create();
    }
    private static int customProjectileAttack(ServerCommandSource source, Entity attacker, RegistryEntry.Reference<EntityType<?>> projectile, Entity target, float power, float uncertainty, NbtCompound nbt) throws CommandSyntaxException {
        Vec3d pos = source.getEntityAnchor().positionAt(attacker);
        NbtCompound nbtCompound = nbt.copy();
        nbtCompound.putString("id", projectile.registryKey().getValue().toString());
        ServerWorld serverWorld = source.getWorld();
        Entity entity = EntityType.loadEntityWithPassengers(nbtCompound, serverWorld, entityx -> {
            entityx.refreshPositionAndAngles(pos.x, pos.y, pos.z, entityx.getYaw(), entityx.getPitch());
            return entityx;
        });
        if (entity == null) throw new SimpleCommandExceptionType(Text.translatable("commands.summon.failed")).create();
        else {
            if (entity instanceof MobEntity m) {
                m.initialize(source.getWorld(), source.getWorld().getLocalDifficulty(entity.getBlockPos()), SpawnReason.COMMAND, null);
                if(m.hasStatusEffect(StatusEffects.NAUSEA)) uncertainty += (float)Math.sqrt(m.getStatusEffect(StatusEffects.NAUSEA).getAmplifier() / 255F) * 122.5F + 10F;
            }
            if (entity instanceof ProjectileEntity p) p.setOwner(attacker);
            if (target == null) entity.setVelocity(attacker.getRotationVector().multiply(power).add(source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty), source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty), source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty)));
            else entity.setVelocity(target.getEyePos().subtract(entity.getPos()).normalize().multiply(power).add(source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty), source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty), source.getWorld().getRandom().nextTriangular(0, 0.0172275 * uncertainty)));
        }
        if (!serverWorld.spawnNewEntityAndPassengers(entity)) throw new SimpleCommandExceptionType(Text.translatable("commands.summon.failed.uuid")).create();
        else return 1;
    }
}
