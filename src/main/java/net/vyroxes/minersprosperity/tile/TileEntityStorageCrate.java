package net.vyroxes.minersprosperity.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.*;
import net.vyroxes.minersprosperity.block.BlockStorageCrate;
import net.vyroxes.minersprosperity.inventory.slot.SlotState;
import net.vyroxes.minersprosperity.machine.SidedIngredientHandler;
import net.vyroxes.minersprosperity.storage.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.IntStream;

public class TileEntityStorageCrate extends TileEntityMachine
{
    private StorageCrateTier tier = StorageCrateTier.BASIC;
    private int slots = 1;
    private boolean compacting;
    private final ItemStack[] templates =
    {
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private final long[] counts = new long[4];
    private final long[] ratios = {1, 1, 1, 1};
    private final boolean[] locked = new boolean[4];
    private final int[] sides = {3, 3, 3, 3, 3, 3};

    private long units;
    private int redstoneMode;
    private int lastSignal;
    public int revision;
    public UUID lastPlayer;
    public long lastClick = -100;
    public int lastSlot;
    public ItemStack lastItem = ItemStack.EMPTY;
    private final IItemHandler[] handlers = new IItemHandler[7];

    public TileEntityStorageCrate()
    {
        super(
                new Builder()
                        .setItemInput(4)
                        .setSidedIngredientHandler(new SidedIngredientHandler.Builder()));
        initHandlers();
    }

    public TileEntityStorageCrate(@NotNull StorageCrateTier tier, int slots, boolean compacting)
    {
        this();
        this.tier = tier;
        this.slots = slots;
        this.compacting = compacting;
    }

    public @NotNull StorageCrateTier getTier()
    {
        return tier;
    }

    public int slots()
    {
        if (!compacting) return slots;
        for (int i = templates.length - 1; i >= 0; i--)
        {
            if (!templates[i].isEmpty()) return i + 1;
        }
        return 1;
    }

    public boolean isCompacting()
    {
        return compacting;
    }

    public boolean creative()
    {
        return tier == StorageCrateTier.CREATIVE;
    }

    public @NotNull ItemStack template(int slot)
    {
        return slot >= 0 && slot < slots() ? templates[slot].copy() : ItemStack.EMPTY;
    }

    public boolean locked(int slot)
    {
        return locked[slot];
    }

    public int redstoneMode()
    {
        return redstoneMode;
    }

    public int side(int side)
    {
        return sides[side];
    }

    public void cycleSide(int side)
    {
        if (side < 0 || side >= 6) return;
        sides[side] = (sides[side] + 1) % 4;
        applySideMode(side);
        changed();
    }

    private void applySideMode(int face)
    {
        SlotState.SlotMode[] modes =
        {
            SlotState.SlotMode.DISABLED,
            SlotState.SlotMode.INPUT,
            SlotState.SlotMode.OUTPUT,
            SlotState.SlotMode.INPUT_OUTPUT
        };
        for (int slot = 0; slot < 4; slot++)
            sidedIngredientHandlers[face].getSlotState(slot).setSlotMode(modes[sides[face]]);
    }

    @Override
    public int[] getInputSlotIds()
    {
        return IntStream.range(0, slots()).toArray();
    }

    @Override
    public int getInputSlotCount()
    {
        return slots();
    }

    @Override
    public @NotNull EnumFacing getMachineFacing()
    {
        return world != null && world.getBlockState(pos).getBlock() instanceof BlockStorageCrate
                ? world.getBlockState(pos).getValue(BlockStorageCrate.FACING)
                : EnumFacing.NORTH;
    }

    @Override
    public @NotNull ITextComponent getDisplayName()
    {
        return hasCustomName()
                ? new TextComponentString(customName)
                : new TextComponentTranslation(
                        world == null
                                ? "container.storage_crate"
                                : getBlockType().getTranslationKey() + ".name");
    }

    @Override
    public void update()
    {
        if (world == null || world.isRemote) return;
        for (SidedIngredientHandler handler : sidedIngredientHandlers)
        {
            boolean input = false, output = false;
            for (int slot = 0; slot < slots(); slot++)
            {
                input |= handler.getSlotMode(slot) == SlotState.SlotMode.AUTO_INPUT;
                output |= handler.getSlotMode(slot) == SlotState.SlotMode.AUTO_OUTPUT;
            }
            if (input) handler.autoInput();
            if (output) handler.autoOutput();
        }
    }

    public void cycleRedstone(int direction)
    {
        if (direction == 0)
        {
            redstoneMode = 0;
            changed();
            return;
        }
        if (slots == 1 || compacting)
        {
            int[] modes = {0, 1, 3, 5};
            int selected = 0;
            for (int i = 0; i < modes.length; i++)
            {
                if (modes[i] == redstoneMode) selected = i;
            }
            redstoneMode = modes[Math.floorMod(selected + direction, modes.length)];
        }
        else redstoneMode = Math.floorMod(redstoneMode + direction, 6);
        changed();
    }

    public long count(int slot)
    {
        return templates[slot].isEmpty()
                ? 0
                : creative() ? Long.MAX_VALUE : compacting ? units / ratios[slot] : counts[slot];
    }

    public long capacity(int slot)
    {
        ItemStack item = templates[compacting ? 0 : slot];
        int size = item.isEmpty() ? 64 : item.getMaxStackSize();
        long capacity = Math.max(1, (long) tier.stacks() * size / (compacting ? 1 : slots));
        return compacting ? capacity * ratios[0] / ratios[slot] : capacity;
    }

    public double fill(int slot)
    {
        if (creative()) return templates[slot].isEmpty() ? 0 : 1;
        if (compacting) return Math.min(1, units / (double) Math.max(1, capacity(0) * ratios[0]));
        return Math.min(1, counts[slot] / (double) Math.max(1, capacity(slot)));
    }

    public int signal()
    {
        boolean anyFull = false, allFull = true, anyEmpty = false, allEmpty = true;
        double total = 0;
        int inspected = compacting ? 1 : slots;
        for (int i = 0; i < inspected; i++)
        {
            double fill = fill(i);
            boolean full = fill >= 1,
                    empty =
                            compacting
                                    ? units == 0 && !creative() || templates[0].isEmpty()
                                    : count(i) == 0;
            anyFull |= full;
            allFull &= full;
            anyEmpty |= empty;
            allEmpty &= empty;
            total += fill;
        }
        return switch (redstoneMode)
        {
            case 1 -> anyFull ? 15 : 0;
            case 2 -> allFull ? 15 : 0;
            case 3 -> anyEmpty ? 15 : 0;
            case 4 -> allEmpty ? 15 : 0;
            case 5 -> total <= 0 ? 0 : Math.min(15, 1 + (int) Math.floor(14 * total / inspected));
            default -> 0;
        };
    }

    public void toggleLock(int slot)
    {
        if (slot < 0 || slot >= slots()) return;
        boolean value = !locked[slot];
        if (compacting) Arrays.fill(locked, value);
        else locked[slot] = value;
        clearEmpty();
        changed();
    }

    private void clearEmpty()
    {
        if (compacting)
        {
            if (units == 0 && !creative() && !locked[0])
            {
                Arrays.fill(templates, ItemStack.EMPTY);
                Arrays.fill(ratios, 1);
            }
        }
        else
            for (int i = 0; i < slots(); i++)
                if (counts[i] == 0 && !locked[i]) templates[i] = ItemStack.EMPTY;
    }

    private void setFamily(@NotNull List<ItemStack> family, long[] factors)
    {
        for (int i = 0; i < family.size(); i++)
        {
            templates[i] = family.get(i).copy();
            templates[i].setCount(1);
            ratios[i] = factors[i];
        }
    }

    public @NotNull ItemStack insert(int slot, @NotNull ItemStack input, boolean simulate)
    {
        if (slot < 0 || slot >= slots() || input.isEmpty()) return input;
        long ratio = 1, capacity;
        List<ItemStack> family = null;
        long[] factors = {1, 1, 1, 1};
        int type = slot;
        if (compacting)
        {
            if (templates[0].isEmpty())
            {
                family = CrateCompacting.family(input, world);
                for (int i = family.size() - 2; i >= 0; i--)
                    factors[i] =
                            factors[i + 1]
                                    * CrateCompacting.ratio(
                                            family.get(i), family.get(i + 1), world);
                type = -1;
                for (int i = 0; i < family.size(); i++)
                    if (CrateCompacting.same(family.get(i), input)) type = i;
                if (type < 0) return input;
                ratio = factors[type];
                capacity = (long) tier.stacks() * family.get(0).getMaxStackSize() * factors[0];
            }
            else
            {
                type = -1;
                for (int i = 0; i < slots(); i++)
                    if (CrateCompacting.same(templates[i], input)) type = i;
                if (type < 0) return input;
                ratio = ratios[type];
                capacity = capacity(0) * ratios[0];
            }
        }
        else
        {
            if (!templates[slot].isEmpty() && !CrateCompacting.same(templates[slot], input))
                return input;
            capacity = Math.max(1, (long) tier.stacks() * input.getMaxStackSize() / slots);
        }
        int accepted;
        if (creative()) accepted = (compacting ? templates[0] : templates[slot]).isEmpty() ? 1 : 0;
        else
            accepted =
                    (int)
                            Math.min(
                                    input.getCount(),
                                    Math.max(
                                            0,
                                            (capacity - (compacting ? units : counts[slot]))
                                                    / ratio));
        if (accepted == 0) return input;
        if (!simulate)
        {
            if (compacting)
            {
                if (family != null) setFamily(family, factors);
                units += accepted * ratio;
            }
            else
            {
                if (templates[slot].isEmpty())
                {
                    templates[slot] = input.copy();
                    templates[slot].setCount(1);
                }
                counts[slot] += accepted;
            }
            changed();
        }
        ItemStack remainder = input.copy();
        remainder.shrink(accepted);
        return remainder;
    }

    public @NotNull ItemStack extract(int slot, int amount, boolean simulate)
    {
        if (slot < 0 || slot >= slots() || amount <= 0 || templates[slot].isEmpty())
            return ItemStack.EMPTY;
        int taken =
                (int) Math.min(Math.min(amount, templates[slot].getMaxStackSize()), count(slot));
        if (taken <= 0) return ItemStack.EMPTY;
        ItemStack output = templates[slot].copy();
        output.setCount(taken);
        if (!simulate && !creative())
        {
            if (compacting) units -= taken * ratios[slot];
            else counts[slot] -= taken;
            clearEmpty();
            changed();
        }
        return output;
    }

    public @NotNull ItemStack insertAny(@NotNull ItemStack stack, boolean simulate)
    {
        if (compacting) return insert(0, stack, simulate);
        ItemStack remainder = stack;
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < slots() && !remainder.isEmpty(); i++)
                if ((pass == 0) != templates[i].isEmpty())
                    remainder = insert(i, remainder, simulate);
        return remainder;
    }

