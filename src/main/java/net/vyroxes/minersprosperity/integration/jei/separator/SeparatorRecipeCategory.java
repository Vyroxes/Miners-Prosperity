package net.vyroxes.minersprosperity.integration.jei.separator;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
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

public class SeparatorRecipeCategory implements IRecipeCategory<SeparatorRecipe>
{
    private static final ResourceLocation SEPARATOR = new ResourceLocation(Tags.MODID, "textures/gui/separator.png");
    private static final ResourceLocation GUI_ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private final IDrawableAnimated animatedArrow;
    private final IDrawable background;
    private final String name;
    private final IDrawable icon;

    public SeparatorRecipeCategory(IGuiHelper helper)
    {
        animatedArrow = helper.createAnimatedDrawable(helper.createDrawable(GUI_ELEMENTS, 208, 157, 24, 17),
                200, IDrawableAnimated.StartDirection.LEFT, false);
        background = helper.createDrawable(SEPARATOR, 27, 22, 113, 47, 0, 17, 0, 0);
        name = I18n.format("tile.separator.name");
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocks.SEPARATOR));
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
        animatedArrow.draw(minecraft, 61, 13);
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
        return RecipeCategories.SEPARATOR;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, @NotNull SeparatorRecipe recipeWrapper, @NotNull IIngredients ingredients)
    {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(0, true, 1, 12);
        stacks.init(1, false, 91, 12);
        recipeLayout.getFluidStacks().init(0, true, 42, 3, 16, 36, 1000, false, null);
        recipeLayout.getFluidStacks().set(ingredients);
        stacks.set(ingredients);
    }
}