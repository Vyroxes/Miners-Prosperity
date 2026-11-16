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

public class MetalPressRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    public static void loadFromJSON()
    {
        File configDir = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes");
        File jsonFile = new File(configDir, "metal_press.json");

        try
        {
            JsonArray array = RecipeFiles.read(jsonFile, RECIPE_VERSION, MetalPressRecipeLoader::createDefaultFile);

            for (JsonElement el : array)
            {
                try
                {
                    JsonObject obj = el.getAsJsonObject();

                    if (!obj.has("input1"))
                    {
                        RecipeFiles.error(jsonFile, "Metal Press recipe has no input1: {}", obj);
                        continue;
                    }

                    if (!obj.has("dieType"))
                    {
                        RecipeFiles.error(jsonFile, "Metal Press recipe has no dieType: {}", obj);
                        continue;
                    }

                    if (!obj.has("result"))
                    {
                        RecipeFiles.error(jsonFile, "Metal Press recipe has no result: {}", obj);
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
                                RecipeFiles.error(jsonFile, "Metal Press recipe has no item for input1: {}", itemName);
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
                            RecipeFiles.error(jsonFile, "Metal Press input1 has neither item nor oreDict: {}", input1);
                            continue;
                        }
                    }

                    MetalPressRecipe.DieType dieType = null;

                    if (obj.has("dieType"))
                    {
                        try
                        {
                            dieType = MetalPressRecipe.DieType.valueOf(obj.get("dieType").getAsString().toUpperCase());
                        }
                        catch (IllegalArgumentException e)
                        {
                            RecipeFiles.error(jsonFile, "Metal Press recipe has unknown dieType: {}", obj.get("dieType").getAsString());
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
                        RecipeFiles.error(jsonFile, "Metal Press recipe has no item for result: {}", resultItemName);
                        continue;
                    }

                    ItemStack resultStack = new ItemStack(resultItem, resultCount, resultMeta);

                    int cookTime = obj.get("cookTime").getAsInt();
                    int energy = obj.get("energyUsage").getAsInt();
                    float xp = obj.get("experience").getAsFloat();

                    MetalPressRecipe.getInstance().addRecipe(input1Stack, oreDictInput1, quantity1, dieType, resultStack, cookTime, energy, xp);
                }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(jsonFile, "Skipping invalid MetalPress recipe: {}", el, error);
                }
            }

            MinersProsperity.logger.info("Loaded {} Metal Press recipes", MetalPressRecipe.getInstance().getLookupTable().getRecipes().size());
        }
        catch (Exception e)
        {
            RecipeFiles.error(jsonFile, "Error while loading metal press recipes from JSON", e);
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
                        // Metal Press - metal_press.json
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
                        // Required: input1, dieType, result, cookTime, energyUsage, experience.
                        // The ingredient field is named "input1", not "input".
                        // "dieType": PLATE, GEAR or ROD (case-insensitive).
                        // Place the corresponding die in the machine's die slot; do not include it in input1.
                        // The die takes durability damage during processing; it is not an ordinary consumed ingredient.
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
                        // The examples cover every die type, item/Ore Dictionary inputs, wildcard metadata,
                        // explicit input/output metadata, multiple output items and optional-field defaults.
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
                        //       "dieType": "PLATE",
                        //       "result": {
                        //         "item": "minersprosperity:iron_plate",
                        //         "meta": 0,
                        //         "count": 1
                        //       },
                        //       "cookTime": 200,
                        //       "energyUsage": 20,
                        //       "experience": 0.7
                        //     },
                        //     {
                        //       "input1": {
                        //         "oreDict": "ingotIron",
                        //         "count": 4
                        //       },
                        //       "dieType": "GEAR",
                        //       "result": {
                        //         "item": "minersprosperity:iron_gear"
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
                        //       "dieType": "ROD",
                        //       "result": {
                        //         "item": "minersprosperity:coal_rod",
                        //         "count": 2
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

        defaultRecipes.add(createRecipe(null, null, "gemCharcoal", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:charcoal_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemCoal", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:coal_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCopper", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:copper_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTin", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:tin_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBronze", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:bronze_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSilver", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:silver_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotLead", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:lead_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotZinc", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:zinc_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBrass", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:brass_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotAluminum", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:aluminum_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDuraluminum", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:duraluminum_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIron", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:iron_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedIron", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:refined_iron_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSteel", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:steel_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotNickel", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:nickel_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotChromium", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:chromium_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardGlowstone", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:glowstone_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemQuartz", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:quartz_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotInconel", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:inconel_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotUranium", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:uranium_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTungsten", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:tungsten_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotGold", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:gold_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotPlatinum", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:platinum_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotWhiteGold", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:white_gold_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotElectrum", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:electrum_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIridium", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:iridium_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTitanium", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:titanium_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemLapis", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:lapis_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRedstone", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:redstone_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemRuby", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:ruby_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemSapphire", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:sapphire_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemAmethyst", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:amethyst_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemTopaz", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:topaz_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemPeridot", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:peridot_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemDiamond", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:diamond_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedObsidian", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:refined_obsidian_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemEmerald", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:emerald_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobalt", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:cobalt_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobaltsteel", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:cobaltsteel_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemOnyx", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:onyx_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDarkSteel", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:dark_steel_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemBlackOpal", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:black_opal_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardAzure", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:azure_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRosalite", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:rosalite_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardVerdite", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:verdite_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemVoidium", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:voidium_gear", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardLuminosite", 4, MetalPressRecipe.DieType.GEAR, "minersprosperity:luminosite_gear", null, 1, 200, 20, 0.7f));

        defaultRecipes.add(createRecipe(null, null, "gemCharcoal", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:charcoal_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemCoal", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:coal_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCopper", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:copper_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTin", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:tin_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBronze", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:bronze_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSilver", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:silver_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotLead", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:lead_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotZinc", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:zinc_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBrass", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:brass_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotAluminum", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:aluminum_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDuraluminum", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:duraluminum_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIron", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:iron_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedIron", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:refined_iron_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSteel", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:steel_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotNickel", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:nickel_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotChromium", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:chromium_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardGlowstone", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:glowstone_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemQuartz", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:quartz_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotInconel", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:inconel_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotUranium", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:uranium_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTungsten", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:tungsten_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotGold", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:gold_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotPlatinum", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:platinum_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotWhiteGold", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:white_gold_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotElectrum", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:electrum_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIridium", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:iridium_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTitanium", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:titanium_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemLapis", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:lapis_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRedstone", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:redstone_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemRuby", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:ruby_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemSapphire", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:sapphire_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemAmethyst", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:amethyst_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemTopaz", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:topaz_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemPeridot", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:peridot_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemDiamond", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:diamond_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedObsidian", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:refined_obsidian_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemEmerald", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:emerald_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobalt", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:cobalt_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobaltsteel", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:cobaltsteel_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemOnyx", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:onyx_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDarkSteel", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:dark_steel_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemBlackOpal", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:black_opal_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardAzure", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:azure_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRosalite", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:rosalite_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardVerdite", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:verdite_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemVoidium", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:voidium_plate", null, 1, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardLuminosite", 1, MetalPressRecipe.DieType.PLATE, "minersprosperity:luminosite_plate", null, 1, 200, 20, 0.7f));

        defaultRecipes.add(createRecipe(null, null, "gemCharcoal", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:charcoal_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemCoal", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:coal_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCopper", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:copper_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTin", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:tin_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBronze", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:bronze_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSilver", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:silver_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotLead", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:lead_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotZinc", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:zinc_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotBrass", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:brass_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotAluminum", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:aluminum_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDuraluminum", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:duraluminum_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIron", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:iron_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedIron", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:refined_iron_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotSteel", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:steel_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotNickel", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:nickel_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotChromium", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:chromium_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardGlowstone", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:glowstone_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemQuartz", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:quartz_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotInconel", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:inconel_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotUranium", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:uranium_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTungsten", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:tungsten_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotGold", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:gold_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotPlatinum", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:platinum_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotWhiteGold", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:white_gold_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotElectrum", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:electrum_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotIridium", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:iridium_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotTitanium", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:titanium_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemLapis", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:lapis_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRedstone", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:redstone_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemRuby", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:ruby_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemSapphire", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:sapphire_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemAmethyst", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:amethyst_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemTopaz", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:topaz_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemPeridot", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:peridot_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemDiamond", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:diamond_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotRefinedObsidian", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:refined_obsidian_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "clusterObsidian", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:obsidian_cluster_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemEmerald", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:emerald_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobalt", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:cobalt_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotCobaltsteel", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:cobaltsteel_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemOnyx", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:onyx_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "ingotDarkSteel", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:dark_steel_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemBlackOpal", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:black_opal_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardAzure", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:azure_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardRosalite", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:rosalite_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardVerdite", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:verdite_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "gemVoidium", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:voidium_rod", null, 2, 200, 20, 0.7f));
        defaultRecipes.add(createRecipe(null, null, "shardLuminosite", 1, MetalPressRecipe.DieType.ROD, "minersprosperity:luminosite_rod", null, 2, 200, 20, 0.7f));

        return defaultRecipes;
    }

    private static JsonObject createRecipe(String input1Item, Object input1Meta, String input1OreDict, int quantity1, MetalPressRecipe.DieType dieType, String resultItem, Integer resultMeta, int resultCount, int cookTime, int energyUsage, float experience)
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

        recipe.add("input1", input1);

        if (dieType != null) recipe.addProperty("dieType", dieType.name());

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
        else throw new IllegalArgumentException("Metal Press recipe has invalid metadata: " + meta);
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