    private void initHandlers()
    {
        for (int face = 0; face < 7; face++)
        {
            final int side = face;
            handlers[face] =
                    new IItemHandler()
                    {
                        @Override
                        public int getSlots()
                        {
                            return slots();
                        }

                        @Override
                        public @NotNull ItemStack getStackInSlot(int slot)
                        {
                            ItemStack stack = template(slot);
                            if (!stack.isEmpty())
                                stack.setCount((int) Math.min(Integer.MAX_VALUE, count(slot)));
                            return stack;
                        }

                        @Override
                        public @NotNull ItemStack insertItem(
                                int slot, @NotNull ItemStack stack, boolean simulate)
                        {
                            return side < 6 && (sides[side] & 1) == 0
                                    ? stack
                                    : insert(slot, stack, simulate);
                        }

                        @Override
                        public @NotNull ItemStack extractItem(
                                int slot, int amount, boolean simulate)
                        {
                            return side < 6 && (sides[side] & 2) == 0
                                    ? ItemStack.EMPTY
                                    : extract(slot, amount, simulate);
                        }

                        @Override
                        public int getSlotLimit(int slot)
                        {
                            return (int) Math.min(Integer.MAX_VALUE, capacity(slot));
                        }
                    };
        }
    }

    public @NotNull IItemHandler inventory()
    {
        return handlers[6];
    }

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, EnumFacing facing)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null
                ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handlers[6])
                : super.getCapability(capability, facing);
    }

    public void changed()
    {
        revision++;
        markDirty();
        if (world != null && !world.isRemote)
        {
            updateAppearance();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
            int signal = signal();
            if (signal != lastSignal)
            {
                lastSignal = signal;
                world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
            }
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        updateAppearance();
    }

    private void updateAppearance()
    {
        if (world == null || world.isRemote || !compacting) return;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockStorageCrate
                && state.getValue(BlockStorageCrate.FORMS) != slots())
            world.setBlockState(pos, state.withProperty(BlockStorageCrate.FORMS, slots()), 2);
    }

    public @NotNull NBTTagCompound data()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Tier", tier.ordinal());
        tag.setInteger("Slots", slots());
        tag.setBoolean("Compacting", compacting);
        tag.setLong("Units", units);
        tag.setInteger("Redstone", redstoneMode);
        tag.setIntArray("Sides", sides);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < (compacting ? 4 : slots()); i++)
        {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("Item", templates[i].serializeNBT());
            entry.setLong("Count", counts[i]);
            entry.setLong("Ratio", ratios[i]);
            entry.setBoolean("Locked", locked[i]);
            list.appendTag(entry);
        }
        tag.setTag("Compartments", list);
        tag.setTag("MachineStates", createStatesTag());
        return tag;
    }

    public void readData(@NotNull NBTTagCompound tag, boolean identity)
    {
        if (identity)
        {
            tier = StorageCrateTier.values()[Math.floorMod(tag.getInteger("Tier"), 7)];
            compacting = tag.getBoolean("Compacting");
            slots =
                    compacting
                            ? 4
                            : tag.getInteger("Slots") == 4
                                    ? 4
                                    : tag.getInteger("Slots") == 2 ? 2 : 1;
        }
        units = Math.max(0, tag.getLong("Units"));
        redstoneMode = Math.floorMod(tag.getInteger("Redstone"), 6);
        int[] ports = tag.getIntArray("Sides");
        for (int i = 0; i < 6; i++) sides[i] = ports.length == 6 ? Math.floorMod(ports[i], 4) : 3;
        NBTTagList list = tag.getTagList("Compartments", 10);
        for (int i = 0; i < 4; i++)
        {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            templates[i] = new ItemStack(entry.getCompoundTag("Item"));
            if (!templates[i].isEmpty()) templates[i].setCount(1);
            counts[i] = Math.max(0, entry.getLong("Count"));
            ratios[i] = Math.max(1, Math.min(729, entry.getLong("Ratio")));
            locked[i] = entry.getBoolean("Locked");
        }
        for (int face = 0; face < 6; face++)
        {
            if (tag.hasKey("MachineStates", 10))
                sidedIngredientHandlers[face].readFromNBT(tag.getCompoundTag("MachineStates"));

        }
        lastSignal = signal();
        revision++;
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setTag("StorageCrate", data());
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        readData(tag.getCompoundTag("StorageCrate"), true);
    }

    @Override
    public @NotNull NBTTagCompound getUpdateTag()
    {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public @NotNull SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(
            @NotNull NetworkManager manager, @NotNull SPacketUpdateTileEntity packet)
    {
        readFromNBT(packet.getNbtCompound());
    }
}