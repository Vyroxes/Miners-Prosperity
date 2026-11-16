package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.vyroxes.minersprosperity.tile.TileEntityTrashCan;
import org.jetbrains.annotations.NotNull;

public class ContainerTrashCan extends Container
{
    private final TileEntityTrashCan tile;

    public ContainerTrashCan(InventoryPlayer inventory, TileEntityTrashCan tile)
    {
        this.tile = tile;
        addSlotToContainer(new SlotItemHandler(tile.getCustomItemStackHandler(), 0, 80, 29));
        addSlotToContainer(new SlotItemHandler(tile.getCustomItemStackHandler(), 1, 80, 49)
        {
            @Override
            public boolean isItemValid(@NotNull ItemStack stack)
            {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
    }

    @Override
    public void addListener(@NotNull net.minecraft.inventory.IContainerListener listener)
    {
        super.addListener(listener);
        if (listener instanceof net.minecraft.entity.player.EntityPlayerMP player)
            player.connection.sendPacket(java.util.Objects.requireNonNull(tile.getUpdatePacket()));
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player)
    {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index)
    {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        if (index < 2)
        {
            if (!mergeItemStack(stack, 2, inventorySlots.size(), true)) return ItemStack.EMPTY;
            slot.putStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
            slot.onTake(player, stack);
            return original;
        }
        ItemStack remaining = tile.getCustomItemStackHandler().insertItem(0, stack, false);
        if (remaining.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.putStack(remaining);
        slot.onTake(player, original);
        return original;
    }
}