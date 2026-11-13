package com.dougg865.bloomberto.api.datagen;

import com.google.gson.JsonObject;

/** A recipe ingredient: a single item or an item tag. Ids like {@code "mymod:ruby"}; without a namespace the mod's own is used. */
public record Ingredient(String kind, String id) {
    public static Ingredient item(String id) {
        return new Ingredient("item", id);
    }

    public static Ingredient tag(String id) {
        return new Ingredient("tag", id);
    }

    JsonObject toJson(java.util.function.UnaryOperator<String> qualify) {
        JsonObject o = new JsonObject();
        o.addProperty(kind, qualify.apply(id));
        return o;
    }
}
