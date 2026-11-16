package net.vyroxes.minersprosperity.integration.jei.rock_crusher;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.init.ModBlocks;
import net.vyroxes.minersprosperity.integration.jei.RecipeCategories;
import org.jetbrains.annotations.NotNull;

public class RockCrusherRecipeCategory extends AbstractRockCrusherRecipeCategory<RockCrusherRecipe>
{
    private final IDrawable background;
    private final IDrawable icon;

    public RockCrusherRecipeCategory(IGuiHelper helper)
    {
        super(helper);
        background = helper.createDrawable(ROCK_CRUSHER, 31, 24, 106, 54);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocks.ROCK_CRUSHER));

    }

    @Override
    public @NotNull String getUid()
    {
        return RecipeCategories.ROCK_CRUSHER;
    }

    @Override
    public @NotNull String getTitle()
    {
        return I18n.format("tile.rock_crusher.name");
    }

    @Override
    public @NotNull String getModName()
    {
        return Tags.MODNAME;
    }

    @Override
    public @NotNull IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void drawExtras(@NotNull Minecraft minecraft)
    {
        animatedArrow.draw(minecraft, 21, 11);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, @NotNull RockCrusherRecipe wrapper, @NotNull IIngredients ingredients)
    {
        IGuiItemStackGroup stacks = layout.getItemStacks();
        stacks.init(input, true, 0, 10);
        for (int i = 0; i < 3; i++) stacks.init(output + i, false, 48 + i * 20, 10);
        stacks.set(ingredients);
        stacks.addTooltipCallback((slot, input, ingredient, tooltip) ->
        {
            if (input || slot < 1 || slot > wrapper.recipe.outputs().size()) return;
            net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Output output = wrapper.recipe.outputs().get(slot - 1);
            for (int i = 0; i < output.counts.length; i++)
                tooltip.add(I18n.format("gui.jei.rock_crusher.count_chance", output.counts[i], output.chances[i]));
        });
    }
}