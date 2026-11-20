package net.vyroxes.minersprosperity.machine.recipe;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.*;

public final class SeparatorRecipe
{
    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static final Map<Fluid, List<Recipe>> BY_FLUID = new HashMap<>();

    private SeparatorRecipe()
    {
    }

    static void replaceRecipes(List<Recipe> recipes)
    {
        RECIPES.clear();
        RECIPES.addAll(recipes);
        BY_FLUID.clear();
        for (Recipe recipe : recipes)
            BY_FLUID.computeIfAbsent(recipe.fluid().getFluid(), key -> new ArrayList<>()).add(recipe);
    }

    public static List<Recipe> getRecipes()
    {
        return Collections.unmodifiableList(RECIPES);
    }

    public static boolean isFluidValid(@Nullable FluidStack fluid)
    {
        return fluid != null && BY_FLUID.containsKey(fluid.getFluid());
    }

    public static boolean isInputValid(ItemStack stack)
    {
        for (Recipe recipe : RECIPES) if (recipe.matchesItem(stack)) return true;
        return false;
    }

    @Nullable
    public static Recipe findRecipe(ItemStack item, @Nullable FluidStack fluid)
    {
        if (item.isEmpty() || !isFluidValid(fluid)) return null;
        for (Recipe recipe : BY_FLUID.get(fluid.getFluid()))
        {
            if (item.getCount() >= recipe.quantity() && fluid.amount >= recipe.fluid().amount && recipe.matchesItem(item)) return recipe;
        }
        return null;
    }

    @Desugar
    public record Recipe(ItemStack input, @Nullable String oreDict, int quantity, FluidStack fluid, ItemStack result,
                         int totalCookTime, int energyUsage, float experience)
    {
        public boolean matchesItem(ItemStack stack)
        {
            if (stack.isEmpty()) return false;
            return oreDict == null ? OreDictionary.itemMatches(input, stack, false) : GrinderRecipe.isOreDictMatch(stack, oreDict);
        }
    }
}