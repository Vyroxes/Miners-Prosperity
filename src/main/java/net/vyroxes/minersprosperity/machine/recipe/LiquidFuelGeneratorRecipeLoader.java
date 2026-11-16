package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.Tags;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class LiquidFuelGeneratorRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    public static void loadFromJSON()
    {
        File file = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes/liquid_fuel_generator.json");
        LiquidFuelGeneratorRecipe recipes = LiquidFuelGeneratorRecipe.getInstance();
        recipes.clear();

        try
        {
            JsonArray array = RecipeFiles.read(file, RECIPE_VERSION, LiquidFuelGeneratorRecipeLoader::createDefaultFile);
            for (JsonElement element : array)
            {
                try
                {
                    JsonObject recipe = element.getAsJsonObject();
                    Fluid fluid = FluidRegistry.getFluid(recipe.get("fluid").getAsString());
                    if (fluid == null) throw new IllegalArgumentException("Unknown fuel fluid");
                    int amount = recipe.get("amount").getAsInt();
                    if (amount > 16000) throw new IllegalArgumentException("Fuel batch exceeds the 16000 mB tank");
                    recipes.addRecipe(new FluidStack(fluid, amount), recipe.get("cookTime").getAsInt(), recipe.get("energyGeneration").getAsInt());
                }
                catch (RuntimeException exception)
                {
                    RecipeFiles.error(file, "Skipping invalid Liquid Fuel Generator recipe: {}", element, exception);
                }
            }
        }
        catch (IOException | RuntimeException exception)
        {
            RecipeFiles.error(file, "Could not load Liquid Fuel Generator recipes from {}", file, exception);
        }
    }
    private static void createDefaultFile(File file) throws IOException
    {
        if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())
        {
            throw new IOException("Cannot create liquid fuel recipe directory");
        }

        String exampleComment = """
                // Liquid Fuel Generator - liquid_fuel_generator.json
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
                // All four fields are required; none has a default value.
                // "fluid": Forge fluid registry name, for example "lava" or "crude_oil".
                // Use the fluid name, not an item/bucket ID, Ore Dictionary name or "minecraft:lava".
                // "amount": integer mB consumed per fuel batch; 1000 mB = one bucket.
                // The loader accepts 1-16000 mB per batch; the batch must also fit the configured tank.
                // "cookTime": positive integer fuel duration in ticks; 20 ticks = 1 second.
                // "energyGeneration": positive integer FE generated per tick, at most 214748364.
                // Counts/time/energy are whole numbers. These are base values before upgrades.
                // For example, 100 mB at 200 ticks and 40 FE/t produces 8000 FE in 10 seconds before upgrades.
                // This format has no item input/result, metadata, experience, output chances or quantity distribution.
                // Use one recipe per fluid. Invalid entries are logged and skipped; inspect the log if fuel is missing.
                //
                // EXAMPLES
                // The examples cover vanilla and modded fluid names, different batch amounts,
                // durations and generation rates. Each example includes every required field.
                //
                // {
                //   "version": "1.0",
                //   "recipes": [
                //     {
                //       "fluid": "lava",
                //       "amount": 100,
                //       "cookTime": 200,
                //       "energyGeneration": 40
                //     },
                //     {
                //       "fluid": "crude_oil",
                //       "amount": 1000,
                //       "cookTime": 2000,
                //       "energyGeneration": 80
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

    private static JsonArray getJsonElements()
    {
        JsonArray recipes = new JsonArray();

        recipes.add(createRecipe("lava", 100, 200, 40));
        recipes.add(createRecipe("crude_oil", 100, 200, 80));

        return recipes;
    }

    private static JsonObject createRecipe(String fluid, int amount, int cookTime, int energyGeneration)
    {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("fluid", fluid);
        recipe.addProperty("amount", amount);
        recipe.addProperty("cookTime", cookTime);
        recipe.addProperty("energyGeneration", energyGeneration);
        return recipe;
    }
}