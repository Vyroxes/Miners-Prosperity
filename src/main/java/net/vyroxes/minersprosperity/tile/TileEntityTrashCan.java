package net.vyroxes.minersprosperity.tile;

import net.minecraft.util.EnumFacing;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import net.vyroxes.minersprosperity.energy.CustomEnergyStorage;
import net.vyroxes.minersprosperity.fluid.CustomFluidTank;
import net.vyroxes.minersprosperity.inventory.slot.SlotState;
import net.vyroxes.minersprosperity.machine.SidedIngredientHandler;

public class TileEntityTrashCan extends TileEntityMachine
{
    private final int[] disposalModes = {1, 1, 1};

    public TileEntityTrashCan()
    {
        super(new Builder().setItemInput(1).setItemOutput(1).setEnergy(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0)
                .addFluidTank(null, Integer.MAX_VALUE, 0, false, true, SlotState.SlotType.INPUT, SlotState.SlotMode.INPUT)
                .setSidedIngredientHandler(new SidedIngredientHandler.Builder().setEnergyHandler(SlotState.SlotMode.INPUT)));
        fluidTanks[0] = new CustomFluidTank(null, 0, Integer.MAX_VALUE, false, true)
        {
            @Override
            public int fill(FluidStack resource, boolean doFill)
            {
                return resource != null && resource.amount > 0 && getDisposalMode(2) != 0 && canDispose() ? resource.amount : 0;
            }
        };
    }

    public boolean canDispose()
    {
        return world == null ? redstoneControlButtonState == 0 : canMachineOperate();
    }

    public int getDisposalMode(int medium)
    {
        return disposalModes[medium];
    }

    public void setDisposalMode(int medium, int mode)
    {
        if (medium < 0 || medium >= disposalModes.length || mode < 0 || mode > (medium == 0 ? 1 : 2)) return;
        disposalModes[medium] = mode;
        if (world != null && world.isRemote) NetworkHandler.sendVariableUpdate(4 + medium, mode, pos);
        markDirty();
        if (world != null && !world.isRemote)
        {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    public boolean preservesContainer(ItemStack stack)
    {
        return !stack.isEmpty() && (disposalModes[1] == 2 && stack.hasCapability(CapabilityEnergy.ENERGY, null)
                || disposalModes[2] == 2 && FluidUtil.getFluidHandler(stack) != null);
    }

    public boolean acceptsItem(ItemStack stack)
    {
        if (stack.isEmpty() || !canDispose()) return false;
        return preservesContainer(stack) || disposalModes[0] != 0;
    }

    public void processInput()
    {
        if (!canDispose()) return;
        ItemStack input = getInputStack(0);
        if (input.isEmpty()) return;
        if (!preservesContainer(input))
        {
            if (disposalModes[0] != 0) customItemStackHandler.setStackInSlot(0, ItemStack.EMPTY);
            return;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        boolean remaining = false;
        if (disposalModes[1] == 2)
        {
            IEnergyStorage energy = result.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null)
            {
                energy.extractEnergy(Integer.MAX_VALUE, false);
                remaining = energy.getEnergyStored() > 0;
            }
        }
        if (disposalModes[2] == 2)
        {
            IFluidHandlerItem fluid = FluidUtil.getFluidHandler(result);
            if (fluid != null)
            {
                fluid.drain(Integer.MAX_VALUE, true);
                result = fluid.getContainer();
                for (net.minecraftforge.fluids.capability.IFluidTankProperties tank : fluid.getTankProperties())
                {
                    FluidStack contents = tank.getContents();
                    if (contents != null && contents.amount > 0) remaining = true;
                }
            }
        }
        // Keep partially drained containers in the input: some items limit extraction per call.
        if (remaining)
        {
            customItemStackHandler.setStackInSlot(0, result);
            return;
        }
        ItemStack output = getOutputStack(0);
        if (!result.isEmpty() && !output.isEmpty()
                && (!ItemStack.areItemsEqual(output, result) || !ItemStack.areItemStackTagsEqual(output, result)
                || output.getCount() + result.getCount() > Math.min(output.getMaxStackSize(), customItemStackHandler.getSlotLimit(1))))
        {
            return;
        }
        customItemStackHandler.setStackInSlot(0, ItemStack.EMPTY);
        if (output.isEmpty()) customItemStackHandler.setStackInSlot(1, result);
        else if (!result.isEmpty())
        {
            output.grow(result.getCount());
            markDirty();
        }
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setIntArray("DisposalModes", disposalModes);
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        if (tag.getCompoundTag("Inventory").getInteger("Size") == 1)
        {
            // Inserting an output slot shifts the fluid and energy configuration indices in older saves.
            tag = tag.copy();
            tag.getCompoundTag("Inventory").setInteger("Size", 2);
            NBTTagCompound states = tag.getCompoundTag("States");
            for (EnumFacing side : EnumFacing.VALUES)
            {
                String key = side.toString() + "SlotStates";
                NBTTagList previous = states.getTagList(key, 10);
                if (previous.tagCount() != 3) continue;
                NBTTagList updated = new NBTTagList();
                updated.appendTag(previous.getCompoundTagAt(0));
                NBTTagCompound output = new NBTTagCompound();
                sidedIngredientHandlers[side.getIndex()].getSlotState(1).writeToNBT(output);
                updated.appendTag(output);
                updated.appendTag(previous.getCompoundTagAt(1));
                updated.appendTag(previous.getCompoundTagAt(2));
                states.setTag(key, updated);
            }
        }
        super.readFromNBT(tag);
        int[] modes = tag.getIntArray("DisposalModes");
        for (int i = 0; i < disposalModes.length; i++)
            disposalModes[i] = i < modes.length ? Math.max(0, Math.min(i == 0 ? 1 : 2, modes[i])) : 1;
    }

    @Override
    protected CustomEnergyStorage createEnergyStorage(int capacity, int maxReceive, int maxExtract, int stored)
    {
        return new CustomEnergyStorage(capacity, maxReceive, 0, 0)
        {
            @Override
            public int receiveEnergy(int amount, boolean simulate)
            {
                return getDisposalMode(1) != 0 && canDispose() ? Math.max(0, amount) : 0;
            }
        };
    }

    @Override
    public void update()
    {
        if (world == null || world.isRemote || !canDispose()) return;
        processInput();
        int fluidState = getFluidTankStateId(0);
        for (EnumFacing side : EnumFacing.VALUES)
        {
            SidedIngredientHandler handler = sidedIngredientHandlers[side.getIndex()];
            if (handler.getSlotMode(0) == SlotState.SlotMode.AUTO_INPUT) handler.autoInput();
            if (handler.getSlotMode(1) == SlotState.SlotMode.AUTO_OUTPUT) handler.autoOutput();
            if (handler.getSlotMode(fluidState) == SlotState.SlotMode.AUTO_INPUT) handler.autoInputFluid();
        }
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return hasCustomName() ? new TextComponentString(customName) : new TextComponentTranslation("container.trash_can");
    }
}