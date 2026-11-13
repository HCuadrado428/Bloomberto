package com.dougg865.bloomberto.api.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Generates a mod's JSON resources (item and block models, block states, language files, recipes, loot tables and tags)
 * from code, in the format of the Minecraft version it runs on. The same code works on every loader.
 *
 * <pre>{@code
 * DataGenerator gen = DataGenerator.create("mymod", outputDir);
 * gen.lang("en_us").add("item.mymod.ruby", "Ruby").add("block.mymod.ruby_block", "Block of Ruby");
 * gen.itemModel("ruby");
 * gen.cubeAll("ruby_block");
 * gen.shaped("ruby_block", 1, List.of("###", "###", "###"), Map.of('#', Ingredient.item("mymod:ruby")));
 * gen.dropSelf("ruby_block");
 * gen.itemTag("c:gems").add("mymod:ruby");
 * gen.generate();
 * }</pre>
 *
 * Names without a namespace belong to the mod ({@code "ruby"} means {@code mymod:ruby}). The output is deterministic and
 * files whose content did not change are left untouched.
 */
public final class DataGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final String modId;
    private final Path root;
    private final Formats formats;
    /** Relative path (forward slashes) to file content. */
    private final Map<String, String> files = new TreeMap<>();
    private final Map<String, LangFile> langs = new LinkedHashMap<>();
    private final Map<String, TagFile> tags = new LinkedHashMap<>();

    private DataGenerator(String modId, Path root, Formats formats) {
        this.modId = modId;
        this.root = root;
        this.formats = formats;
    }

    public static DataGenerator create(String modId, Path outputRoot) {
        return new DataGenerator(modId, outputRoot, Formats.current());
    }

    /** For tools and tests that need a specific version's format. */
    public static DataGenerator create(String modId, Path outputRoot, Formats formats) {
        return new DataGenerator(modId, outputRoot, formats);
    }

    // ---------- ids ----------

    private String qualify(String name) {
        return name.contains(":") ? name : modId + ":" + name;
    }

    private static String pathOf(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    private static String namespaceOf(String id) {
        return id.substring(0, id.indexOf(':'));
    }

    private void put(String relativePath, JsonElement json) {
        if (files.putIfAbsent(relativePath, GSON.toJson(json) + "\n") != null) {
            throw new IllegalStateException("Two generators write the same file: " + relativePath);
        }
    }

    // ---------- models ----------

    /** {@code assets/<ns>/models/item/<name>.json} using {@code item/generated} with the texture {@code <ns>:item/<name>}. */
    public DataGenerator itemModel(String name) {
        return itemModel(name, "minecraft:item/generated");
    }

    /** Like {@link #itemModel} but with {@code item/handheld} (tools and weapons). */
    public DataGenerator handheldItemModel(String name) {
        return itemModel(name, "minecraft:item/handheld");
    }

    private DataGenerator itemModel(String name, String parent) {
        String id = qualify(name);
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", namespaceOf(id) + ":item/" + pathOf(id));
        model.add("textures", textures);
        put("assets/" + namespaceOf(id) + "/models/item/" + pathOf(id) + ".json", model);
        return this;
    }

    /**
     * A block that looks the same on every side: the block model ({@code cube_all} with {@code <ns>:block/<name>}), the block
     * state and the item model that points to the block model.
     */
    public DataGenerator cubeAll(String name) {
        String id = qualify(name);
        String ns = namespaceOf(id);
        String path = pathOf(id);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", ns + ":block/" + path);
        model.add("textures", textures);
        put("assets/" + ns + "/models/block/" + path + ".json", model);

        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", ns + ":block/" + path);
        variants.add("", variant);
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        put("assets/" + ns + "/blockstates/" + path + ".json", state);

        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", ns + ":block/" + path);
        put("assets/" + ns + "/models/item/" + path + ".json", itemModel);
        return this;
    }

    // ---------- language ----------

    /** A language file such as {@code "en_us"}; add entries to the returned object. */
    public LangFile lang(String code) {
        return langs.computeIfAbsent(code, c -> new LangFile());
    }

    /** The entries of one language file. */
    public static final class LangFile {
        private final Map<String, String> entries = new TreeMap<>();

        public LangFile add(String key, String value) {
            if (entries.putIfAbsent(key, value) != null) {
                throw new IllegalStateException("Duplicate language key: " + key);
            }
            return this;
        }
    }

    // ---------- recipes ----------

    /** A shaped crafting recipe. {@code key} maps pattern characters to ingredients. */
    public DataGenerator shaped(String result, int count, List<String> pattern, Map<Character, Ingredient> key) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        JsonArray rows = new JsonArray();
        pattern.forEach(rows::add);
        recipe.add("pattern", rows);
        JsonObject keys = new JsonObject();
        new TreeMap<>(key).forEach((c, ingredient) -> keys.add(String.valueOf(c), ingredient.toJson(this::qualify)));
        recipe.add("key", keys);
        recipe.add("result", resultOf(qualify(result), count));
        return putRecipe(result, recipe);
    }

    /** A shapeless crafting recipe. */
    public DataGenerator shapeless(String result, int count, List<Ingredient> ingredients) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        JsonArray list = new JsonArray();
        ingredients.forEach(i -> list.add(i.toJson(this::qualify)));
        recipe.add("ingredients", list);
        recipe.add("result", resultOf(qualify(result), count));
        return putRecipe(result, recipe);
    }

    /**
     * A cooking recipe.
     *
     * @param type one of {@code smelting}, {@code blasting}, {@code smoking}, {@code campfire_cooking}
     */
    public DataGenerator cooking(String type, Ingredient input, String result, float experience, int cookingTime) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:" + type);
        recipe.addProperty("category", "misc");
        recipe.add("ingredient", input.toJson(this::qualify));
        recipe.addProperty("experience", experience);
        recipe.addProperty("cookingtime", cookingTime);
        String id = qualify(result);
        if (formats.singular) {
            JsonObject r = new JsonObject();
            r.addProperty("id", id);
            recipe.add("result", r);
        } else {
            recipe.addProperty("result", id);
        }
        return putRecipe(pathOf(id) + "_from_" + type, recipe);
    }

    public DataGenerator smelting(Ingredient input, String result, float experience, int cookingTime) {
        return cooking("smelting", input, result, experience, cookingTime);
    }

    private JsonObject resultOf(String id, int count) {
        JsonObject r = new JsonObject();
        r.addProperty(formats.singular ? "id" : "item", id);
        if (count != 1) {
            r.addProperty("count", count);
        }
        return r;
    }

    private DataGenerator putRecipe(String name, JsonObject recipe) {
        String id = qualify(name);
        put("data/" + namespaceOf(id) + "/" + formats.recipeDir() + "/" + pathOf(id) + ".json", recipe);
        return this;
    }

    // ---------- loot tables ----------

    /** A block loot table that drops the block itself (and nothing if it was destroyed by an explosion). */
    public DataGenerator dropSelf(String block) {
        return dropItem(block, block, 1, 1);
    }

    /**
     * A block loot table that drops {@code item}, {@code min} to {@code max} of them.
     */
    public DataGenerator dropItem(String block, String item, int min, int max) {
        String blockId = qualify(block);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", qualify(item));
        if (min != max || min != 1) {
            JsonObject function = new JsonObject();
            function.addProperty("function", "minecraft:set_count");
            if (min == max) {
                function.addProperty("count", min);
            } else {
                JsonObject range = new JsonObject();
                range.addProperty("type", "minecraft:uniform");
                range.addProperty("min", min);
                range.addProperty("max", max);
                function.add("count", range);
            }
            JsonArray functions = new JsonArray();
            functions.add(function);
            entry.add("functions", functions);
        }
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("conditions", conditions);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        put("data/" + namespaceOf(blockId) + "/" + formats.lootDir() + "/blocks/" + pathOf(blockId) + ".json", table);
        return this;
    }

    // ---------- tags ----------

    /** The entries of one tag. */
    public static final class TagFile {
        private final Set<String> values = new LinkedHashSet<>();
        private boolean replace;

        /** Adds an entry (an id) or, with a leading {@code #}, another tag. */
        public TagFile add(String id) {
            values.add(id);
            return this;
        }

        public TagFile addTag(String tagId) {
            return add("#" + tagId);
        }

        /** Make the tag replace entries from other data packs instead of extending them. */
        public TagFile replace() {
            this.replace = true;
            return this;
        }
    }

    public TagFile itemTag(String id) {
        return tag("item", id);
    }

    public TagFile blockTag(String id) {
        return tag("block", id);
    }

    private TagFile tag(String kind, String id) {
        return tags.computeIfAbsent(kind + "|" + qualify(id), k -> new TagFile());
    }

    // ---------- output ----------

    /** All files that would be written, as {@code relative path -> content}, without touching the disk. */
    public Map<String, String> preview() {
        Map<String, String> all = new TreeMap<>(files);
        langs.forEach((code, lang) -> {
            JsonObject json = new JsonObject();
            lang.entries.forEach(json::addProperty);
            all.put("assets/" + modId + "/lang/" + code + ".json", GSON.toJson(json) + "\n");
        });
        tags.forEach((key, tag) -> {
            String kind = key.substring(0, key.indexOf('|'));
            String id = key.substring(key.indexOf('|') + 1);
            JsonObject json = new JsonObject();
            json.addProperty("replace", tag.replace);
            JsonArray values = new JsonArray();
            tag.values.forEach(v -> values.add(v.startsWith("#") ? "#" + qualify(v.substring(1)) : qualify(v)));
            json.add("values", values);
            all.put("data/" + namespaceOf(id) + "/" + formats.tagDir(kind) + "/" + pathOf(id) + ".json", GSON.toJson(json) + "\n");
        });
        return all;
    }

    /**
     * Writes every file under the output root, creating folders as needed.
     *
     * @return the relative paths of the files that were created or changed (unchanged files are left alone)
     */
    public List<String> generate() throws IOException {
        List<String> written = new ArrayList<>();
        for (Map.Entry<String, String> file : preview().entrySet()) {
            Path target = root.resolve(file.getKey());
            if (Files.exists(target) && Files.readString(target, StandardCharsets.UTF_8).equals(file.getValue())) {
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
            written.add(file.getKey());
        }
        return written;
    }
}
