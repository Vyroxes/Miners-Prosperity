package net.vyroxes.minersprosperity.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.inventory.ItemStackHelper;
import net.vyroxes.minersprosperity.inventory.container.ContainerWorktable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TileEntityWorktable extends TileEntity
{
    private final List<ContainerWorktable> viewers = new ArrayList<>();
    private boolean needsSync;
    private boolean loading;
    public final InventoryCrafting grid = new InventoryCrafting(new Container()
    {
        @Override
        public boolean canInteractWith(@NotNull EntityPlayer player)
        {
            return false;
        }

        @Override
        public void onCraftMatrixChanged(@NotNull IInventory inventory)
        {
            changed();
        }
    }, 3, 3)
    {
        @Override
        public void markDirty()
        {
            changed();
        }

        @Override
        public void clear()
        {
            super.clear(); changed();
        }
    };

    private void changed()
    {
        if (loading) return;
        markDirty();
        needsSync = true;
        for (ContainerWorktable viewer : viewers) viewer.onCraftMatrixChanged(grid);
    }

    public void open(ContainerWorktable container)
    {
        viewers.add(container);
    }
    public void close(ContainerWorktable container)
    {
        viewers.remove(container);
    }

    public void sync()
    {
        if (!needsSync || world == null || world.isRemote) return;
        needsSync = false;
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) stacks.set(i, grid.getStackInSlot(i));
        ItemStackHelper.saveAllItems(tag, stacks);
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, stacks);
        loading = true;
        try
        {
            for (int i = 0; i < 9; i++) grid.setInventorySlotContents(i, stacks.get(i));
        }
        finally { loading = false; }
    }

    @Override
    public @NotNull NBTTagCompound getUpdateTag()
    {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(@NotNull NetworkManager manager, SPacketUpdateTileEntity packet)
    {
        readFromNBT(packet.getNbtCompound());
    }
}