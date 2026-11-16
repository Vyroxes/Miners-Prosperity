package net.vyroxes.minersprosperity.item.backpack;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.inventory.slot.SlotBackpack;
import org.jetbrains.annotations.NotNull;

public class BackpackCapabilityProvider implements ICapabilitySerializable<NBTTagCompound>
{
    private final Inventory inventory;
    private final ItemStackHandler configInventory;

    public BackpackCapabilityProvider(ItemStack owner, int mainSize, int configSize)
    {
        inventory = new Inventory(owner, mainSize);
        configInventory = new ItemStackHandler(configSize)
        {
            @Override
            protected void onContentsChanged(int slot)
            {
                BackpackSettings.data(owner).setBoolean("AutomationDirty", true);
            }
        };
    }

    public static class Inventory extends ItemStackHandler
    {
        public final ItemStackHandler manual;
        private final ItemStack owner;
        public Runnable changed = () -> {};

        Inventory(ItemStack owner, int size)
        {
            this.owner = owner;
            manual = new ItemStackHandler(size)
            {
                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack)
                {
                    return SlotBackpack.isAllowed(stack);
                }

                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
                {
                    return stack.isEmpty() || isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
                }

                @Override
                protected void onContentsChanged(int slot)
                {
                    BackpackSettings.data(owner).setBoolean("AutomationDirty", true);
                    Inventory.this.changed.run();
                }
            };
        }

        @Override
        public int getSlots()
        {
            return manual.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot)
        {
            return manual.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return manual.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return !BackpackSettings.locked(owner, slot) && manual.isItemValid(slot, stack);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
        {
            return isItemValid(slot, stack) ? manual.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return BackpackSettings.locked(owner, slot) ? ItemStack.EMPTY : manual.extractItem(slot, amount, simulate);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack)
        {
            if (!BackpackSettings.locked(owner, slot) && (stack.isEmpty() || manual.isItemValid(slot, stack)))
                manual.setStackInSlot(slot, stack);
        }
    }

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (facing == null || facing == EnumFacing.DOWN);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, EnumFacing facing)
    {
        if (!hasCapability(capability, facing)) return null;
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(facing == EnumFacing.DOWN ? configInventory : inventory);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Inventory", inventory.manual.serializeNBT());
        tag.setTag("ConfigurationInventory", configInventory.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        if (nbt.hasKey("Inventory", 10)) loadInventory(inventory.manual, nbt.getCompoundTag("Inventory"));
        if (nbt.hasKey("ConfigurationInventory", 10))
        {
            NBTTagCompound filter = nbt.getCompoundTag("ConfigurationInventory").copy();
            filter.setInteger("Size", configInventory.getSlots());
            configInventory.deserializeNBT(filter);
        }
        BackpackSettings.data(inventory.owner).setBoolean("AutomationDirty", true);
    }

    private static void loadInventory(ItemStackHandler inventory, NBTTagCompound saved)
    {
        NBTTagCompound normalized = saved.copy();

        normalized.setInteger("Size", Math.max(inventory.getSlots(), Math.min(150, saved.getInteger("Size"))));
        inventory.deserializeNBT(normalized);
    }

    public ItemStackHandler getMainInventory()
    {
        return inventory.manual;
    }
    public ItemStackHandler getConfigInventory()
    {
        return configInventory;
    }
}