package net.vyroxes.minersprosperity.integration.jei.rock_crusher;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.Tags;

public abstract class AbstractRockCrusherRecipeCategory<T extends IRecipeWrapper> implements IRecipeCategory<T>
{
    protected static final ResourceLocation ROCK_CRUSHER = new ResourceLocation(Tags.MODID, "textures/gui/rock_crusher.png");
    protected static final ResourceLocation GUI_ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    protected static final int input = 0;
    protected static final int output = 1;
    protected final IDrawableAnimated animatedArrow;

    public AbstractRockCrusherRecipeCategory(IGuiHelper helper)
    {
        IDrawableStatic staticArrow = helper.createDrawable(GUI_ELEMENTS, 208, 140, 24, 16);
        animatedArrow = helper.createAnimatedDrawable(staticArrow, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }
}