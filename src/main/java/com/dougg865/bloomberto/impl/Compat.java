package com.dougg865.bloomberto.impl;

import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Small differences between the Minecraft versions Bloomberto supports, in one place so the rest of the code stays free of
 * version conditions. Server-safe: nothing here touches client classes.
 */
public final class Compat {
    private Compat() {
    }

    /** The id of a registry key ({@code location()} before 1.21.11, {@code identifier()} after). */
    public static ResourceLocation id(ResourceKey<?> key) {
        //? if >=1.21.11 {
        /*return key.identifier();
        *///?} else {
        return key.location();
        //?}
    }

    public static String playerName(Player player) {
        //? if >=1.21.11 {
        /*return player.getGameProfile().name();
        *///?} else {
        return player.getGameProfile().getName();
        //?}
    }

    /** The server an entity lives on, or {@code null} on the client. */
    public static MinecraftServer server(Entity entity) {
        //? if >=1.21.11 {
        /*return entity.level().getServer();
        *///?} else {
        return entity.getServer();
        //?}
    }

    public static Set<String> keys(CompoundTag tag) {
        //? if >=1.21.11 {
        /*return tag.keySet();
        *///?} else {
        return tag.getAllKeys();
        //?}
    }

    /** The number in a numeric tag, or {@code null} for any other tag. */
    public static Number number(Tag tag) {
        if (!(tag instanceof NumericTag n)) {
            return null;
        }
        //? if >=1.21.11 {
        /*return n.box();
        *///?} else {
        return n.getAsNumber();
        //?}
    }

    /** The text of a string tag, or {@code null} for any other tag. */
    public static String string(Tag tag) {
        if (!(tag instanceof StringTag s)) {
            return null;
        }
        //? if >=1.21.11 {
        /*return s.value();
        *///?} else {
        return s.getAsString();
        //?}
    }

    /** Operator permission (command level 2, "game masters"). */
    public static boolean isGameMaster(CommandSourceStack source) {
        //? if >=1.21.11 {
        /*return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return source.hasPermission(2);
        //?}
    }

    public static String minecraftVersion() {
        //? if >=1.21.11 {
        /*return net.minecraft.SharedConstants.getCurrentVersion().name();
        *///?} else {
        return net.minecraft.SharedConstants.getCurrentVersion().getName();
        //?}
    }
}
