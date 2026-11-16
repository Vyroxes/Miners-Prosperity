package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.integration.jei.JEICompat;

public final class BaublesRecipeBook
{
    private BaublesRecipeBook()
    {
    }

    public static void open(Minecraft mc)
    {
        // Vanilla recipe placement and JEI transfer handlers expect the normal player container.
        GuiInventory inventory = new GuiInventory(mc.player);
        mc.displayGuiScreen(inventory);
        if (Loader.isModLoaded("jei") && JEICompat.showCraftingRecipes()) return;
        if (inventory.recipeBookGui.isVisible()) return;
        inventory.recipeBookGui.toggleVisibility();
        // Re-fire InitGuiEvent so Baubles and other mods restore their buttons after recentering.
        inventory.setWorldAndResolution(mc, inventory.width, inventory.height);
    }
}