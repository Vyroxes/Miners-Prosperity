package net.vyroxes.minersprosperity.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.vyroxes.minersprosperity.block.machine.BlockMachine;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.inventory.slot.SlotState;
import net.vyroxes.minersprosperity.machine.SidedIngredientHandler;
import net.vyroxes.minersprosperity.machine.upgrade.MachineUpgradeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TileEntityStorageCrateController extends TileEntityMachine
{
    private final List<TileEntityStorageCrate> crates = new ArrayList<>();
    private final List<TileEntityStorageCrate> slotCrates = new ArrayList<>();
    private final List<Integer> slotIds = new ArrayList<>();
    private final IItemHandler[] handlers = new IItemHandler[7];
    private boolean assignEmpty;
    private long nextScan;
    private int knownCount;
    private int scannedRange = -1;
    private long[] connectedPositions = new long[0];
    private int transferCursor;
    private final int[] outputCursors = new int[6];
    private boolean transferring;

    public TileEntityStorageCrateController()
    {
        super(new Builder().setItemInput(1).setItemOutput(1).setItemUpgrade(1)
                .setSidedIngredientHandler(new SidedIngredientHandler.Builder()));
        for (int i = 0; i < handlers.length; i++) handlers[i] = new StorageHandler(i < 6 ? EnumFacing.byIndex(i) : null);
    }

    @Override
    public MachineUpgradeType getUpgradeType(int index)
    {
        return index == 0 ? MachineUpgradeType.RANGE : null;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return new TextComponentTranslation("tile.storage_crate_controller.name");
    }

    @Override
    public boolean supportsRange()
    {
        return true;
    }

    @Override
    protected int getBaseRange()
    {
        return ModConfig.BLOCKS.storageControllerBaseRange;
    }

    @Override
    protected int getRangePerUpgrade()
    {
        return ModConfig.BLOCKS.storageControllerRangePerUpgrade;
    }

    @Override
    protected void onRangeChanged()
    {
        nextScan = 0;
    }

    public boolean assignsEmpty()
    {
        return assignEmpty;
    }
    public int connectedCount()
    {
        return knownCount;
    }

    @Override
    public long[] rangeHighlights()
    {
        return connectedPositions;
    }

    public void toggleAssignEmpty()
    {
        assignEmpty = !assignEmpty;
        changed();
    }

    private void changed()
    {
        markDirty();
        if (world == null || world.isRemote) return;
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    private boolean available(TileEntityStorageCrate crate)
    {
        BlockPos target = crate.getPos();
        return !crate.isInvalid() && world != null && world.isBlockLoaded(target)
                && world.getTileEntity(target) == crate
                && isInRange(target);
    }

    private void refreshNetwork()
    {
        if (world == null || world.isRemote) return;
        int radius = range();
        long tick = world.getTotalWorldTime();
        if (tick < nextScan && radius == scannedRange) return;
        nextScan = tick + 20;
        scannedRange = radius;
        List<TileEntityStorageCrate> found = new ArrayList<>();
        for (int x = (pos.getX() - radius) >> 4; x <= (pos.getX() + radius) >> 4; x++)
            for (int z = (pos.getZ() - radius) >> 4; z <= (pos.getZ() + radius) >> 4; z++)
            {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) continue;
                for (TileEntity tile : chunk.getTileEntityMap().values())
                    if (tile instanceof TileEntityStorageCrate && available((TileEntityStorageCrate) tile))
                        found.add((TileEntityStorageCrate) tile);
            }
        found.sort(Comparator.comparingLong(crate -> crate.getPos().toLong()));
        crates.clear();
        crates.addAll(found.subList(0, Math.min(found.size(), ModConfig.BLOCKS.storageControllerMaxCrates)));
        slotCrates.clear();
        slotIds.clear();
        long[] positions = new long[crates.size()];
        for (int i = 0; i < crates.size(); i++)
        {
            TileEntityStorageCrate crate = crates.get(i);
            positions[i] = crate.getPos().toLong();
            for (int slot = 0; slot < crate.slots(); slot++)
            {
                slotCrates.add(crate);
                slotIds.add(slot);
            }
        }
        knownCount = crates.size();
        if (!java.util.Arrays.equals(connectedPositions, positions))
        {
            connectedPositions = positions;
            changed();
        }
        BlockMachine.setMachineState(crates.isEmpty() ? BlockMachine.MachineState.OFF : BlockMachine.MachineState.ACTIVE, world, pos);
    }

    @Override
    public void update()
    {
        if (world == null || world.isRemote) return;
        refreshNetwork();
        if (crates.isEmpty() || !canMachineOperate() || world.getTotalWorldTime() % 4 != 0) return;
        for (EnumFacing relative : EnumFacing.VALUES)
        {
            EnumFacing side = getWorldSide(getMachineFacing(), relative);
            BlockPos neighbour = pos.offset(side);
            if (!world.isBlockLoaded(neighbour)) continue;
            TileEntity target = world.getTileEntity(neighbour);
            if (target == null || target instanceof TileEntityStorageCrateController || target instanceof TileEntityStorageCrate) continue;
            IItemHandler other = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (other == null) continue;
            SidedIngredientHandler config = sidedIngredientHandlers[relative.ordinal()];
            StorageHandler handler = (StorageHandler) handlers[side.ordinal()];
            if (config.getSlotMode(0) == SlotState.SlotMode.AUTO_INPUT)
            {
                int size = other.getSlots();
                for (int checked = 0; checked < Math.min(size, 64); checked++)
                {
                    int slot = Math.floorMod(transferCursor++, size);
                    ItemStack offered = other.extractItem(slot, 64, true);
                    if (offered.isEmpty()) continue;
                    int accepted = offered.getCount() - handler.insertItem(0, offered, true).getCount();
                    if (accepted <= 0) continue;
                    ItemStack extracted = other.extractItem(slot, accepted, false);
                    ItemStack remainder = handler.insertItem(0, extracted, false);
                    if (!remainder.isEmpty())
                    {
                        remainder = other.insertItem(slot, remainder, false);
                        if (!remainder.isEmpty()) net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, remainder);
                    }
                    break;
                }
            }
            if (config.getSlotMode(1) == SlotState.SlotMode.AUTO_OUTPUT)
                for (int checked = 0, size = handler.getSlots() - 1; checked < Math.min(size, 64); checked++)
                {
                    int slot = 1 + Math.floorMod(outputCursors[side.ordinal()]++, size);
                    ItemStack offered = handler.extractItem(slot, 64, true);
                    if (offered.isEmpty()) continue;
                    int accepted = offered.getCount() - ItemHandlerHelper.insertItemStacked(other, offered, true).getCount();
                    if (accepted <= 0) continue;
                    ItemStack extracted = handler.extractItem(slot, accepted, false);
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(other, extracted, false);
                    if (!remainder.isEmpty())
                    {
                        remainder = insertNetwork(remainder, false);
                        if (!remainder.isEmpty()) net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, remainder);
                    }
                    break;
                }
        }
    }

    private boolean permits(EnumFacing side, boolean input)
    {
        if (!canMachineOperate()) return false;
        if (side == null) return true;
        EnumFacing relative = getRelativeSide(getMachineFacing(), side);
        SlotState.SlotMode mode = sidedIngredientHandlers[relative.ordinal()].getSlotMode(input ? 0 : 1);
        return mode == SlotState.SlotMode.INPUT_OUTPUT || mode == (input ? SlotState.SlotMode.INPUT : SlotState.SlotMode.OUTPUT)
                || mode == (input ? SlotState.SlotMode.AUTO_INPUT : SlotState.SlotMode.AUTO_OUTPUT);
    }

    public ItemStack insertNetwork(ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty() || transferring) return stack;
        refreshNetwork();
        transferring = true;
        try
        {
            ItemStack remainder = stack;
            for (int pass = 0; pass < (assignEmpty ? 2 : 1); pass++)
                for (TileEntityStorageCrate crate : crates)
                {
                    if (!available(crate)) continue;
                    int limit = crate.isCompacting() ? 1 : crate.slots();
                    for (int slot = 0; slot < limit; slot++)
                    {
                        boolean empty = crate.template(slot).isEmpty();
                        if (empty != (pass == 1) || empty && crate.locked(slot)) continue;
                        remainder = crate.insert(slot, remainder, simulate);
                        if (remainder.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            return remainder;
        }
        finally { transferring = false; }
    }

    public boolean isCreativeSlot(int slot)
    {
        return slot > 0 && slot <= slotCrates.size() && available(slotCrates.get(slot - 1))
                && slotCrates.get(slot - 1).creative();
    }

    private class StorageHandler implements IItemHandler
    {
        private final EnumFacing side;
        private StorageHandler(EnumFacing side)
        {
            this.side = side;
        }

        @Override
        public int getSlots()
        {
            refreshNetwork(); return slotCrates.size() + 1;
        }

        private boolean valid(int slot)
        {
            return slot > 0 && slot <= slotCrates.size() && available(slotCrates.get(slot - 1))
                    && slotIds.get(slot - 1) < slotCrates.get(slot - 1).slots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot)
        {
            if (!valid(slot) || !permits(side, false)) return ItemStack.EMPTY;
            return slotCrates.get(slot - 1).inventory().getStackInSlot(slotIds.get(slot - 1));
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
        {
            return slot == 0 && permits(side, true) ? insertNetwork(stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (!valid(slot) || !permits(side, false)) return ItemStack.EMPTY;
            return slotCrates.get(slot - 1).extract(slotIds.get(slot - 1), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot == 0 || valid(slot) ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return slot == 0 && permits(side, true);
        }
    }

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, EnumFacing side)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, EnumFacing side)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handlers[side == null ? 6 : side.ordinal()])
                : super.getCapability(capability, side);
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setBoolean("AssignEmpty", assignEmpty);
        int[] positions = new int[connectedPositions.length * 3];
        for (int i = 0; i < connectedPositions.length; i++)
        {
            BlockPos target = BlockPos.fromLong(connectedPositions[i]);
            positions[i * 3] = target.getX();
            positions[i * 3 + 1] = target.getY();
            positions[i * 3 + 2] = target.getZ();
        }
        tag.setIntArray("ConnectedCrates", positions);
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        assignEmpty = tag.getBoolean("AssignEmpty");
        int[] positions = tag.getIntArray("ConnectedCrates");
        connectedPositions = new long[Math.min(512, positions.length / 3)];
        for (int i = 0; i < connectedPositions.length; i++)
            connectedPositions[i] = new BlockPos(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]).toLong();
        knownCount = connectedPositions.length;
        nextScan = 0;
    }
}