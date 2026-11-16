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

public class CrystallizerRecipeLoader
{
    private static final String RECIPE_VERSION = "1.0";

    public static void loadFromJSON()
    {
        File configDir = new File(Loader.instance().getConfigDir(), Tags.MODID + "/recipes");
        File jsonFile = new File(configDir, "crystallizer.json");

        try
        {
            JsonArray array = RecipeFiles.read(jsonFile, RECIPE_VERSION, CrystallizerRecipeLoader::createDefaultFile);

            for (JsonElement el : array)
            {
                try
                {
                    JsonObject obj = el.getAsJsonObject();

                    if (!obj.has("input"))
                    {
                        RecipeFiles.error(jsonFile, "Crystallizer recipe has no input: {}", obj);
                        continue;
                    }

                    if (!obj.has("result"))
                    {
                        RecipeFiles.error(jsonFile, "Crystallizer recipe has no result: {}", obj);
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
                                RecipeFiles.error(jsonFile, "Crystallizer recipe has no item for input: {}", itemName);
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
                            RecipeFiles.error(jsonFile, "Crystallizer input has neither item nor oreDict: {}", input);
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
                        RecipeFiles.error(jsonFile, "Crystallizer recipe has no item for result: {}", resultItemName);
                        continue;
                    }

                    ItemStack resultStack = new ItemStack(resultItem, resultCount, resultMeta);

                    int cookTime = obj.get("cookTime").getAsInt();
                    int energy = obj.get("energyUsage").getAsInt();
                    float xp = obj.get("experience").getAsFloat();

                    CrystallizerRecipe.getInstance().addRecipe(inputStack, oreDictInput, quantity, resultStack, cookTime, energy, xp);
                }
                catch (RuntimeException error)
                {
                    RecipeFiles.error(jsonFile, "Skipping invalid Crystallizer recipe: {}", el, error);
                }
            }

            MinersProsperity.logger.info("Loaded {} Crystallizer recipes", CrystallizerRecipe.getInstance().getLookupTable().getRecipes().size());
        }
        catch (Exception e)
        {
            RecipeFiles.error(jsonFile, "Error while loading crystallizer recipes from JSON", e);
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
        recipes.add(createRecipe("minecraft:glowstone_dust", null, null, 1, "minersprosperity:glowstone_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:quartz_dust", null, null, 1, "minecraft:quartz", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:quartz_dirty_dust", null, null, 1, "minecraft:quartz", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:tanzanite_dust", null, null, 1, "minersprosperity:tanzanite", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:ruby_dust", null, null, 1, "minersprosperity:ruby", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:ruby_dirty_dust", null, null, 1, "minersprosperity:ruby", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:sapphire_dust", null, null, 1, "minersprosperity:sapphire", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:sapphire_dirty_dust", null, null, 1, "minersprosperity:sapphire", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:amethyst_dust", null, null, 1, "minersprosperity:amethyst", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:amethyst_dirty_dust", null, null, 1, "minersprosperity:amethyst", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:topaz_dust", null, null, 1, "minersprosperity:topaz", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:topaz_dirty_dust", null, null, 1, "minersprosperity:topaz", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:peridot_dust", null, null, 1, "minersprosperity:peridot", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:peridot_dirty_dust", null, null, 1, "minersprosperity:peridot", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:diamond_dust", null, null, 1, "minecraft:diamond", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:diamond_dirty_dust", null, null, 1, "minecraft:diamond", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:emerald_dust", null, null, 1, "minecraft:emerald", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:emerald_dirty_dust", null, null, 1, "minecraft:emerald", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:onyx_dust", null, null, 1, "minersprosperity:onyx", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:onyx_dirty_dust", null, null, 1, "minersprosperity:onyx", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:black_opal_dust", null, null, 1, "minersprosperity:black_opal", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:black_opal_dirty_dust", null, null, 1, "minersprosperity:black_opal", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:azure_dust", null, null, 1, "minersprosperity:azure_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:azure_dirty_dust", null, null, 1, "minersprosperity:azure_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:rosalite_dust", null, null, 1, "minersprosperity:rosalite_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:rosalite_dirty_dust", null, null, 1, "minersprosperity:rosalite_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:verdite_dust", null, null, 1, "minersprosperity:verdite_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:verdite_dirty_dust", null, null, 1, "minersprosperity:verdite_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:voidium_dust", null, null, 1, "minersprosperity:voidium", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:voidium_dirty_dust", null, null, 1, "minersprosperity:voidium", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:luminosite_dust", null, null, 1, "minersprosperity:luminosite_shard", null, 1, 200, 20, 0.7F));
        recipes.add(createRecipe("minersprosperity:luminosite_dirty_dust", null, null, 1, "minersprosperity:luminosite_shard", null, 1, 200, 20, 0.7F));
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
        else throw new IllegalArgumentException("Crystallizer recipe has invalid metadata: " + meta);
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