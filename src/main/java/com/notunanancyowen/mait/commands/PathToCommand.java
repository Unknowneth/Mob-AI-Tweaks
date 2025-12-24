package com.notunanancyowen.mait.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class PathToCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("pathto").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("pathingMob", EntityArgumentType.entity()).then(CommandManager.argument("pathToLocationPos", Vec3ArgumentType.vec3()).executes(context -> pathTo(EntityArgumentType.getEntity(context,"pathingMob"), Vec3ArgumentType.getVec3(context, "pathToLocationPos"), 1.0, 0)).then(CommandManager.argument("speedMultiplier", DoubleArgumentType.doubleArg(0.0)).executes(context -> pathTo(EntityArgumentType.getEntity(context,"pathingMob"), Vec3ArgumentType.getVec3(context, "pathToLocationPos"), DoubleArgumentType.getDouble(context, "speedMultiplier"), 0)).then(CommandManager.argument("distanceFromDestination", IntegerArgumentType.integer(0)).executes(context -> pathTo(EntityArgumentType.getEntity(context,"pathingMob"), Vec3ArgumentType.getVec3(context, "pathToLocationPos"), DoubleArgumentType.getDouble(context, "speedMultiplier"), IntegerArgumentType.getInteger(context, "distanceFromDestination")))))).then(CommandManager.argument("pathToEntityPos", EntityArgumentType.entity()).executes(context -> pathTo(EntityArgumentType.getEntity(context,"pathingMob"), EntityArgumentType.getEntity(context,"pathToEntityPos"), 1.0)).then(CommandManager.argument("speedMultiplier", DoubleArgumentType.doubleArg(0.0)).executes(context -> pathTo(EntityArgumentType.getEntity(context,"pathingMob"), EntityArgumentType.getEntity(context,"pathToEntityPos"), DoubleArgumentType.getDouble(context, "speedMultiplier"))))).then(CommandManager.literal("stop").executes(context -> stop(EntityArgumentType.getEntity(context,"pathingMob"))))));
    }
    private static int pathTo(Entity entity, Vec3d vec3d, double speed, int distance) throws CommandSyntaxException {
        if(entity instanceof PathAwareEntity p) if(vec3d.distanceTo(entity.getPos()) < distance) throw new SimpleCommandExceptionType(Text.translatable("commands.pathto.invalidPathway", entity.getName())).create();
        else return p.getNavigation().startMovingTo(vec3d.getX(), vec3d.getY(), vec3d.getZ(), distance, speed) ? 1 : 0;
        throw new SimpleCommandExceptionType(Text.translatable("commands.pathto.invalidPather", entity.getName())).create();
    }
    private static int pathTo(Entity entity, Entity target, double speed) throws CommandSyntaxException {
        if(entity instanceof PathAwareEntity p) return p.getNavigation().startMovingTo(target, speed) ? 1 : 0;
        throw new SimpleCommandExceptionType(Text.translatable("commands.pathto.invalidPather", entity.getName())).create();
    }
    private static int stop(Entity entity) throws CommandSyntaxException {
        if(entity instanceof PathAwareEntity p) p.getNavigation().stop();
        else throw new SimpleCommandExceptionType(Text.translatable("commands.pathto.invalidPather", entity.getName())).create();
        return 1;
    }
}
