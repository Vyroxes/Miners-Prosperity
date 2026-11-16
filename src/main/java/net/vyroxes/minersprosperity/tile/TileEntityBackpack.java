package net.vyroxes.minersprosperity.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.vyroxes.minersprosperity.item.backpack.BackpackAutomation;
import net.vyroxes.minersprosperity.item.backpack.BackpackSettings;
import org.jetbrains.annotations.NotNull;

public class TileEntityBackpack extends TileEntity implements ITickable
{
    private ItemStack backpack = ItemStack.EMPTY;

    public ItemStack getBackpack()
    {
        return backpack;
    }

    public void setBackpack(ItemStack stack)
    {
        backpack = stack;
        watchChanges();
        BackpackSettings.data(backpack).setBoolean("IsOpen", false);
        BackpackSettings.data(backpack).setBoolean("LockEdit", false);
        markDirty();
        if (world != null) world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
    }

    private void watchChanges()
    {
        net.minecraftforge.items.IItemHandler handler = backpack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof net.vyroxes.minersprosperity.item.backpack.BackpackCapabilityProvider.Inventory)
            ((net.vyroxes.minersprosperity.item.backpack.BackpackCapabilityProvider.Inventory) handler).changed = this::markDirty;
    }

    @Override
    public void update()
    {
        if (world.isRemote || backpack.isEmpty() || world.getTotalWorldTime() % 20 != 0) return;
        if (BackpackSettings.data(backpack).getBoolean("AutomationDirty"))
        {
            BackpackAutomation.process(backpack, world);
            markDirty();
        }
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setTag("Backpack", backpack.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        backpack = new ItemStack(tag.getCompoundTag("Backpack"));
        watchChanges();
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

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return backpack.getCapability(capability, null);
        return super.getCapability(capability, facing);
    }
}