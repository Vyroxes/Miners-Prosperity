package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;

public final class RockCrusherRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    private RockCrusherRecipeLoader()
    {
    }

    public static void loadFromJSON()
    {
        File file = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes/rock_crusher.json");
        try
        {
            JsonArray entries = RecipeFiles.read(file, RECIPE_VERSION, RockCrusherRecipeLoader::createDefaultFile);
            RockCrusherRecipe.getInstance().clear();
            for (int i = 0; i < entries.size(); i++)
            {
                try { RockCrusherRecipe.getInstance().addRecipe(parseRecipe(entries.get(i).getAsJsonObject())); }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(file, "Invalid Rock Crusher recipe at index {}: {}", i, error.getMessage());
                }
            }
            MinersProsperity.logger.info("Loaded {} Rock Crusher recipes", RockCrusherRecipe.getInstance().getRecipes().size());
        }
        catch (IOException | RuntimeException error)
        {
            RecipeFiles.error(file, "Could not load Rock Crusher recipes", error);
        }
    }

    private static void createDefaultFile(File file) throws IOException
    {
        Files.createDirectories(file.toPath().getParent());
            String exampleComment =
                    """
                    // Rock Crusher - rock_crusher.json
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
                    // Required: input, outputs, cookTime, energyUsage, experience.
                    // "input" is one input object; its count is consumed for one operation.
                    // "outputs" is an array of 1-3 output objects, mapped to output slots from left to right.
                    // Unused slots are omitted; each defined output must have a chance of producing items.
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
                    // Each output requires "item": "namespace:item_id"; optional integer "meta" defaults to 0.
                    // Output oreDict, wildcard metadata and custom NBT are not supported.
                    // Choose one of the following two formats independently for each output:
                    // A) "count": positive integer (default 1), "chance": percentage (default 100).
                    //    A chance of 20 means that exact quantity has a 20% chance; otherwise 0 items (80%).
                    // B) "counts": [{"count": 0, "chance": 65}, {"count": 1, "chance": 30}, {"count": 2, "chance": 5}].
                    //    Each entry is one mutually exclusive quantity and its percentage, not an extra roll.
                    //    All percentages must total 100 (tolerance 0.000001); include zero explicitly when needed.
                    //    Counts must be unique integers from 0 to the item's stack limit (usually 64).
                    //    Quantities such as 3, 5 or more are supported; at least one count must be positive.
                    //    Every chance must be finite, greater than 0 and at most 100; decimals are supported.
                    // Do not combine "counts" with "count" or "chance" in the same output.
                    // Output slots roll independently; chances across different slots do not need to total 100.
                    // Input count must be an integer from 1 to 64 and must fit the input's actual stack limit.
                    //
                    // PROCESS FIELDS
                    // "cookTime": required positive integer duration in ticks; 20 ticks = 1 second.
                    // "energyUsage": required positive integer FE consumed per tick.
                    // "experience": required non-negative XP number per processed input set; decimals are allowed.
                    // These are base values before upgrades; XP is stored in the machine's experience tank.
                    // For example, 200 ticks at 20 FE/t means 10 seconds and 4000 FE before upgrades.
                    // Counts/time/energy are whole numbers; chances and XP may be fractional where supported.
                    //
                    // PROCESSING AND SPACE
                    // Quantities are rolled only when processing finishes, separately for each processed input set.
                    // Normal output mode requires room for the maximum possible quantity in every output slot.
                    // For example, a stack of 63 prevents processing if that slot can produce 2, even at a low chance.
                    // Configured Void Excess/Void All output modes may discard items instead of requiring that room.
                    // Invalid recipe entries are logged and skipped; inspect the log if a recipe is missing.
                    //
                    // EXAMPLES
                    // Example 1 mixes guaranteed output, a 0/1/2 distribution and a fractional byproduct chance.
                    // Example 2 uses Ore Dictionary input and a 0/3/5 distribution (65%/30%/5%).
                    // Example 3 uses wildcard input metadata, a single output with metadata and default count/chance.
                    // Add/remove output objects for 1, 2 or 3 outputs without changing the recipe field names.
                    //
                    // {
                    //   "version": "1.0",
                    //   "recipes": [
                    //     {
                    //       "input": {
                    //         "item": "minecraft:iron_ore",
                    //         "meta": 0,
                    //         "count": 1
                    //       },
                    //       "outputs": [
                    //         {
                    //           "item": "minersprosperity:iron_chunk",
                    //           "count": 2,
                    //           "chance": 100
                    //         },
                    //         {
                    //           "item": "minersprosperity:manganese_dust",
                    //           "counts": [
                    //             {
                    //               "count": 0,
                    //               "chance": 65
                    //             },
                    //             {
                    //               "count": 1,
                    //               "chance": 30
                    //             },
                    //             {
                    //               "count": 2,
                    //               "chance": 5
                    //             }
                    //           ]
                    //         },
                    //         {
                    //           "item": "minersprosperity:stone_dust",
                    //           "count": 1,
                    //           "chance": 12.5
                    //         }
                    //       ],
                    //       "cookTime": 200,
                    //       "energyUsage": 20,
                    //       "experience": 0.7
                    //     },
                    //     {
                    //       "input": {
                    //         "oreDict": "oreCopper"
                    //       },
                    //       "outputs": [
                    //         {
                    //           "item": "minersprosperity:copper_chunk",
                    //           "count": 2
                    //         },
                    //         {
                    //           "item": "minersprosperity:stone_dust",
                    //           "counts": [
                    //             {
                    //               "count": 0,
                    //               "chance": 65
                    //             },
                    //             {
                    //               "count": 3,
                    //               "chance": 30
                    //             },
                    //             {
                    //               "count": 5,
                    //               "chance": 5
                    //             }
                    //           ]
                    //         }
                    //       ],
                    //       "cookTime": 200,
                    //       "energyUsage": 20,
                    //       "experience": 0.7
                    //     },
                    //     {
                    //       "input": {
                    //         "item": "minecraft:stone",
                    //         "meta": "*"
                    //       },
                    //       "outputs": [
                    //         {
                    //           "item": "minecraft:sand",
                    //           "meta": 0
                    //         }
                    //       ],
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
                new GsonBuilder().setPrettyPrinting().create().toJson(RecipeFiles.document(RECIPE_VERSION, getJsonElements()), writer);
            }
    }

    static JsonArray getJsonElements()
    {
        JsonArray recipes = new JsonArray();
        recipes.add(createRecipe("oreDict", "oreHalite", null, 1, 300, 30, 0.5, output("minersprosperity:salt_chunk", 2, 100), distributedOutput("minersprosperity:lithium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreSulfur", null, 1, 300, 30, 0.5, output("minersprosperity:sulfur_chunk", 2, 100), distributedOutput("minersprosperity:boron_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:flint_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreGraphite", null, 1, 300, 30, 0.5, output("minersprosperity:graphite_chunk", 2, 100), distributedOutput("minersprosperity:carbon_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:coal_tiny_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreCopper", null, 1, 300, 30, 0.5, output("minersprosperity:copper_chunk", 2, 100), output("minersprosperity:malachite", 1, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreTin", null, 1, 300, 30, 0.5, output("minersprosperity:tin_chunk", 2, 100), distributedOutput("minersprosperity:zirconium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreSilver", null, 1, 300, 30, 0.5, output("minersprosperity:silver_chunk", 2, 100), distributedOutput("minersprosperity:gold_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:garnet", 1, 3)));
        recipes.add(createRecipe("oreDict", "oreLead", null, 1, 300, 30, 0.5, output("minersprosperity:lead_chunk", 2, 100), distributedOutput("minersprosperity:silver_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:thorium_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreZinc", null, 1, 300, 30, 0.5, output("minersprosperity:zinc_chunk", 2, 100), distributedOutput("minersprosperity:magnesium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreAluminum", null, 1, 300, 30, 0.5, output("minersprosperity:aluminum_chunk", 2, 100), distributedOutput("minersprosperity:magnesium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:beryllium_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreNickel", null, 1, 300, 30, 0.5, output("minersprosperity:nickel_chunk", 2, 100), distributedOutput("minersprosperity:cobalt_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:molybdenum_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreChromium", null, 1, 300, 30, 0.5, output("minersprosperity:chromium_chunk", 2, 100), distributedOutput("minersprosperity:vanadium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreUranium", null, 1, 300, 30, 0.5, output("minersprosperity:uranium_chunk", 2, 100), distributedOutput("minersprosperity:thorium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:lead_tiny_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreTungsten", null, 1, 300, 30, 0.5, output("minersprosperity:tungsten_chunk", 2, 100), distributedOutput("minersprosperity:molybdenum_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "orePlatinum", null, 1, 300, 30, 0.5, output("minersprosperity:platinum_chunk", 2, 100), distributedOutput("minersprosperity:iridium_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:nickel_tiny_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreIridium", null, 1, 300, 30, 0.5, output("minersprosperity:iridium_chunk", 2, 100), distributedOutput("minersprosperity:platinum_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:ender_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreTitanium", null, 1, 300, 30, 0.5, output("minersprosperity:titanium_chunk", 2, 100), distributedOutput("minersprosperity:zirconium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:vanadium_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreRuby", null, 1, 300, 30, 0.5, output("minersprosperity:ruby_chunk", 2, 100), output("minersprosperity:garnet", 1, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreSapphire", null, 1, 300, 30, 0.5, output("minersprosperity:sapphire_chunk", 2, 100), output("minersprosperity:tanzanite", 1, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreAmethyst", null, 1, 300, 30, 0.5, output("minersprosperity:amethyst_chunk", 2, 100), output("minersprosperity:garnet", 1, 5), output("minersprosperity:quartz_tiny_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreTopaz", null, 1, 300, 30, 0.5, output("minersprosperity:topaz_chunk", 2, 100), output("minersprosperity:amber", 1, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "orePeridot", null, 1, 300, 30, 0.5, output("minersprosperity:peridot_chunk", 2, 100), output("minersprosperity:malachite", 1, 5), output("minersprosperity:magnesium_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreCobalt", null, 1, 300, 30, 0.5, output("minersprosperity:cobalt_chunk", 2, 100), distributedOutput("minersprosperity:nickel_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:manganese_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreOnyx", null, 1, 300, 30, 0.5, output("minersprosperity:onyx_chunk", 2, 100), distributedOutput("minersprosperity:obsidian_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:flint_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreBlackOpal", null, 1, 300, 30, 0.5, output("minersprosperity:black_opal_chunk", 2, 100), distributedOutput("minersprosperity:obsidian_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:ender_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreAzure", null, 1, 300, 30, 0.5, output("minersprosperity:azure_chunk", 2, 100), distributedOutput("minersprosperity:netherrack_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:glowstone_tiny_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreRosalite", null, 1, 300, 30, 0.5, output("minersprosperity:rosalite_chunk", 2, 100), distributedOutput("minersprosperity:netherrack_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:garnet", 1, 3)));
        recipes.add(createRecipe("oreDict", "oreVerdite", null, 1, 300, 30, 0.5, output("minersprosperity:verdite_chunk", 2, 100), distributedOutput("minersprosperity:netherrack_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:vanadium_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreVoidium", null, 1, 300, 30, 0.5, output("minersprosperity:voidium_chunk", 2, 100), distributedOutput("minersprosperity:end_stone_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:ender_dust", 1, 20)));
        recipes.add(createRecipe("oreDict", "oreLuminosite", null, 1, 300, 30, 0.5, output("minersprosperity:luminosite_chunk", 2, 100), distributedOutput("minersprosperity:end_stone_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:glowstone_tiny_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:coal_ore", null, 1, 300, 30, 0.5, output("minersprosperity:coal_chunk", 2, 100), output("minersprosperity:amber", 1, 5), output("minersprosperity:carbon_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:iron_ore", null, 1, 300, 30, 0.5, output("minersprosperity:iron_chunk", 2, 100), distributedOutput("minersprosperity:manganese_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:gold_ore", null, 1, 300, 30, 0.5, output("minersprosperity:gold_chunk", 2, 100), distributedOutput("minersprosperity:silver_tiny_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:amber", 1, 3)));
        recipes.add(createRecipe("item", "minecraft:diamond_ore", null, 1, 300, 30, 0.5, output("minersprosperity:diamond_chunk", 2, 100), output("minersprosperity:garnet", 1, 5), output("minersprosperity:carbon_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:emerald_ore", null, 1, 300, 30, 0.5, output("minersprosperity:emerald_chunk", 2, 100), distributedOutput("minersprosperity:beryllium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:malachite", 1, 3)));
        recipes.add(createRecipe("item", "minecraft:quartz_ore", null, 1, 300, 30, 0.5, output("minersprosperity:quartz_chunk", 2, 100), distributedOutput("minersprosperity:netherrack_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:glowstone_tiny_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:lapis_ore", null, 1, 300, 30, 0.5, output("minersprosperity:lapis_chunk", 2, 100), distributedOutput("minersprosperity:boron_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:stone_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:redstone_ore", null, 1, 300, 30, 0.5, output("minersprosperity:redstone_chunk", 2, 100), distributedOutput("minersprosperity:lithium_dust", 0, 65, 1, 30, 2, 5), output("minersprosperity:flint_dust", 1, 20)));
        recipes.add(createRecipe("item", "minecraft:stone", 0, 1, 200, 20, 0.1, output("minecraft:gravel", 1, 100), output("minersprosperity:stone_dust", 1, 30), output("minersprosperity:garnet", 1, 0.5)));
        recipes.add(createRecipe("item", "minecraft:cobblestone", null, 1, 200, 20, 0.1, output("minecraft:gravel", 1, 100), output("minersprosperity:stone_dust", 1, 25), output("minecraft:flint", 1, 5)));
        recipes.add(createRecipe("item", "minecraft:gravel", null, 1, 200, 20, 0.1, output("minecraft:sand", 1, 100), output("minecraft:flint", 1, 20), output("minersprosperity:amber_tiny_dust", 1, 1)));
        recipes.add(createRecipe("item", "minecraft:sand", "*", 1, 200, 20, 0.1, output("minersprosperity:glass_dust", 1, 100), output("minersprosperity:salt_tiny_dust", 1, 5), output("minersprosperity:quartz_tiny_dust", 1, 2)));
        recipes.add(createRecipe("item", "minecraft:stone", 1, 1, 250, 20, 0.1, output("minersprosperity:stone_dust", 1, 100), output("minersprosperity:garnet", 1, 1), output("minersprosperity:quartz_dust", 1, 10)));
        recipes.add(createRecipe("item", "minecraft:stone", 3, 1, 250, 20, 0.1, output("minersprosperity:stone_dust", 1, 100), output("minersprosperity:tanzanite", 1, 1), output("minersprosperity:quartz_dust", 1, 10)));
        recipes.add(createRecipe("item", "minecraft:stone", 5, 1, 250, 20, 0.1, output("minersprosperity:stone_dust", 1, 100), output("minersprosperity:malachite", 1, 1), output("minersprosperity:flint_dust", 1, 10)));
        recipes.add(createRecipe("item", "minecraft:netherrack", null, 1, 200, 20, 0.1, output("minersprosperity:netherrack_dust", 1, 100), output("minersprosperity:sulfur_tiny_dust", 1, 15), output("minersprosperity:glowstone_tiny_dust", 1, 2)));
        recipes.add(createRecipe("item", "minecraft:end_stone", null, 1, 250, 25, 0.2, output("minersprosperity:end_stone_dust", 1, 100), output("minersprosperity:ender_dust", 1, 3), output("minersprosperity:lithium_tiny_dust", 1, 5)));
        recipes.add(createRecipe("item", "minecraft:obsidian", null, 1, 400, 30, 0.2, output("minersprosperity:obsidian_dust", 1, 100), output("minersprosperity:ender_tiny_dust", 1, 2), output("minersprosperity:glass_tiny_dust", 1, 10)));
        return recipes;
    }

    private static JsonObject createRecipe(String inputType, String inputName, Object meta, int count,
                                           int cookTime, int energyUsage, double experience, JsonObject... outputs)
    {
        JsonObject input = new JsonObject();
        input.addProperty(inputType, inputName);
        if (meta instanceof Number) input.addProperty("meta", (Number) meta);
        else if (meta instanceof String) input.addProperty("meta", (String) meta);
        input.addProperty("count", count);
        JsonArray result = new JsonArray();
        for (JsonObject output : outputs) result.add(output);
        JsonObject recipe = new JsonObject();
        recipe.add("input", input);
        recipe.add("outputs", result);
        recipe.addProperty("cookTime", cookTime);
        recipe.addProperty("energyUsage", energyUsage);
        recipe.addProperty("experience", experience);
        return recipe;
    }

    private static JsonObject output(String item, int count, double chance)
    {
        JsonObject output = new JsonObject();
        output.addProperty("item", item);
        output.addProperty("count", count);
        output.addProperty("chance", chance);
        return output;
    }

    private static JsonObject distributedOutput(String item, int... quantitiesAndChances)
    {
        JsonObject output = new JsonObject();
        output.addProperty("item", item);
        JsonArray counts = new JsonArray();
        for (int i = 0; i < quantitiesAndChances.length; i += 2)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("count", quantitiesAndChances[i]);
            entry.addProperty("chance", quantitiesAndChances[i + 1]);
            counts.add(entry);
        }
        output.add("counts", counts);
        return output;
    }

    public static RockCrusherRecipe.Recipe parseRecipe(JsonObject json)
    {
        JsonObject input = json.getAsJsonObject("input");
        if (input.has("item") == input.has("oreDict")) throw new IllegalArgumentException("Specify one input: item or oreDict");
        ItemStack stack = input.has("item") ? item(input, true) : ItemStack.EMPTY;
        String ore = input.has("oreDict") ? RecipeFiles.oreDictionaryName(input) : null;
        int quantity = input.has("count") ? integer(input, "count") : 1;
        List<RockCrusherRecipe.Output> outputs = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("outputs"))
        {
            JsonObject output = element.getAsJsonObject();
            List<Integer> counts = new ArrayList<>();
            List<Double> chances = new ArrayList<>();
            if (output.has("counts"))
            {
                if (output.has("count") || output.has("chance")) throw new IllegalArgumentException("Use counts or count/chance, not both");
                for (JsonElement count : output.getAsJsonArray("counts"))
                {
                    counts.add(integer(count.getAsJsonObject(), "count"));
                    chances.add(count.getAsJsonObject().get("chance").getAsDouble());
                }
            }
            else
            {
                int count = output.has("count") ? integer(output, "count") : 1;
                double chance = output.has("chance") ? output.get("chance").getAsDouble() : 100;
                if (count < 1 || !Double.isFinite(chance) || chance <= 0 || chance > 100)
                    throw new IllegalArgumentException("Invalid output count/chance");
                counts.add(count);
                chances.add(chance);
                if (chance < 100) { counts.add(0); chances.add(100 - chance); }
            }
            outputs.add(new RockCrusherRecipe.Output(item(output, false), counts.stream().mapToInt(Integer::intValue).toArray(),
                    chances.stream().mapToDouble(Double::doubleValue).toArray()));
        }
        return new RockCrusherRecipe.Recipe(stack, ore, quantity, outputs, integer(json, "cookTime"),
                integer(json, "energyUsage"), json.get("experience").getAsFloat());
    }

    private static int integer(JsonObject json, String key)
    {
        java.math.BigDecimal value = json.get(key).getAsBigDecimal();
        return value.intValueExact();
    }

    private static ItemStack item(JsonObject json, boolean wildcard)
    {
        String name = json.get("item").getAsString();
        Item item = Item.getByNameOrId(name);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + name);
        int meta = 0;
        if (json.has("meta"))
            meta = wildcard && json.get("meta").getAsString().equals("*") ? OreDictionary.WILDCARD_VALUE : integer(json, "meta");
        ItemStack result = new ItemStack(item, 1, meta);
        if (result.isEmpty()) throw new IllegalArgumentException("Empty item: " + name);
        return result;
    }
}