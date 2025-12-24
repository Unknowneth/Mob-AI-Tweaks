package com.notunanancyowen.mait.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.util.math.Vec3d;

public class RotateCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rotate").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("target", EntityArgumentType.entity()).then(CommandManager.argument("rotation", RotationArgumentType.rotation()).executes(context -> rotateToPos(context.getSource(), EntityArgumentType.getEntity(context, "target"), RotationArgumentType.getRotation(context, "rotation")))).then(CommandManager.literal("facing").then(CommandManager.literal("entity").then(CommandManager.argument("facingEntity", EntityArgumentType.entity()).executes(context -> rotateFacingLookTarget(context.getSource(), EntityArgumentType.getEntity(context, "target"), new RotateCommand.LookAtEntity(EntityArgumentType.getEntity(context, "facingEntity"), EntityAnchorArgumentType.EntityAnchor.FEET))).then(CommandManager.argument("facingAnchor", EntityAnchorArgumentType.entityAnchor()).executes(context -> rotateFacingLookTarget(context.getSource(), EntityArgumentType.getEntity(context, "target"), new RotateCommand.LookAtEntity(EntityArgumentType.getEntity(context, "facingEntity"), EntityAnchorArgumentType.getEntityAnchor(context, "facingAnchor"))))))).then(CommandManager.argument("facingLocation", Vec3ArgumentType.vec3()).executes(context -> rotateFacingLookTarget(context.getSource(), EntityArgumentType.getEntity(context, "target"), new RotateCommand.LookAtPosition(Vec3ArgumentType.getVec3(context, "facingLocation"))))))));
    }
    private static int rotateToPos(ServerCommandSource source, Entity entity, PosArgument pos) {
        Vec2f vec2f = pos.toAbsoluteRotation(source);
        entity.setYaw(vec2f.y);
        entity.setHeadYaw(vec2f.y);
        entity.setPitch(vec2f.x);
        source.sendFeedback(() -> Text.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }
    private static int rotateFacingLookTarget(ServerCommandSource source, Entity entity, LookTarget lookTarget) {
        lookTarget.look(source, entity);
        source.sendFeedback(() -> Text.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }
    record LookAtEntity(Entity entity, EntityAnchorArgumentType.EntityAnchor anchor) implements RotateCommand.LookTarget {
        @Override public void look(ServerCommandSource source, Entity entity) {
            if (entity instanceof ServerPlayerEntity serverPlayerEntity) serverPlayerEntity.lookAtEntity(source.getEntityAnchor(), this.entity, this.anchor);
            else entity.lookAt(source.getEntityAnchor(), this.anchor.positionAt(this.entity));
        }
    }
    record LookAtPosition(Vec3d position) implements RotateCommand.LookTarget {
        @Override public void look(ServerCommandSource source, Entity entity) {
            entity.lookAt(source.getEntityAnchor(), this.position);
        }
    }
    @FunctionalInterface
    interface LookTarget {
        void look(ServerCommandSource source, Entity entity);
    }
}
