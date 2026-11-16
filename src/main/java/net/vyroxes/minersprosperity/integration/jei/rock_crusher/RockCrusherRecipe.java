package net.vyroxes.minersprosperity.integration.jei.rock_crusher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public class RockCrusherRecipe implements IRecipeWrapper
{
    public final net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Recipe recipe;
    private final List<ItemStack> inputs;

    public RockCrusherRecipe(net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Recipe recipe, List<ItemStack> inputs)
    {
        this.recipe = recipe;
        this.inputs = inputs;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void getIngredients(IIngredients ingredients)
    {
        ingredients.setInputLists(ItemStack.class, Collections.singletonList(inputs));
        List<List<ItemStack>> outputs = new ArrayList<>();
        for (net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Output output : recipe.outputs())
        {
            List<ItemStack> variants = new ArrayList<>();
            for (int count : output.counts)
            {
                if (count == 0) continue;
                ItemStack stack = output.item.copy();
                stack.setCount(count);
                variants.add(stack);
            }
            outputs.add(variants);
        }
        ingredients.setOutputLists(ItemStack.class, outputs);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY)
    {
        String experience = I18n.format("gui.jei.rock_crusher.experience", recipe.experience());
        minecraft.fontRenderer.drawString(experience, width - minecraft.fontRenderer.getStringWidth(experience), 0, 0x808080);
        for (int i = 0; i < recipe.outputs().size(); i++)
        {
            net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.Output output = recipe.outputs().get(i);
            double chance = 0;
            for (int j = 0; j < output.counts.length; j++) if (output.counts[j] > 0) chance += output.chances[j];
            String text = I18n.format("gui.jei.rock_crusher.chance", chance);
            GlStateManager.pushMatrix();
            GlStateManager.translate(57 + i * 20, 30, 0);
            GlStateManager.scale(0.6, 0.6, 1);
            minecraft.fontRenderer.drawString(text, -minecraft.fontRenderer.getStringWidth(text) / 2, 0, 0x808080);
            GlStateManager.popMatrix();
        }
        String time = I18n.format("gui.jei.rock_crusher.time", recipe.totalCookTime() / 20);
        minecraft.fontRenderer.drawString(time, 1 + (width - minecraft.fontRenderer.getStringWidth(time)) / 2, 37, 0x808080);
        String energy = I18n.format("gui.jei.rock_crusher.energy", recipe.energyUsage());
        minecraft.fontRenderer.drawString(energy, 1 + (width - minecraft.fontRenderer.getStringWidth(energy)) / 2, 47, 0x808080);
    }
}