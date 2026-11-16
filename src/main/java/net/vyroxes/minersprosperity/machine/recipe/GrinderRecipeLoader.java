package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class GrinderRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    public static void loadFromJSON()
    {
        File configDir = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes");
        File jsonFile = new File(configDir, "grinder.json");

        try
        {
            JsonArray array = RecipeFiles.read(jsonFile, RECIPE_VERSION, GrinderRecipeLoader::createDefaultFile);

            for (JsonElement el : array)
            {
                try
                {
                    JsonObject obj = el.getAsJsonObject();

                    if (!obj.has("input"))
                    {
                        RecipeFiles.error(jsonFile, "Grinder recipe has no input: {}", obj);
                        continue;
                    }

                    if (!obj.has("result"))
                    {
                        RecipeFiles.error(jsonFile, "Grinder recipe has no result: {}", obj);
                        continue;
                    }

                    ItemStack inputStack = null;
                    String oreDictInput = null;
                    int quantity = 1;

                    if (obj.has("input"))
                    {
                        JsonObject input = obj.getAsJsonObject("input");
                        if (input.has("item"))
                        {
                            String itemName = input.get("item").getAsString();
                            int meta = getMetadata(input);
                            int count = input.has("count") ? input.get("count").getAsInt() : 1;

                            Item item = Item.getByNameOrId(itemName);
                            if (item == null)
                            {
                                RecipeFiles.error(jsonFile, "Grinder recipe has no item for input: {}", itemName);
                                continue;
                            }
                            inputStack = new ItemStack(item, count, meta);
                            quantity = count;
                        }
                        else if (input.has("oreDict"))
                        {
                            oreDictInput = RecipeFiles.oreDictionaryName(input);
                            quantity = input.has("count") ? input.get("count").getAsInt() : 1;
                        }
                        else
                        {
                            RecipeFiles.error(jsonFile, "Grinder input has neither item nor oreDict: {}", input);
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
                        RecipeFiles.error(jsonFile, "Grinder recipe has no item for result: {}", resultItemName);
                        continue;
                    }

                    ItemStack resultStack = new ItemStack(resultItem, resultCount, resultMeta);

                    int cookTime = obj.get("cookTime").getAsInt();
                    int energy = obj.get("energyUsage").getAsInt();
                    float xp = obj.get("experience").getAsFloat();

                    GrinderRecipe.getInstance().addRecipe(inputStack, oreDictInput, quantity, resultStack, cookTime, energy, xp);
                }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(jsonFile, "Skipping invalid Grinder recipe: {}", el, error);
                }
            }

            MinersProsperity.logger.info("Loaded {} Grinder recipes", GrinderRecipe.getInstance().getLookupTable().getRecipes().size());
        }
        catch (Exception e)
        {
            RecipeFiles.error(jsonFile, "Error while loading grinder recipes from JSON", e);
        }
    }

    private static void createDefaultFile(File file) throws IOException
    {
        File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Cannot create " + directory);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            new GsonBuilder().setPrettyPrinting().create().toJson(RecipeFiles.document(RECIPE_VERSION, getJsonElements()), writer);
        }
    }

    private static JsonArray getJsonElements()
    {
        JsonArray recipes = new JsonArray();
        recipes.add(createRecipe("minersprosperity:sulfur_chunk", null, null, 2, "minersprosperity:sulfur_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:graphite_chunk", null, null, 2, "minersprosperity:graphite_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:salt_chunk", null, null, 2, "minersprosperity:salt_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:coal_chunk", null, null, 2, "minersprosperity:coal_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:iron_chunk", null, null, 2, "minersprosperity:iron_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:quartz_chunk", null, null, 2, "minersprosperity:quartz_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:lapis_chunk", null, null, 2, "minersprosperity:lapis_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:redstone_chunk", null, null, 2, "minersprosperity:redstone_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:gold_chunk", null, null, 2, "minersprosperity:gold_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:diamond_chunk", null, null, 2, "minersprosperity:diamond_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:emerald_chunk", null, null, 2, "minersprosperity:emerald_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:copper_chunk", null, null, 2, "minersprosperity:copper_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:tin_chunk", null, null, 2, "minersprosperity:tin_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:silver_chunk", null, null, 2, "minersprosperity:silver_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:lead_chunk", null, null, 2, "minersprosperity:lead_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:zinc_chunk", null, null, 2, "minersprosperity:zinc_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:aluminum_chunk", null, null, 2, "minersprosperity:aluminum_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:nickel_chunk", null, null, 2, "minersprosperity:nickel_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:chromium_chunk", null, null, 2, "minersprosperity:chromium_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:uranium_chunk", null, null, 2, "minersprosperity:uranium_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:tungsten_chunk", null, null, 2, "minersprosperity:tungsten_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:platinum_chunk", null, null, 2, "minersprosperity:platinum_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:iridium_chunk", null, null, 2, "minersprosperity:iridium_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:titanium_chunk", null, null, 2, "minersprosperity:titanium_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:ruby_chunk", null, null, 2, "minersprosperity:ruby_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:sapphire_chunk", null, null, 2, "minersprosperity:sapphire_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:amethyst_chunk", null, null, 2, "minersprosperity:amethyst_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:topaz_chunk", null, null, 2, "minersprosperity:topaz_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:peridot_chunk", null, null, 2, "minersprosperity:peridot_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:cobalt_chunk", null, null, 2, "minersprosperity:cobalt_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:onyx_chunk", null, null, 2, "minersprosperity:onyx_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:black_opal_chunk", null, null, 2, "minersprosperity:black_opal_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:azure_chunk", null, null, 2, "minersprosperity:azure_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:rosalite_chunk", null, null, 2, "minersprosperity:rosalite_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:verdite_chunk", null, null, 2, "minersprosperity:verdite_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:voidium_chunk", null, null, 2, "minersprosperity:voidium_clump", null, 3, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:luminosite_chunk", null, null, 2, "minersprosperity:luminosite_clump", null, 3, 200, 20, 0.7F));
        return recipes;
    }

    private static JsonObject createRecipe(String inputItem, Object inputMeta, String inputOreDict, int quantity, String resultItem, Integer resultMeta, int resultCount, int cookTime, int energyUsage, float experience)
    {
        JsonObject recipe = new JsonObject();

        JsonObject input = new JsonObject();
        if (inputItem != null)
        {
            input.addProperty("item", inputItem);
            addMeta(input, inputMeta);
        }
        else if (inputOreDict != null) input.addProperty("oreDict", inputOreDict);
        input.addProperty("count", quantity);

        recipe.add("input", input);

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
        else throw new IllegalArgumentException("Grinder recipe has invalid metadata: " + meta);
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