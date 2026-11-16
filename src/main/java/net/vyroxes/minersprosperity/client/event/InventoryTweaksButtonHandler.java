package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.client.gui.GuiBackpack;

import java.util.List;

public class InventoryTweaksButtonHandler
{
    private static final int[] BUTTON_ORDER = {54696387, 54696389, 54696388, 54696386};

    private InventoryTweaksButtonHandler()
    {
    }

    public static void positionButtons(GuiContainer gui, List<GuiButton> buttons)
    {
        if (!Loader.isModLoaded("inventorytweaks")) return;

        int count = 0;
        for (GuiButton button : buttons)
        {
            if (button.visible && button.id >= 54696386 && button.id < 54696390) count++;
        }
        int x = gui.getGuiLeft() + gui.getXSize() - 7 - count * 12;
        int y = gui.getGuiTop() + (gui instanceof GuiBackpack ? 27 : 4);
        for (int id : BUTTON_ORDER)
        {
            for (GuiButton button : buttons)
            {
                if (!button.visible || button.id != id) continue;
                button.x = x;
                button.y = y;
                x += 12;
                break;
            }
        }
    }
}