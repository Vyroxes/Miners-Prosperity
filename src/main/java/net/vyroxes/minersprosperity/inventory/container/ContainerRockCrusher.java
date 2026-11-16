package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.vyroxes.minersprosperity.inventory.slot.SlotMachineOutput;
import net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;
import org.jetbrains.annotations.NotNull;

public class ContainerRockCrusher extends Container
{
    private final TileEntityMachine tileEntity;
    private final int[] previous = new int[9];
    private final int[] received = new int[9];

    public ContainerRockCrusher(InventoryPlayer playerInventory, TileEntityMachine tileEntity)
    {
        this.tileEntity = tileEntity;
        IItemHandler handler = this.tileEntity.getCustomItemStackHandler();

        this.addSlotToContainer(new SlotItemHandler(handler, 0, 32, 35));
        this.addSlotToContainer(new SlotItemHandler(handler, 1, 8, 53));
        for (int i = 0; i < 3; i++)
            this.addSlotToContainer(new SlotMachineOutput(handler, this.tileEntity.getOutputSlotId(i), 80 + i * 20, 35, this.tileEntity));
        java.util.Arrays.fill(previous, Integer.MIN_VALUE);

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j) this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }

        for (int k = 0; k < 9; ++k) this.addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();
        int[] values = {tileEntity.getCookTime(), tileEntity.getTotalCookTime(), tileEntity.getEnergyStored(),
                tileEntity.getMaxEnergyStored(), tileEntity.getMaxReceive(), tileEntity.getMaxExtract(),
                tileEntity.getEnergyUsage(), tileEntity.getEnergyGeneration(), tileEntity.getFluidStored()};
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == previous[i]) continue;
            // Window properties are signed shorts; send both halves of energy and XP values.
            for (IContainerListener listener : listeners)
            {
                listener.sendWindowProperty(this, i * 2, values[i] & 0xFFFF);
                listener.sendWindowProperty(this, i * 2 + 1, values[i] >>> 16);
            }
            previous[i] = values[i];
        }
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        if (id < 0 || id >= received.length * 2) return;
        int index = id / 2;
        received[index] = id % 2 == 0 ? (received[index] & 0xFFFF0000) | (data & 0xFFFF)
                : (received[index] & 0xFFFF) | ((data & 0xFFFF) << 16);
        tileEntity.setField(index, received[index]);
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer entityPlayer)
    {
        return this.tileEntity.isUsableByPlayer(entityPlayer);
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer entityPlayer, int index)
    {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 5)
        {
            if (!mergeItemStack(stack, 5, 41, true)) return ItemStack.EMPTY;
        }
        else if (RockCrusherRecipe.getInstance().findRecipe(stack) != null)
        {
            if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        else if (tileEntity.getCustomItemStackHandler().isEnergyItemValid(stack))
        {
            if (!mergeItemStack(stack, 1, 2, false)) return ItemStack.EMPTY;
        }
        else if (index < 32)
        {
            if (!mergeItemStack(stack, 32, 41, false)) return ItemStack.EMPTY;
        }
        else if (!mergeItemStack(stack, 5, 32, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(entityPlayer, stack);
        return original;
    }
}