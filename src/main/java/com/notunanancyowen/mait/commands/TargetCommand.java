package com.notunanancyowen.mait.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class TargetCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("target").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("attacker", EntityArgumentType.entity()).then(CommandManager.argument("target", EntityArgumentType.entity()).executes(context -> target(context.getSource(), EntityArgumentType.getEntity(context, "attacker"), EntityArgumentType.getEntity(context, "target"))))));
    }
    private static int target(ServerCommandSource source, Entity entity, Entity target) throws CommandSyntaxException {
        if(entity instanceof MobEntity m) {
            if(target instanceof LivingEntity l) {
                if(m.getId() == l.getId()) throw new SimpleCommandExceptionType(Text.translatable("commands.target.selfHarmNotAllowed")).create();
                m.setTarget(l);
                if(m.getTarget() != null) source.sendFeedback(() -> Text.translatable("commands.target.success", m.getName(), l.getName()), true);
                else throw new SimpleCommandExceptionType(Text.translatable("commands.target.fail", m.getName(), l.getName())).create();
                return 1;
            }
            else throw new SimpleCommandExceptionType(Text.translatable("commands.target.invalidTarget", target.getName())).create();
        }
        else throw new SimpleCommandExceptionType(Text.translatable("commands.target.invalidAttacker", entity.getName())).create();
    }
}
