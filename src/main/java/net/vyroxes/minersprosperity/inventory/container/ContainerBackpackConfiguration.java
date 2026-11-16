package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.inventory.slot.SlotFilter;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;

public class ContainerBackpackConfiguration extends Container
{
    protected final ItemStackHandler configurationInventory;
    protected final ItemStack backpackItemStack;
    protected final boolean fromBaubles;
    protected final int configurationRows;
    protected final int configurationColumns;
    protected final int configurationStartY;
    protected final int inventoryStartY;
    protected final int hotbarStartY;


    public ContainerBackpackConfiguration(InventoryPlayer playerInventory, ItemStack backpackItemStack, boolean fromBaubles)
    {
        this.backpackItemStack = backpackItemStack;
        if (playerInventory.player != null && !playerInventory.player.world.isRemote)
            net.vyroxes.minersprosperity.item.backpack.BackpackSettings.ensureIdentity(backpackItemStack);
        this.configurationInventory = ItemBackpackBase.getConfigurationInventory(backpackItemStack);
        this.fromBaubles = fromBaubles;
        backpackItemStack.getOrCreateSubCompound("BackpackData").setBoolean("IsOpen", true);

        ItemBackpackBase item = (ItemBackpackBase) backpackItemStack.getItem();
        int totalSlots = item.getInventorySize();

        this.configurationColumns = item.getInventoryColumns();
        this.configurationRows = totalSlots / configurationColumns;

        int slotSize = 18;
        int columnOffset = Math.max(0, (configurationColumns - 9) / 2) * slotSize;
        configurationStartY = 40;
        int filterColumns = configurationColumns - getPanelColumns() - 1;
        for (int i = 0; i < configurationInventory.getSlots(); i++)
            this.addSlotToContainer(new SlotFilter(configurationInventory, i,
                    8 + i % filterColumns * 18, configurationStartY + i / filterColumns * 18));

        inventoryStartY = 53 + getContentHeight();
        hotbarStartY = inventoryStartY + 58;

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * slotSize + columnOffset, inventoryStartY + row * slotSize));
            }
        }

        for (int i = 0; i < 9; i++)
        {
            final int index = i;
            this.addSlotToContainer(new Slot(playerInventory, i, 8 + i * slotSize + columnOffset, hotbarStartY)
            {
                @Override
                public boolean canTakeStack(@NotNull EntityPlayer playerIn)
                {
                    ItemStack stackInSlot = playerInventory.getStackInSlot(index);
                    if (!stackInSlot.isEmpty() && stackInSlot.getItem() == backpackItemStack.getItem())
                    {
                        NBTTagCompound tag = stackInSlot.getTagCompound();
                        if (tag != null && tag.hasKey("BackpackData"))
                        {
                            NBTTagCompound backpackData = tag.getCompoundTag("BackpackData");
                            return !backpackData.getBoolean("IsOpen");
                        }
                    }
                    return true;
                }
            });
        }
    }

    public int getPanelColumns()
    {
        return configurationRows < 6 ? 3 : 2;
    }

    public int getContentHeight()
    {
        return configurationRows * 18;
    }

    public int getValue(String key)
    {
        return net.vyroxes.minersprosperity.item.backpack.BackpackSettings.get(backpackItemStack, key);
    }

    public void changeSetting(String key, int direction)
    {
        int value = direction == 0 ? net.vyroxes.minersprosperity.item.backpack.BackpackSettings.defaultValue(key)
                : Math.floorMod(getValue(key) + direction, net.vyroxes.minersprosperity.item.backpack.BackpackSettings.limit(key));
        net.vyroxes.minersprosperity.item.backpack.BackpackSettings.set(backpackItemStack, key, value);
        NetworkHandler.sendBackpackStateUpdate(value, key, fromBaubles);
    }

    public String getFilterText(boolean ore)
    {
        return net.vyroxes.minersprosperity.item.backpack.BackpackSettings.filterText(backpackItemStack, ore);
    }

    public boolean setFilterText(boolean ore, String text)
    {
        return net.vyroxes.minersprosperity.item.backpack.BackpackSettings.setFilterText(backpackItemStack, ore, text);
    }

    public boolean isOpenBackpack(ItemStack stack, EntityPlayer player)
    {
        if (net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, backpackItemStack)) return true;
        return fromBaubles ? net.minecraftforge.fml.common.Loader.isModLoaded("baubles")
                && net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, net.vyroxes.minersprosperity.integration.baubles.BaublesCompat.getBackpackStackFromBaubles(player))
                : net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, player.getHeldItemMainhand());
    }

    public int getRows()
    {
        return this.configurationRows;
    }

    public int getColumns()
    {
        return this.configurationColumns;
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer playerIn, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot clickedSlot = this.inventorySlots.get(index);

        int configurationSlotCount = configurationInventory.getSlots();
        int inventorySlotEnd = configurationSlotCount + 27 + 9;

        if (clickedSlot != null && clickedSlot.getHasStack())
        {
            ItemStack itemstack1 = clickedSlot.getStack();
            itemstack = itemstack1.copy();

            if (index >= configurationSlotCount && index < inventorySlotEnd)
            {
                ItemStack ghostCopy = itemstack.copy();
                ghostCopy.setCount(1);

                for (int i = 0; i < configurationSlotCount; i++)
                {
                    Slot slot = this.inventorySlots.get(i);
                    if (slot instanceof SlotFilter)
                    {
                        ItemStack existing = slot.getStack();

                        if (existing.isEmpty() || ItemStack.areItemsEqual(existing, ghostCopy))
                        {
                            slot.putStack(ghostCopy);
                            return ItemStack.EMPTY;
                        }
                    }
                }

                return ItemStack.EMPTY;
            }

            if (index < configurationSlotCount)
            {
                return ItemStack.EMPTY;
            }

            if (!this.mergeItemStack(itemstack1, 0, configurationSlotCount, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0)
            {
                clickedSlot.putStack(ItemStack.EMPTY);
            }
            else
            {
                clickedSlot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            clickedSlot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    public boolean setFilter(int slotId, @NotNull ItemStack ingredient)
    {
        if (slotId < 0 || slotId >= configurationInventory.getSlots()) return false;
        ItemStack copy = ingredient.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        for (int i = 0; i < configurationInventory.getSlots(); i++)
        {
            if (i != slotId && !copy.isEmpty()
                    && ItemStack.areItemStacksEqual(configurationInventory.getStackInSlot(i), copy)) return false;
        }
        configurationInventory.setStackInSlot(slotId, copy);
        return true;
    }

    @Override
    public @NotNull ItemStack slotClick(int slotId, int dragType, @NotNull ClickType clickTypeIn, @NotNull EntityPlayer player)
    {
        if (slotId >= 0 && slotId < this.inventorySlots.size())
        {
            Slot slot = this.inventorySlots.get(slotId);

            if (slot instanceof SlotFilter)
            {
                setFilter(slotId, player.inventory.getItemStack());

                return ItemStack.EMPTY;
            }
        }

        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer playerIn)
    {
        return fromBaubles ? net.minecraftforge.fml.common.Loader.isModLoaded("baubles") &&
                net.vyroxes.minersprosperity.integration.baubles.BaublesCompat.getBackpackStackFromBaubles(playerIn) == backpackItemStack
                : playerIn.getHeldItemMainhand() == backpackItemStack;
    }

    @Override
    public void onContainerClosed(@NotNull EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);

        backpackItemStack.getOrCreateSubCompound("BackpackData").setBoolean("IsOpen", false);
    }
}