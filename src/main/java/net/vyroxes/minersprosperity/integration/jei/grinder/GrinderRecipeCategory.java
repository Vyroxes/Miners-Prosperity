package net.vyroxes.minersprosperity.integration.jei.grinder;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.init.ModBlocks;
import net.vyroxes.minersprosperity.integration.jei.RecipeCategories;
import org.jetbrains.annotations.NotNull;

public class GrinderRecipeCategory extends AbstractGrinderRecipeCategory<GrinderRecipe>
{
    private final IDrawable background;
    private final String name;
    private final IDrawable icon;

    public GrinderRecipeCategory(IGuiHelper helper)
    {
        super(helper);
        background = helper.createDrawable(GRINDER, 42, 21, 82, 54);
        name = I18n.format("tile.grinder.name");
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocks.GRINDER));
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public @NotNull IDrawable getBackground()
    {
        return background;
    }

    @Override
    public void drawExtras(@NotNull Minecraft minecraft)
    {
        animatedArrow.draw(minecraft, 26, 14);
    }

    @Override
    public @NotNull String getTitle()
    {
        return name;
    }

    @Override
    public @NotNull String getModName()
    {
        return Tags.MODNAME;
    }

    @Override
    public @NotNull String getUid()
    {
        return RecipeCategories.GRINDER;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, @NotNull GrinderRecipe recipeWrapper, @NotNull IIngredients ingredients)
    {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(input, true, 0, 13);
        stacks.init(output, false, 61, 13);
        stacks.set(ingredients);
    }
}