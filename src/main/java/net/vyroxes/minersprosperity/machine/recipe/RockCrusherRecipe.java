package net.vyroxes.minersprosperity.machine.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class RockCrusherRecipe
{
    private static final RockCrusherRecipe INSTANCE = new RockCrusherRecipe();
    private final List<Recipe> recipes = new ArrayList<>();

    public static RockCrusherRecipe getInstance()
    {
        return INSTANCE;
    }
    public List<Recipe> getRecipes()
    {
        return Collections.unmodifiableList(recipes);
    }
    public void clear()
    {
        recipes.clear();
    }
    public void addRecipe(Recipe recipe)
    {
        recipes.add(recipe);
    }

    public Recipe findRecipe(ItemStack input)
    {
        if (input.isEmpty()) return null;
        for (Recipe recipe : recipes) if (recipe.matches(input)) return recipe;
        return null;
    }

    public static final class Output
    {
        public final ItemStack item;
        public final int[] counts;
        public final double[] chances;
        public final int maxCount;

        public Output(ItemStack item, int[] counts, double[] chances)
        {
            if (item.isEmpty() || counts.length == 0 || counts.length != chances.length)
                throw new IllegalArgumentException("Output requires an item and count/chance pairs");
            double sum = 0;
            int max = 0;
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (int i = 0; i < counts.length; i++)
            {
                if (counts[i] < 0 || counts[i] > item.getMaxStackSize() || !seen.add(counts[i])
                        || !Double.isFinite(chances[i]) || chances[i] <= 0 || chances[i] > 100)
                    throw new IllegalArgumentException("Invalid or duplicate output count/chance");
                sum += chances[i];
                max = Math.max(max, counts[i]);
            }
            if (Math.abs(sum - 100) > 0.000001 || max == 0)
                throw new IllegalArgumentException("Output chances must add up to 100 and allow a positive result");
            this.item = item.copy();
            this.item.setCount(1);
            this.counts = counts.clone();
            this.chances = chances.clone();
            this.maxCount = max;
        }

        public int roll(Random random)
        {
            double roll = random.nextDouble() * 100;
            double cumulative = 0;
            for (int i = 0; i < counts.length; i++)
            {
                cumulative += chances[i];
                if (roll < cumulative) return counts[i];
            }
            return counts[counts.length - 1];
        }
    }

    @Desugar
    public record Recipe(ItemStack input, String oreDict, int quantity, List<Output> outputs, int totalCookTime, int energyUsage, float experience) {
            public Recipe(ItemStack input, String oreDict, int quantity, List<Output> outputs, int totalCookTime, int energyUsage, float experience) {
                if ((input.isEmpty() && (oreDict==null || oreDict.isEmpty())) || quantity < 1 || quantity > 64 || outputs.isEmpty() || outputs.size() > 3 || totalCookTime < 1 || energyUsage < 1 || !Float.isFinite(experience) || experience < 0)
                    throw new IllegalArgumentException("Invalid Rock Crusher recipe");
                this.input = input.copy();
                this.oreDict = oreDict;
                this.quantity = quantity;
                this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
                this.totalCookTime = totalCookTime;
                this.energyUsage = energyUsage;
                this.experience = experience;
            }

            public boolean matches(ItemStack stack) {
                return !stack.isEmpty() && (input.isEmpty() ? PulverizerRecipe.isOreDictMatch(stack, oreDict):OreDictionary.itemMatches(input, stack, false));
            }
        }
}