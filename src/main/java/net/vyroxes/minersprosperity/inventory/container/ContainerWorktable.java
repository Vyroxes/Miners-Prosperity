package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.tile.TileEntityWorktable;
import org.jetbrains.annotations.NotNull;

public class ContainerWorktable extends ContainerWorkbench
{
    private final TileEntityWorktable tile;

    public ContainerWorktable(InventoryPlayer inventory, TileEntityWorktable tile)
    {
        super(inventory, tile.getWorld(), tile.getPos());
        this.tile = tile;
        craftMatrix = tile.grid;
        inventorySlots.clear();
        inventoryItemStacks.clear();
        addSlotToContainer(new SlotCrafting(inventory.player, craftMatrix, craftResult, 0, 124, 35));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlotToContainer(new Slot(craftMatrix, col + row * 3, 30 + col * 18, 17 + row * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
        tile.open(this);
        onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player)
    {
        return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();
        tile.sync();
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        tile.close(this);
        tile.sync();

        if (!player.world.isRemote && !player.inventory.getItemStack().isEmpty())
        {
            player.dropItem(player.inventory.getItemStack(), false);
            player.inventory.setItemStack(ItemStack.EMPTY);
        }
    }
}