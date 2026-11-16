package net.vyroxes.minersprosperity.integration.jei;

import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.client.gui.GuiBackpackConfiguration;
import net.vyroxes.minersprosperity.inventory.slot.SlotFilter;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageBackpackFilter;
import org.jetbrains.annotations.NotNull;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackpackGhostIngredientHandler implements IGhostIngredientHandler<GuiBackpackConfiguration>
{
    @Override
    public <I> @NotNull List<Target<I>> getTargets(@NotNull GuiBackpackConfiguration gui, @NotNull I ingredient, boolean doStart)
    {
        if (gui.isEditingFilter() || !(ingredient instanceof ItemStack stack) || stack.isEmpty()) return Collections.emptyList();
        List<Target<I>> targets = new ArrayList<>();
        for (Slot slot : gui.inventorySlots.inventorySlots)
        {
            if (!(slot instanceof SlotFilter)) continue;
            targets.add(new Target<I>()
            {
                @Override
                public @NotNull Rectangle getArea()
                {
                    return new Rectangle(gui.getGuiLeft() + slot.xPos, gui.getGuiTop() + slot.yPos, 16, 16);
                }

                @Override
                public void accept(@NotNull I ingredient)
                {
                    if (gui.isEditingFilter() || !(ingredient instanceof ItemStack stack) || stack.isEmpty()) return;
                    NetworkHandler.NETWORK.sendToServer(new MessageBackpackFilter(gui.inventorySlots.windowId, slot.slotNumber, stack));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete()
    {
    }
}