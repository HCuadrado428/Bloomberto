package com.dougg865.bloomberto.api.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataGeneratorTest {
    private static JsonObject json(DataGenerator gen, String path) {
        String text = gen.preview().get(path);
        assertTrue(text != null, "missing file " + path + " in " + gen.preview().keySet());
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static DataGenerator sample(Formats formats, Path dir) {
        DataGenerator gen = DataGenerator.create("mymod", dir, formats);
        gen.lang("en_us").add("item.mymod.ruby", "Ruby").add("block.mymod.ruby_block", "Block of Ruby");
        gen.itemModel("ruby");
        gen.handheldItemModel("ruby_sword");
        gen.cubeAll("ruby_block");
        gen.shaped("ruby_block", 1, List.of("###", "###", "###"), Map.of('#', Ingredient.item("mymod:ruby")));
        gen.shapeless("ruby", 9, List.of(Ingredient.item("mymod:ruby_block")));
        gen.smelting(Ingredient.item("mymod:raw_ruby"), "ruby", 0.7f, 200);
        gen.dropSelf("ruby_block");
        gen.dropItem("ruby_ore", "ruby", 1, 3);
        gen.itemTag("c:gems").add("mymod:ruby").addTag("c:other_gems");
        gen.blockTag("mymod:mineable").add("mymod:ruby_block");
        return gen;
    }

    @Test
    void modelsAndBlockStatesMatchVanillaLayout(@TempDir Path dir) {
        DataGenerator gen = sample(Formats.V1_21_1, dir);
        JsonObject item = json(gen, "assets/mymod/models/item/ruby.json");
        assertEquals("minecraft:item/generated", item.get("parent").getAsString());
        assertEquals("mymod:item/ruby", item.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals("minecraft:item/handheld", json(gen, "assets/mymod/models/item/ruby_sword.json").get("parent").getAsString());
        JsonObject block = json(gen, "assets/mymod/models/block/ruby_block.json");
        assertEquals("minecraft:block/cube_all", block.get("parent").getAsString());
        assertEquals("mymod:block/ruby_block", block.getAsJsonObject("textures").get("all").getAsString());
        assertEquals("mymod:block/ruby_block", json(gen, "assets/mymod/blockstates/ruby_block.json")
                .getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        assertEquals("mymod:block/ruby_block", json(gen, "assets/mymod/models/item/ruby_block.json").get("parent").getAsString());
    }

    @Test
    void recipesInTheOldFormat(@TempDir Path dir) {
        DataGenerator gen = sample(Formats.V1_20_1, dir);
        JsonObject shaped = json(gen, "data/mymod/recipes/ruby_block.json");
        assertEquals("minecraft:crafting_shaped", shaped.get("type").getAsString());
        assertEquals("mymod:ruby_block", shaped.getAsJsonObject("result").get("item").getAsString());
        assertFalse(shaped.getAsJsonObject("result").has("count"), "count 1 is omitted, like vanilla");
        assertEquals("mymod:ruby", shaped.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals(3, shaped.getAsJsonArray("pattern").size());
        JsonObject shapeless = json(gen, "data/mymod/recipes/ruby.json");
        assertEquals(9, shapeless.getAsJsonObject("result").get("count").getAsInt());
        JsonObject smelting = json(gen, "data/mymod/recipes/ruby_from_smelting.json");
        assertEquals("mymod:ruby", smelting.get("result").getAsString(), "smelting results are plain strings in 1.20.1");
        assertEquals(200, smelting.get("cookingtime").getAsInt());
        assertEquals("minecraft:smelting", smelting.get("type").getAsString());
    }

    @Test
    void recipesInTheNewFormat(@TempDir Path dir) {
        DataGenerator gen = sample(Formats.V1_21_1, dir);
        JsonObject shaped = json(gen, "data/mymod/recipe/ruby_block.json");
        assertEquals("mymod:ruby_block", shaped.getAsJsonObject("result").get("id").getAsString());
        JsonObject smelting = json(gen, "data/mymod/recipe/ruby_from_smelting.json");
        assertEquals("mymod:ruby", smelting.getAsJsonObject("result").get("id").getAsString(), "smelting results are objects in 1.21");
        assertFalse(gen.preview().containsKey("data/mymod/recipes/ruby_block.json"));
    }

    @Test
    void lootTablesUseTheVersionsFolder(@TempDir Path dir) {
        JsonObject old = json(sample(Formats.V1_20_1, dir), "data/mymod/loot_tables/blocks/ruby_block.json");
        JsonObject now = json(sample(Formats.V1_21_1, dir), "data/mymod/loot_table/blocks/ruby_block.json");
        for (JsonObject table : List.of(old, now)) {
            assertEquals("minecraft:block", table.get("type").getAsString());
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            assertEquals("minecraft:survives_explosion",
                    pool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString());
            assertEquals("mymod:ruby_block", pool.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        }
    }

    @Test
    void dropItemWithARange(@TempDir Path dir) {
        JsonObject table = json(sample(Formats.V1_21_1, dir), "data/mymod/loot_table/blocks/ruby_ore.json");
        JsonObject entry = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("mymod:ruby", entry.get("name").getAsString());
        JsonObject count = entry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals(1, count.get("min").getAsInt());
        assertEquals(3, count.get("max").getAsInt());
        assertEquals("minecraft:uniform", count.get("type").getAsString());
    }

    @Test
    void tagsUseTheVersionsFolderAndKeepOtherNamespaces(@TempDir Path dir) {
        JsonObject old = json(sample(Formats.V1_20_1, dir), "data/c/tags/items/gems.json");
        JsonObject now = json(sample(Formats.V1_21_1, dir), "data/c/tags/item/gems.json");
        for (JsonObject tag : List.of(old, now)) {
            assertFalse(tag.get("replace").getAsBoolean());
            assertEquals("mymod:ruby", tag.getAsJsonArray("values").get(0).getAsString());
            assertEquals("#c:other_gems", tag.getAsJsonArray("values").get(1).getAsString());
        }
        assertTrue(sample(Formats.V1_20_1, dir).preview().containsKey("data/mymod/tags/blocks/mineable.json"));
        assertTrue(sample(Formats.V1_21_1, dir).preview().containsKey("data/mymod/tags/block/mineable.json"));
    }

    @Test
    void languageFileIsSortedByKey(@TempDir Path dir) {
        String text = sample(Formats.V1_21_1, dir).preview().get("assets/mymod/lang/en_us.json");
        assertTrue(text.indexOf("block.mymod.ruby_block") < text.indexOf("item.mymod.ruby"));
        JsonObject lang = JsonParser.parseString(text).getAsJsonObject();
        assertEquals("Ruby", lang.get("item.mymod.ruby").getAsString());
    }

    @Test
    void generateWritesFilesAndSkipsUnchangedOnes(@TempDir Path dir) throws IOException {
        List<String> first = sample(Formats.V1_21_1, dir).generate();
        assertTrue(first.contains("assets/mymod/models/item/ruby.json"));
        assertTrue(Files.exists(dir.resolve("data/mymod/recipe/ruby_block.json")));
        assertEquals(first.size(), sample(Formats.V1_21_1, dir).preview().size());
        assertEquals(List.of(), sample(Formats.V1_21_1, dir).generate(), "second run changes nothing");
        Files.writeString(dir.resolve("assets/mymod/models/item/ruby.json"), "{}");
        assertEquals(List.of("assets/mymod/models/item/ruby.json"), sample(Formats.V1_21_1, dir).generate());
    }

    @Test
    void outputIsDeterministic(@TempDir Path dir) {
        assertEquals(sample(Formats.V1_21_1, dir).preview(), sample(Formats.V1_21_1, dir).preview());
    }

    @Test
    void duplicateOutputsAreRejected(@TempDir Path dir) {
        DataGenerator gen = DataGenerator.create("mymod", dir, Formats.V1_21_1);
        gen.itemModel("a");
        assertThrows(IllegalStateException.class, () -> gen.itemModel("a"));
        gen.lang("en_us").add("k", "v");
        assertThrows(IllegalStateException.class, () -> gen.lang("en_us").add("k", "w"));
    }

    @Test
    void namesWithoutANamespaceBelongToTheMod(@TempDir Path dir) {
        DataGenerator gen = DataGenerator.create("mymod", dir, Formats.V1_21_1);
        gen.shaped("block", 1, List.of("#"), Map.of('#', Ingredient.item("gem")));
        gen.shapeless("gem", 1, List.of(Ingredient.tag("gems"), Ingredient.item("minecraft:stick")));
        gen.smelting(Ingredient.item("raw"), "gem", 0f, 100);
        gen.itemTag("gems").add("gem").addTag("c:more").add("other:thing");
        assertEquals("mymod:gem", json(gen, "data/mymod/recipe/block.json").getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        var ingredients = json(gen, "data/mymod/recipe/gem.json").getAsJsonArray("ingredients");
        assertEquals("mymod:gems", ingredients.get(0).getAsJsonObject().get("tag").getAsString());
        assertEquals("minecraft:stick", ingredients.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("mymod:raw", json(gen, "data/mymod/recipe/gem_from_smelting.json").getAsJsonObject("ingredient").get("item").getAsString());
        var values = json(gen, "data/mymod/tags/item/gems.json").getAsJsonArray("values");
        assertEquals("mymod:gem", values.get(0).getAsString());
        assertEquals("#c:more", values.get(1).getAsString());
        assertEquals("other:thing", values.get(2).getAsString());
    }
}
