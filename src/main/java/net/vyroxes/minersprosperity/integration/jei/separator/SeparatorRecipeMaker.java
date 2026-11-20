package net.vyroxes.minersprosperity.integration.jei.separator;

import mezz.jei.api.IJeiHelpers;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.machine.recipe.SeparatorRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeparatorRecipeMaker
{
    public static List<net.vyroxes.minersprosperity.integration.jei.separator.SeparatorRecipe> getRecipes(IJeiHelpers helpers)
    {
        List<net.vyroxes.minersprosperity.integration.jei.separator.SeparatorRecipe> jeiRecipes = new ArrayList<>();

        for (SeparatorRecipe.Recipe recipe : SeparatorRecipe.getRecipes())
        {
            List<ItemStack> inputVariants = getInputsFromStackOrOreDict(recipe.input(), recipe.oreDict(), recipe.quantity());
            if (inputVariants.isEmpty() || recipe.result() == null || recipe.result().isEmpty()) continue;

            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(inputVariants);

            jeiRecipes.add(new net.vyroxes.minersprosperity.integration.jei.separator.SeparatorRecipe(inputs, recipe.result().copy(), recipe.fluid().copy(), recipe.totalCookTime(), recipe.energyUsage(), recipe.experience()));
        }

        return jeiRecipes;
    }

    private static List<ItemStack> getInputsFromStackOrOreDict(ItemStack stack, String oreDictName, int amount)
    {
        if (stack != null && !stack.isEmpty())
        {
            ItemStack modifiedStack = stack.copy();
            modifiedStack.setCount(amount);
            return Collections.singletonList(modifiedStack);
        }

        if (oreDictName != null && !oreDictName.isEmpty())
        {
            List<ItemStack> oreStacks = new ArrayList<>();

            for (ItemStack oreStack : OreDictionary.getOres(oreDictName))
            {
                if (oreStack == null || oreStack.isEmpty()) continue;

                ItemStack modifiedStack = oreStack.copy();
                modifiedStack.setCount(amount);
                oreStacks.add(modifiedStack);
            }

            return oreStacks;
        }

        return Collections.emptyList();
    }
}