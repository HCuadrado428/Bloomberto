package com.dougg865.bloomberto.impl.command;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.config.Config;
import com.dougg865.bloomberto.api.config.ConfigValue;
import com.dougg865.bloomberto.api.config.Configs;
import com.dougg865.bloomberto.impl.config.ConfigSync;
import com.dougg865.bloomberto.platform.Services;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /bloomberto reload} re-reads every registered config file and re-sends synced values to the players;
 * {@code /bloomberto info} prints versions and config values. Requires permission level 2.
 */
public final class BloombertoCommand {
    private BloombertoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bloomberto")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    int count = reloadConfigs();
                    ConfigSync.sendToAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Bloomberto: reloaded " + count + " config file(s)"), true);
                    return count;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    for (String line : info()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return 1;
                })));
    }

    /** Re-reads every registered config from disk. @return how many were reloaded */
    public static int reloadConfigs() {
        int count = 0;
        for (Config config : Configs.all()) {
            config.load();
            count++;
        }
        return count;
    }

    /** The lines printed by {@code /bloomberto info}. */
    public static java.util.List<String> info() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Bloomberto " + Bloomberto.MOD_NAME + " on " + Services.PLATFORM.getLoaderName()
                + ", Minecraft " + SharedConstants.getCurrentVersion().getName());
        for (Config config : Configs.all()) {
            lines.add("[" + config.modId() + "] " + config.file().getFileName());
            for (ConfigValue<?> value : config.values()) {
                lines.add("  " + value.path() + " = " + value.encode() + (value.isSynced() ? " (synced)" : ""));
            }
        }
        return lines;
    }
}
