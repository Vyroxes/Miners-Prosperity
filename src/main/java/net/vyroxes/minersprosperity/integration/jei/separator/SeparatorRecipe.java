package net.vyroxes.minersprosperity.integration.jei.separator;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.vyroxes.minersprosperity.integration.jei.JEICompat;
import java.awt.*;
import java.util.List;

public class SeparatorRecipe implements IRecipeWrapper
{
    private final List<List<ItemStack>> inputs;
    private final ItemStack output;
    private final FluidStack fluid;
    private final float experience;
    private final int cookTime;
    private final int energy;

    public SeparatorRecipe(List<List<ItemStack>> inputs, ItemStack output, FluidStack fluid, int cookTime, int energy, float experience)
    {
        this.inputs = inputs;
        this.output = output;
        this.fluid = fluid;
        this.experience = experience;
        this.cookTime = cookTime;
        this.energy = energy;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void getIngredients(IIngredients ingredients)
    {
        ingredients.setInputLists(ItemStack.class, inputs);
        ingredients.setInput(FluidStack.class, fluid);
        ingredients.setOutput(ItemStack.class, output);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY)
    {
        FontRenderer renderer = minecraft.fontRenderer;
        String experienceString = JEICompat.translateToLocalFormatted("gui.jei.category.smelting.experience", experience);
        int experienceStringWidth = renderer.getStringWidth(experienceString);
        renderer.drawString(experienceString, recipeWidth - experienceStringWidth, 0, Color.GRAY.getRGB());

        String cookTimeString = cookTime/20 + " seconds";
        int cookTimeStringWidth = renderer.getStringWidth(cookTimeString);
        renderer.drawString(cookTimeString, 1 + (recipeWidth - cookTimeStringWidth)/2, 44, Color.GRAY.getRGB());

        String energyString = energy + " FE/t";
        int energyStringWidth = renderer.getStringWidth(energyString);
        renderer.drawString(energyString, 1 + (recipeWidth - energyStringWidth)/2, 54, Color.GRAY.getRGB());
    }
}