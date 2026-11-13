package com.dougg865.bloomberto.api.datagen;

/** The data pack format differences between supported Minecraft versions. */
public enum Formats {
    /** Minecraft 1.20.1: plural folders ({@code recipes}, {@code loot_tables}, {@code tags/items}), recipe results with {@code item}. */
    V1_20_1(false),
    /** Minecraft 1.21.1: singular folders ({@code recipe}, {@code loot_table}, {@code tags/item}), recipe results with {@code id}. */
    V1_21_1(true);

    final boolean singular;

    Formats(boolean singular) {
        this.singular = singular;
    }

    String recipeDir() {
        return singular ? "recipe" : "recipes";
    }

    String lootDir() {
        return singular ? "loot_table" : "loot_tables";
    }

    String tagDir(String kind) {
        return "tags/" + (singular ? kind : kind + "s");
    }

    /** The format of the Minecraft version this build of Bloomberto targets. */
    public static Formats current() {
        //? if >=1.21 {
        return V1_21_1;
        //?} else {
        /*return V1_20_1;
        *///?}
    }
}
