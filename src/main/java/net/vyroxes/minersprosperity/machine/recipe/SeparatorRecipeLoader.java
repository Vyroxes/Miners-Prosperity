package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SeparatorRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    private SeparatorRecipeLoader()
    {
    }

    public static void loadFromJSON()
    {
        File file = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes/separator.json");
        try
        {
            List<SeparatorRecipe.Recipe> recipes = new ArrayList<>();
            for (JsonElement element : RecipeFiles.read(file, RECIPE_VERSION, SeparatorRecipeLoader::writeDefaults))
            {
                try
                {
                    JsonObject json = element.getAsJsonObject();
                    JsonObject input = json.getAsJsonObject("input");
                    JsonObject liquid = json.getAsJsonObject("fluid");
                    String oreDict = input.has("oreDict") ? RecipeFiles.oreDictionaryName(input) : null;
                    int count = input.has("count") ? positive(input, "count") : 1;
                    ItemStack stack = oreDict == null ? readStack(input, count, true) : ItemStack.EMPTY;
                    FluidStack fluid = FluidRegistry.getFluidStack(liquid.get("name").getAsString(), positive(liquid, "amount"));
                    if (fluid == null) throw new JsonParseException("Unknown fluid: " + liquid);
                    JsonObject output = json.getAsJsonObject("result");
                    ItemStack result = readStack(output, output.has("count") ? positive(output, "count") : 1, false);
                    float xp = json.get("experience").getAsFloat();
                    if (!Float.isFinite(xp) || xp < 0) throw new JsonParseException("Invalid experience");
                    recipes.add(new SeparatorRecipe.Recipe(stack, oreDict, count, fluid, result,
                            positive(json, "cookTime"), positive(json, "energyUsage"), xp));
                }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(file, "Skipping invalid Separator recipe: {}", element, error);
                }
            }
            SeparatorRecipe.replaceRecipes(recipes);
            MinersProsperity.logger.info("Loaded {} Separator recipes", recipes.size());
        }
        catch (IOException | RuntimeException error)
        {
            RecipeFiles.error(file, "Error while loading Separator recipes", error);
        }
    }

    private static int positive(JsonObject json, String key)
    {
        int value = json.get(key).getAsInt();
        if (value <= 0) throw new JsonParseException(key + " must be positive");
        return value;
    }

    private static ItemStack readStack(JsonObject json, int count, boolean input)
    {
        String id = json.get("item").getAsString();
        Item item = Item.getByNameOrId(id);
        if (item == null) throw new JsonParseException("Unknown item: " + id);
        int meta = !json.has("meta") ? 0 : input && "*".equals(json.get("meta").getAsString())
                ? OreDictionary.WILDCARD_VALUE : json.get("meta").getAsInt();
        ItemStack stack = new ItemStack(item, count, meta);
        if (stack.isEmpty() || count > stack.getMaxStackSize()) throw new JsonParseException("Invalid stack: " + json);
        return stack;
    }

    private static void writeDefaults(File file) throws IOException
    {
        JsonArray recipes = new JsonArray();
        recipes.add(defaultRecipe("sulfur_clump", "water", "sulfur_dirty_dust"));
        recipes.add(defaultRecipe("graphite_clump", "water", "graphite_dirty_dust"));
        recipes.add(defaultRecipe("salt_clump", "water", "salt_dirty_dust"));
        recipes.add(defaultRecipe("coal_clump", "water", "coal_dirty_dust"));
        recipes.add(defaultRecipe("iron_clump", "water", "iron_dirty_dust"));
        recipes.add(defaultRecipe("quartz_clump", "water", "quartz_dirty_dust"));
        recipes.add(defaultRecipe("lapis_clump", "water", "lapis_dirty_dust"));
        recipes.add(defaultRecipe("redstone_clump", "water", "redstone_dirty_dust"));
        recipes.add(defaultRecipe("gold_clump", "water", "gold_dirty_dust"));
        recipes.add(defaultRecipe("diamond_clump", "water", "diamond_dirty_dust"));
        recipes.add(defaultRecipe("emerald_clump", "water", "emerald_dirty_dust"));
        recipes.add(defaultRecipe("copper_clump", "water", "copper_dirty_dust"));
        recipes.add(defaultRecipe("tin_clump", "water", "tin_dirty_dust"));
        recipes.add(defaultRecipe("silver_clump", "water", "silver_dirty_dust"));
        recipes.add(defaultRecipe("lead_clump", "water", "lead_dirty_dust"));
        recipes.add(defaultRecipe("zinc_clump", "water", "zinc_dirty_dust"));
        recipes.add(defaultRecipe("aluminum_clump", "water", "aluminum_dirty_dust"));
        recipes.add(defaultRecipe("nickel_clump", "water", "nickel_dirty_dust"));
        recipes.add(defaultRecipe("chromium_clump", "water", "chromium_dirty_dust"));
        recipes.add(defaultRecipe("uranium_clump", "water", "uranium_dirty_dust"));
        recipes.add(defaultRecipe("tungsten_clump", "water", "tungsten_dirty_dust"));
        recipes.add(defaultRecipe("platinum_clump", "water", "platinum_dirty_dust"));
        recipes.add(defaultRecipe("iridium_clump", "water", "iridium_dirty_dust"));
        recipes.add(defaultRecipe("titanium_clump", "water", "titanium_dirty_dust"));
        recipes.add(defaultRecipe("ruby_clump", "water", "ruby_dirty_dust"));
        recipes.add(defaultRecipe("sapphire_clump", "water", "sapphire_dirty_dust"));
        recipes.add(defaultRecipe("amethyst_clump", "water", "amethyst_dirty_dust"));
        recipes.add(defaultRecipe("topaz_clump", "water", "topaz_dirty_dust"));
        recipes.add(defaultRecipe("peridot_clump", "water", "peridot_dirty_dust"));
        recipes.add(defaultRecipe("cobalt_clump", "water", "cobalt_dirty_dust"));
        recipes.add(defaultRecipe("onyx_clump", "water", "onyx_dirty_dust"));
        recipes.add(defaultRecipe("black_opal_clump", "water", "black_opal_dirty_dust"));
        recipes.add(defaultRecipe("azure_clump", "water", "azure_dirty_dust"));
        recipes.add(defaultRecipe("rosalite_clump", "water", "rosalite_dirty_dust"));
        recipes.add(defaultRecipe("verdite_clump", "water", "verdite_dirty_dust"));
        recipes.add(defaultRecipe("voidium_clump", "water", "voidium_dirty_dust"));
        recipes.add(defaultRecipe("luminosite_clump", "water", "luminosite_dirty_dust"));

        recipes.add(defaultRecipe("sulfur_clump", "flotation_solution", "sulfur_concentrate"));
        recipes.add(defaultRecipe("graphite_clump", "flotation_solution", "graphite_concentrate"));
        recipes.add(defaultRecipe("salt_clump", "flotation_solution", "salt_concentrate"));
        recipes.add(defaultRecipe("coal_clump", "flotation_solution", "coal_concentrate"));
        recipes.add(defaultRecipe("iron_clump", "flotation_solution", "iron_concentrate"));
        recipes.add(defaultRecipe("quartz_clump", "flotation_solution", "quartz_concentrate"));
        recipes.add(defaultRecipe("lapis_clump", "flotation_solution", "lapis_concentrate"));
        recipes.add(defaultRecipe("redstone_clump", "flotation_solution", "redstone_concentrate"));
        recipes.add(defaultRecipe("gold_clump", "flotation_solution", "gold_concentrate"));
        recipes.add(defaultRecipe("diamond_clump", "flotation_solution", "diamond_concentrate"));
        recipes.add(defaultRecipe("emerald_clump", "flotation_solution", "emerald_concentrate"));
        recipes.add(defaultRecipe("copper_clump", "flotation_solution", "copper_concentrate"));
        recipes.add(defaultRecipe("tin_clump", "flotation_solution", "tin_concentrate"));
        recipes.add(defaultRecipe("silver_clump", "flotation_solution", "silver_concentrate"));
        recipes.add(defaultRecipe("lead_clump", "flotation_solution", "lead_concentrate"));
        recipes.add(defaultRecipe("zinc_clump", "flotation_solution", "zinc_concentrate"));
        recipes.add(defaultRecipe("aluminum_clump", "flotation_solution", "aluminum_concentrate"));
        recipes.add(defaultRecipe("nickel_clump", "flotation_solution", "nickel_concentrate"));
        recipes.add(defaultRecipe("chromium_clump", "flotation_solution", "chromium_concentrate"));
        recipes.add(defaultRecipe("uranium_clump", "flotation_solution", "uranium_concentrate"));
        recipes.add(defaultRecipe("tungsten_clump", "flotation_solution", "tungsten_concentrate"));
        recipes.add(defaultRecipe("platinum_clump", "flotation_solution", "platinum_concentrate"));
        recipes.add(defaultRecipe("iridium_clump", "flotation_solution", "iridium_concentrate"));
        recipes.add(defaultRecipe("titanium_clump", "flotation_solution", "titanium_concentrate"));
        recipes.add(defaultRecipe("ruby_clump", "flotation_solution", "ruby_concentrate"));
        recipes.add(defaultRecipe("sapphire_clump", "flotation_solution", "sapphire_concentrate"));
        recipes.add(defaultRecipe("amethyst_clump", "flotation_solution", "amethyst_concentrate"));
        recipes.add(defaultRecipe("topaz_clump", "flotation_solution", "topaz_concentrate"));
        recipes.add(defaultRecipe("peridot_clump", "flotation_solution", "peridot_concentrate"));
        recipes.add(defaultRecipe("cobalt_clump", "flotation_solution", "cobalt_concentrate"));
        recipes.add(defaultRecipe("onyx_clump", "flotation_solution", "onyx_concentrate"));
        recipes.add(defaultRecipe("black_opal_clump", "flotation_solution", "black_opal_concentrate"));
        recipes.add(defaultRecipe("azure_clump", "flotation_solution", "azure_concentrate"));
        recipes.add(defaultRecipe("rosalite_clump", "flotation_solution", "rosalite_concentrate"));
        recipes.add(defaultRecipe("verdite_clump", "flotation_solution", "verdite_concentrate"));
        recipes.add(defaultRecipe("voidium_clump", "flotation_solution", "voidium_concentrate"));
        recipes.add(defaultRecipe("luminosite_clump", "flotation_solution", "luminosite_concentrate"));
        String exampleComment =
                """
                        // Separator - separator.json
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
                        // Required: input, fluid, result, cookTime, energyUsage, experience.
                        // Both the item input and fluid are consumed for one operation.
                        // The first matching recipe with sufficient input and fluid is selected.
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
                        // FLUID FIELDS
                        // "fluid": required object with "name" and "amount".
                        // "name": registered fluid name, such as "water" or "flotation_solution".
                        // "amount": required positive integer in mB consumed per operation.
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
                        // Counts, fluid amounts, time and energy are whole numbers; XP may be fractional.
                        //
                        // EXAMPLES
                        // These examples show an item input with explicit metadata and an Ore Dictionary input.
                        // They demonstrate syntax; choose the ingredients and balance appropriate to your pack.
                        //
                        // {
                        //   "version": "1.0",
                        //   "recipes": [
                        //     {
                        //       "input": {
                        //         "item": "minersprosperity:iron_clump",
                        //         "meta": 0,
                        //         "count": 3
                        //       },
                        //       "fluid": {
                        //         "name": "water",
                        //         "amount": 250
                        //       },
                        //       "result": {
                        //         "item": "minersprosperity:iron_dirty_dust",
                        //         "count": 4
                        //       },
                        //       "cookTime": 200,
                        //       "energyUsage": 20,
                        //       "experience": 0.7
                        //     },
                        //     {
                        //       "input": {
                        //         "oreDict": "clumpIron",
                        //         "count": 3
                        //       },
                        //       "fluid": {
                        //         "name": "flotation_solution",
                        //         "amount": 250
                        //       },
                        //       "result": {
                        //         "item": "minersprosperity:iron_concentrate",
                        //         "count": 4
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
            new GsonBuilder().setPrettyPrinting().create().toJson(RecipeFiles.document(RECIPE_VERSION, recipes), writer);
        }
    }

    private static JsonObject defaultRecipe(String inputItem, String fluidName, String outputItem)
    {
        JsonObject recipe = new JsonObject();
        JsonObject input = new JsonObject();
        input.addProperty("item", Tags.MODID + ":" + inputItem);
        input.addProperty("count", 3);
        recipe.add("input", input);
        JsonObject fluid = new JsonObject();
        fluid.addProperty("name", fluidName);
        fluid.addProperty("amount", 250);
        recipe.add("fluid", fluid);
        JsonObject result = new JsonObject();
        result.addProperty("item", Tags.MODID + ":" + outputItem);
        result.addProperty("count", 4);
        recipe.add("result", result);
        recipe.addProperty("cookTime", 200);
        recipe.addProperty("energyUsage", 20);
        recipe.addProperty("experience", 0.7F);
        return recipe;
    }
}