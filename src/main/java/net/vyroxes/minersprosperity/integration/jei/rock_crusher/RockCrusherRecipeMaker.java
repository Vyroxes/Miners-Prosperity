package net.vyroxes.minersprosperity.integration.jei.rock_crusher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class RockCrusherRecipeMaker
{
    public static List<RockCrusherRecipe> getRecipes(mezz.jei.api.IJeiHelpers helpers)
    {
        List<RockCrusherRecipe> wrappers = new ArrayList<>();
        for (net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Recipe recipe
                : net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.getInstance().getRecipes())
        {
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> candidates = recipe.input().isEmpty() ? OreDictionary.getOres(recipe.oreDict(), false) : Collections.singletonList(recipe.input());
            for (ItemStack candidate : candidates)
            {
                if (candidate.isEmpty()) continue;
                ItemStack input = candidate.copy();
                input.setCount(recipe.quantity());
                inputs.add(input);
            }
            if (!inputs.isEmpty()) wrappers.add(new RockCrusherRecipe(recipe, inputs));
        }
        return wrappers;
    }

}