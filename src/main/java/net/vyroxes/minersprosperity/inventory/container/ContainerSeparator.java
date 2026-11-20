package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.block.state.IBlockState;
import net.vyroxes.minersprosperity.inventory.slot.SlotMachineOutput;
import net.vyroxes.minersprosperity.machine.recipe.SeparatorRecipe;
import net.vyroxes.minersprosperity.tile.TileEntitySeparator;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.Objects;

public class ContainerSeparator extends Container
{
    private final TileEntityMachine tileEntity;
    private final int[] lastValues = new int[9];
    private final int[] clientValues = new int[9];
    private FluidStack lastFluid;
    private boolean fluidInitialized;

    public ContainerSeparator(InventoryPlayer playerInventory, TileEntityMachine tileEntity)
    {
        this.tileEntity = tileEntity;
        Arrays.fill(this.lastValues, -1);
        IItemHandler handler = tileEntity.getCustomItemStackHandler();
        this.addSlotToContainer(new SlotItemHandler(handler, tileEntity.getInputSlotId(0), 29, 35));
        this.addSlotToContainer(new SlotItemHandler(handler, tileEntity.getInputSlotId(1), 49, 25));
        this.addSlotToContainer(new SlotItemHandler(handler, tileEntity.getEnergySlotId(0), 8, 53));
        this.addSlotToContainer(new SlotMachineOutput(handler, tileEntity.getOutputSlotId(0), 119, 35, tileEntity));
        this.addSlotToContainer(new SlotItemHandler(handler, tileEntity.getOutputSlotId(1), 49, 45));

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++)
        {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void addListener(@NotNull IContainerListener listener)
    {
        super.addListener(listener);
        Arrays.fill(this.lastValues, -1);
        this.fluidInitialized = false;
        this.detectAndSendChanges();
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();
        FluidStack fluid = Objects.requireNonNull(this.tileEntity.getFluidTank(0)).getFluid();
        if (!this.fluidInitialized || (fluid == null ? this.lastFluid != null : this.lastFluid == null || !fluid.isFluidEqual(this.lastFluid) || fluid.amount != this.lastFluid.amount))
        {
            if (!this.tileEntity.getWorld().isRemote)
            {
                IBlockState state = this.tileEntity.getWorld().getBlockState(this.tileEntity.getPos());
                this.tileEntity.getWorld().notifyBlockUpdate(this.tileEntity.getPos(), state, state, 2);
            }
            this.lastFluid = fluid == null ? null : fluid.copy();
            this.fluidInitialized = true;
        }

        int[] values = {this.tileEntity.getCookTime(), this.tileEntity.getTotalCookTime(),
                this.tileEntity.getEnergyStored(), this.tileEntity.getMaxEnergyStored(),
                this.tileEntity.getMaxReceive(), this.tileEntity.getMaxExtract(),
                this.tileEntity.getEnergyUsage(), this.tileEntity.getEnergyGeneration(), this.tileEntity.getFluidStored()};

        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == this.lastValues[i]) continue;

            for (IContainerListener listener : this.listeners)
            {
                listener.sendWindowProperty(this, i * 2, values[i] & 0xFFFF);
                listener.sendWindowProperty(this, i * 2 + 1, values[i] >>> 16);
            }

            this.lastValues[i] = values[i];
        }
    }

    @Override
    public void updateProgressBar(int id, int data)
    {
        if (id < 0 || id >= this.clientValues.length * 2) return;

        int field = id / 2;
        if (id % 2 == 0) this.clientValues[field] = (this.clientValues[field] & 0xFFFF0000) | (data & 0xFFFF);
        else this.clientValues[field] = (this.clientValues[field] & 0xFFFF) | ((data & 0xFFFF) << 16);

        if (id % 2 == 1)
        {
            this.tileEntity.setField(field, this.clientValues[field]);
        }
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player)
    {
        return this.tileEntity.isUsableByPlayer(player);
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index)
    {
        Slot slot = this.inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < 5) moved = this.mergeItemStack(stack, 5, 41, index >= 3);
        else if (SeparatorRecipe.isInputValid(stack)) moved = this.mergeItemStack(stack, 0, 1, false);
        else if (TileEntitySeparator.isFluidContainer(stack)) moved = this.mergeItemStack(stack, 1, 2, false);
        else if (this.tileEntity.getCustomItemStackHandler().isEnergyItemValid(stack)) moved = this.mergeItemStack(stack, 2, 3, false);
        else if (index < 32) moved = this.mergeItemStack(stack, 32, 41, false);
        else moved = this.mergeItemStack(stack, 5, 32, false);

        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (index == 3) slot.onSlotChange(stack, original);
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}