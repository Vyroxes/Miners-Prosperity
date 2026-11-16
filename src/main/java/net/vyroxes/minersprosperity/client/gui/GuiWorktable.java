package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.vyroxes.minersprosperity.inventory.container.ContainerWorktable;
import net.vyroxes.minersprosperity.tile.TileEntityWorktable;

public class GuiWorktable extends GuiCrafting
{
    public GuiWorktable(InventoryPlayer inventory, TileEntityWorktable tile)
    {
        super(inventory, tile.getWorld(), tile.getPos());
        inventorySlots = new ContainerWorktable(inventory, tile);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        fontRenderer.drawString(I18n.format("tile.worktable.name"), 28, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 4210752);
    }
}