package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class AlloyFurnaceRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    public static void loadFromJSON()
    {
        File configDir = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes");
        File jsonFile = new File(configDir, "alloy_furnace.json");

        try
        {
            JsonArray array = RecipeFiles.read(jsonFile, RECIPE_VERSION, AlloyFurnaceRecipeLoader::createDefaultFile);

            for (JsonElement el : array)
            {
                try
                {
                    JsonObject obj = el.getAsJsonObject();

                    if (!obj.has("input1"))
                    {
                        RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no input1: {}", obj);
                        continue;
                    }

                    if (!obj.has("input2"))
                    {
                        RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no input2: {}", obj);
                        continue;
                    }

                    if (!obj.has("result"))
                    {
                        RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no result: {}", obj);
                        continue;
                    }

                    ItemStack input1Stack = null;
                    String oreDictInput1 = null;
                    int quantity1 = 1;

                    if (obj.has("input1"))
                    {
                        JsonObject input1 = obj.getAsJsonObject("input1");
                        if (input1.has("item"))
                        {
                            String itemName = input1.get("item").getAsString();
                            int meta = getMetadata(input1);
                            int count = input1.has("count") ? input1.get("count").getAsInt() : 1;

                            Item item = Item.getByNameOrId(itemName);
                            if (item == null)
                            {
                                RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no item for input1: {}", itemName);
                                continue;
                            }
                            input1Stack = new ItemStack(item, count, meta);
                            quantity1 = count;
                        }
                        else if (input1.has("oreDict"))
                        {
                            oreDictInput1 = RecipeFiles.oreDictionaryName(input1);
                            quantity1 = input1.has("count") ? input1.get("count").getAsInt() : 1;
                        }
                        else
                        {
                            RecipeFiles.error(jsonFile, "AlloyFurnace input1 has neither item nor oreDict: {}", input1);
                            continue;
                        }
                    }

                    ItemStack input2Stack = null;
                    String oreDictInput2 = null;
                    int quantity2 = 1;

                    if (obj.has("input2"))
                    {
                        JsonObject input2 = obj.getAsJsonObject("input2");
                        if (input2.has("item"))
                        {
                            String itemName = input2.get("item").getAsString();
                            int meta = getMetadata(input2);
                            int count = input2.has("count") ? input2.get("count").getAsInt() : 1;

                            Item item = Item.getByNameOrId(itemName);
                            if (item == null)
                            {
                                RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no item for input2: {}", itemName);
                                continue;
                            }
                            input2Stack = new ItemStack(item, count, meta);
                            quantity2 = count;
                        }
                        else if (input2.has("oreDict"))
                        {
                            oreDictInput2 = RecipeFiles.oreDictionaryName(input2);
                            quantity2 = input2.has("count") ? input2.get("count").getAsInt() : 1;
                        }
                        else
                        {
                            RecipeFiles.error(jsonFile, "AlloyFurnace input2 has neither item nor oreDict: {}", input2);
                            continue;
                        }
                    }

                    JsonObject resultObj = obj.getAsJsonObject("result");
                    String resultItemName = resultObj.get("item").getAsString();
                    int resultMeta = resultObj.has("meta") ? resultObj.get("meta").getAsInt() : 0;
                    int resultCount = resultObj.has("count") ? resultObj.get("count").getAsInt() : 1;

                    Item resultItem = Item.getByNameOrId(resultItemName);
                    if (resultItem == null)
                    {
                        RecipeFiles.error(jsonFile, "Alloy Furnace recipe has no item for result: {}", resultItemName);
                        continue;
                    }

                    ItemStack resultStack = new ItemStack(resultItem, resultCount, resultMeta);

                    int cookTime = obj.get("cookTime").getAsInt();
                    int energy = obj.get("energyUsage").getAsInt();
                    float xp = obj.get("experience").getAsFloat();

                    AlloyFurnaceRecipe.getInstance().addRecipe(input1Stack, input2Stack, oreDictInput1, oreDictInput2, quantity1, quantity2, resultStack, cookTime, energy, xp);
                }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(jsonFile, "Skipping invalid AlloyFurnace recipe: {}", el, error);
                }
            }

            MinersProsperity.logger.info("Loaded {} Alloy Furnace recipes", AlloyFurnaceRecipe.getInstance().getLookupTable().getRecipes().size());
        }
        catch (Exception e)
        {
            RecipeFiles.error(jsonFile, "Error while loading alloy furnace recipes from JSON", e);
        }
    }

    private static void createDefaultFile(File file) throws IOException
    {
        File configDir = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes");
        if (!configDir.exists() && !configDir.mkdirs())
        {
            throw new IOException("Could not create config directory: " + configDir.getAbsolutePath());
        }

        JsonArray defaultRecipes = getJsonElements();

        String exampleComment =
                """
                        // Alloy Furnace - alloy_furnace.json
                        //
                        // FILE FORMAT
                        // The root object contains "version" and the "recipes" array: {"version": "1.0", "recipes": [...]}.
                        // Inside "recipes", separate recipe objects with commas; do not add a trailing comma after the last one.
                        // Use double-quoted keys/strings and JSON numbers (decimal point, not comma).
                        // An empty "recipes": [] disables this file's recipes. These // comments are supported.
                        // Older versions (including unversioned arrays) are backed up before new defaults are installed.
                        // Keep "version" unchanged when editing recipes; change the array, not these examples.
                        // Restart Minecraft to apply changes. Newer-version or malformed files are not replaced.
                        // Item/fluid IDs and Ore Dictionary names must exist and use their registered spelling.
                        // Avoid duplicate or overlapping inputs: recipes are alternatives, not a sequence of steps.
                        //
                        // RECIPE FIELDS
                        // Required: input1, input2, result, cookTime, energyUsage, experience.
                        // "input1" and "input2" are separate input objects; both are consumed for one operation.
                        // Each input may independently use an item ID or an Ore Dictionary name.
                        //
                        // INPUT FIELDS
                        // Use exactly one of "item": "namespace:item_id" or "oreDict": "registeredOreName".
                        // "count": integer items consumed per operation; optional, default 1.
                        // "meta": item metadata integer; optional, default 0. Use "*" to match any input metadata.
                        // Metadata applies only to item inputs; omit it for oreDict inputs.
                        // An oreDict input accepts any registered matching item; it does not mix different items in one slot.
                        // Input counts must be positive and fit the input item's slot/stack limit.
                        // NBT predicates and custom NBT output data are not supported by this format.
                        //
                        // OUTPUT FIELDS
                        // "result": required object with "item": "namespace:item_id".
                        // Its "count" is optional (default 1); "meta" is optional (integer, default 0).
                        // Use a positive output count no larger than the output item's stack limit.
                        // The result is deterministic; oreDict, wildcard metadata, chance and counts are not output options.
                        // Random quantities/byproducts are supported only by Rock Crusher's outputs format.
                        //
                        // PROCESS FIELDS
                        // "cookTime": required positive integer duration in ticks; 20 ticks = 1 second.
                        // "energyUsage": required positive integer FE consumed per tick.
                        // "experience": required non-negative XP number per processed input set; decimals are allowed.
                        // These are base values before upgrades; XP is stored in the machine's experience tank.
                        // For example, 200 ticks at 20 FE/t means 10 seconds and 4000 FE before upgrades.
                        // Counts/time/energy are whole numbers; chances and XP may be fractional where supported.
                        //
                        // EXAMPLES
                        // The examples cover concrete item inputs, Ore Dictionary inputs, mixed input types,
                        // explicit input/output metadata, wildcard input metadata and explicit/omitted counts.
                        // They demonstrate syntax; choose the ingredients and balance appropriate to your pack.
                        //
                        // {
                        //   "version": "1.0",
                        //   "recipes": [
                        //     {
                        //       "input1": {
                        //         "item": "minecraft:iron_ingot",
                        //         "meta": 0,
                        //         "count": 1
                        //       },
                        //       "input2": {
                        //         "item": "minecraft:coal",
                        //         "meta": 0,
                        //         "count": 2
                        //       },
                        //       "result": {
                        //         "item": "minersprosperity:steel_ingot",
                        //         "meta": 0,
                        //         "count": 1
                        //       },
                        //       "cookTime": 200,
                        //       "energyUsage": 20,
                        //       "experience": 0.7
                        //     },
                        //     {
                        //       "input1": {
                        //         "oreDict": "ingotCopper",
                        //         "count": 3
                        //       },
                        //       "input2": {
                        //         "oreDict": "ingotTin"
                        //       },
                        //       "result": {
                        //         "item": "minersprosperity:bronze_ingot",
                        //         "count": 4
                        //       },
                        //       "cookTime": 200,
                        //       "energyUsage": 20,
                        //       "experience": 0.7
                        //     },
                        //     {
                        //       "input1": {
                        //         "item": "minecraft:coal",
                        //         "meta": "*"
                        //       },
                        //       "input2": {
                        //         "oreDict": "ingotIron"
                        //       },
                        //       "result": {
                        //         "item": "minersprosperity:steel_ingot"
                        //       },
                        //       "cookTime": 200,
                        //       "energyUsage": 20,
                        //       "experience": 0.7
                        //     }
                        //   ]
                        // }
                        //
                        //
                        """;

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            writer.write(exampleComment);
            new GsonBuilder().setPrettyPrinting().create().toJson(RecipeFiles.document(RECIPE_VERSION, defaultRecipes), writer);
        }
    }

    private static @NotNull JsonArray getJsonElements()
    {
        JsonArray defaultRecipes = new JsonArray();

        defaultRecipes.add(createRecipe(null, null, "ingotCopper", null, null, "ingotTin", 3, 1, "minersprosperity:bronze_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCopper", null, null, "ingotZinc", 2, 1, "minersprosperity:brass_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCopper", null, null, "ingotAluminum", 1, 3, "minersprosperity:duraluminum_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIron", null, null, "gemCoal", 1, 1, "minersprosperity:steel_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotNickel", null, null, "ingotChromium", 2, 1, "minersprosperity:inconel_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotGold", null, null, "ingotPlatinum", 2, 1, "minersprosperity:white_gold_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotGold", null, null, "ingotSilver", 1, 1, "minersprosperity:electrum_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemAmethyst", null, null, "ingotRefinedObsidian", 1, 2, "minersprosperity:obsidian_cluster", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobalt", null, null, "ingotSteel", 1, 2, "minersprosperity:cobaltsteel_ingot", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSteel", null, null, "gemOnyx", 1, 1, "minersprosperity:dark_steel_ingot", null, 1, 200, 20, 0.7f));

        return defaultRecipes;
    }

    private static JsonObject createRecipe(String input1Item, Object input1Meta, String input1OreDict, String input2Item, Object input2Meta, String input2OreDict, int quantity1, int quantity2, String resultItem, Integer resultMeta, int resultCount, int cookTime, int energyUsage, float experience)
    {
        JsonObject recipe = new JsonObject();

        JsonObject input1 = new JsonObject();
        if (input1Item != null)
        {
            input1.addProperty("item", input1Item);
            addMeta(input1, input1Meta);
        }
        else if (input1OreDict != null) input1.addProperty("oreDict", input1OreDict);
        input1.addProperty("count", quantity1);

        JsonObject input2 = new JsonObject();
        if (input2Item != null)
        {
            input2.addProperty("item", input2Item);
            addMeta(input2, input2Meta);
        }
        else if (input2OreDict != null) input2.addProperty("oreDict", input2OreDict);
        input2.addProperty("count", quantity2);

        recipe.add("input1", input1);
        recipe.add("input2", input2);

        JsonObject result = new JsonObject();
        result.addProperty("item", resultItem);
        if (resultMeta != null) result.addProperty("meta", resultMeta);
        result.addProperty("count", resultCount);
        recipe.add("result", result);

        recipe.addProperty("cookTime", cookTime);
        recipe.addProperty("energyUsage", energyUsage);
        recipe.addProperty("experience", experience);

        return recipe;
    }

    private static void addMeta(JsonObject object, Object meta)
    {
        if (meta == null) return;

        if (meta instanceof Number) object.addProperty("meta", (Number) meta);
        else if (meta instanceof String && meta.equals("*")) object.addProperty("meta", "*");
        else throw new IllegalArgumentException("Alloy Furnace recipe has invalid metadata: " + meta);
    }

    private static int getMetadata(JsonObject object)
    {
        if (!object.has("meta")) return 0;

        JsonElement element = object.get("meta");

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
        {
            String value = element.getAsString().trim();

            if (value.equals("*")) return OreDictionary.WILDCARD_VALUE;
        }

        return element.getAsInt();
    }
}