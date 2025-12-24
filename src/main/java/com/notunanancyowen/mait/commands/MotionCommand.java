package com.notunanancyowen.mait.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class MotionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("motion")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                                .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, BoolArgumentType.getBool(context, "strafe"))))))
                                        .then(CommandManager.literal("~")
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 2, BoolArgumentType.getBool(context, "strafe")))))))
                                .then(CommandManager.literal("~")
                                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, BoolArgumentType.getBool(context, "strafe"))))))
                                        .then(CommandManager.literal("~")
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.of(DoubleArgumentType.getDouble(context, "x")), Optional.empty(), Optional.empty(), 2, BoolArgumentType.getBool(context, "strafe"))))))))
                        .then(CommandManager.literal("~")
                                .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, BoolArgumentType.getBool(context, "strafe"))))))
                                        .then(CommandManager.literal("~")
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "y")), Optional.empty(), 2, BoolArgumentType.getBool(context, "strafe")))))))
                                .then(CommandManager.literal("~")
                                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.of(DoubleArgumentType.getDouble(context, "z")), 2, BoolArgumentType.getBool(context, "strafe"))))))
                                        .then(CommandManager.literal("~")
                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 0, false))
                                                .then(CommandManager.literal("set")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 0, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 0, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("add")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 1, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 1, BoolArgumentType.getBool(context, "strafe")))))
                                                .then(CommandManager.literal("multiply")
                                                        .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 2, false))
                                                        .then(CommandManager.argument("strafe", BoolArgumentType.bool())
                                                                .executes(context -> move(EntityArgumentType.getEntity(context, "entity"), Optional.empty(), Optional.empty(), Optional.empty(), 2, BoolArgumentType.getBool(context, "strafe"))))))))));
    }
    @SuppressWarnings("all")
    private static int move(Entity entity, Optional<Double> x, Optional<Double> y, Optional<Double> z, int mode, boolean strafe) {
        Vec3d velocity = new Vec3d(x.orElse(mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getX()), y.orElse(mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getY()), z.orElse(mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getZ()));
        if(strafe) {
            velocity = velocity.rotateY(entity.getYaw() / -180F * (float)Math.PI - (float)Math.PI / 2F);
            if(x.isEmpty()) velocity = new Vec3d(mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getX(), velocity.getY(), velocity.getZ());
            if(y.isEmpty()) velocity = new Vec3d(velocity.getX(), mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getY(), velocity.getZ());
            if(z.isEmpty()) velocity = new Vec3d(velocity.getX(), velocity.getY(), mode == 2 ? 1 : mode == 1 ? 0 : entity.getVelocity().getZ());
        }
        switch (mode) {
            case 0 -> entity.setVelocity(velocity);
            case 1 -> entity.addVelocity(velocity);
            case 2 -> entity.setVelocity(entity.getVelocity().multiply(velocity));
        }
        entity.velocityModified = true;
        entity.velocityDirty = true;
        return 1;
    }
}
