package net.vyroxes.minersprosperity.inventory.container;

import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.inventory.slot.SlotBackpack;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;
import org.jetbrains.annotations.NotNull;

@ChestContainer
public class ContainerBackpack extends Container
{
    protected final ItemStackHandler backpackInventory;
    protected final ItemStack backpackItemStack;
    protected final boolean fromBaubles;
    protected final int backpackRows;
    protected final int backpackColumns;
    protected final int backpackStartY;
    protected final int inventoryStartY;
    protected final int hotbarStartY;
    private net.vyroxes.minersprosperity.tile.TileEntityBackpack placed;

    public ContainerBackpack(InventoryPlayer playerInventory, ItemStack backpackItemStack, boolean fromBaubles)
    {
        this.backpackItemStack = backpackItemStack;
        if (playerInventory.player != null && !playerInventory.player.world.isRemote)
            net.vyroxes.minersprosperity.item.backpack.BackpackSettings.ensureIdentity(backpackItemStack);
        this.backpackInventory = ItemBackpackBase.getBackpackInventory(backpackItemStack);
        this.fromBaubles = fromBaubles;
        backpackItemStack.getOrCreateSubCompound("BackpackData").setBoolean("IsOpen", true);

        ItemBackpackBase item = (ItemBackpackBase) backpackItemStack.getItem();
        int totalSlots = item.getInventorySize();

        this.backpackColumns = item.getInventoryColumns();
        this.backpackRows = totalSlots / backpackColumns;

        int slotSize = 18;
        int columnOffset = Math.max(0, (backpackColumns - 9) / 2) * slotSize;
        backpackStartY = 23 + 17;

        for (int row = 0; row < backpackRows; row++)
        {
            for (int col = 0; col < backpackColumns; col++)
            {
                this.addSlotToContainer(new SlotBackpack(backpackInventory, col + row * backpackColumns, 8 + col * slotSize, backpackStartY + row * slotSize));
            }
        }

        inventoryStartY = 23 + 26 + backpackRows * slotSize + 4;
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

    public ContainerBackpack(InventoryPlayer playerInventory, net.vyroxes.minersprosperity.tile.TileEntityBackpack tile)
    {
        this(playerInventory, tile.getBackpack(), false);
        this.placed = tile;
    }

    public boolean isPlaced()
    {
        return placed != null;
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();
        if (placed != null && net.vyroxes.minersprosperity.item.backpack.BackpackSettings.data(backpackItemStack).getBoolean("AutomationDirty")) placed.markDirty();
    }

    @Override
    public @NotNull ItemStack slotClick(int slotId, int button, net.minecraft.inventory.@NotNull ClickType type, @NotNull EntityPlayer player)
    {
        if (slotId >= 0 && slotId < backpackInventory.getSlots() && type == net.minecraft.inventory.ClickType.PICKUP
                && net.vyroxes.minersprosperity.item.backpack.BackpackSettings.get(backpackItemStack, "LockEdit") == 1)
        {
            net.vyroxes.minersprosperity.item.backpack.BackpackSettings.toggleLock(backpackItemStack, slotId);
            return ItemStack.EMPTY;
        }
        Slot clicked = slotId >= 0 && slotId < inventorySlots.size() ? inventorySlots.get(slotId) : null;
        ItemStack before = clicked == null ? ItemStack.EMPTY : clicked.getStack().copy();
        ItemStack result = super.slotClick(slotId, button, type, player);

        if (clicked != null && !ItemStack.areItemStacksEqual(before, clicked.getStack()))
            net.vyroxes.minersprosperity.item.backpack.BackpackSettings.data(backpackItemStack).setBoolean("AutomationDirty", true);
        return result;
    }

    public boolean isOpenBackpack(ItemStack stack, EntityPlayer player)
    {
        if (placed != null) return false;
        if (net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, backpackItemStack)) return true;
        return fromBaubles ? net.minecraftforge.fml.common.Loader.isModLoaded("baubles")
                && net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, net.vyroxes.minersprosperity.integration.baubles.BaublesCompat.getBackpackStackFromBaubles(player))
                : net.vyroxes.minersprosperity.item.backpack.BackpackSettings.sameBackpack(stack, player.getHeldItemMainhand());
    }

    public int getRows()
    {
        return this.backpackRows;
    }

    @ChestContainer.RowSizeCallback
    public int getColumns()
    {
        return this.backpackColumns;
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer playerIn, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        int backpackSlotCount = backpackInventory.getSlots();
        int inventorySlotEnd = backpackSlotCount + 27 + 9;

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < backpackSlotCount)
            {
                if (!this.mergeItemStack(itemstack1, backpackSlotCount, inventorySlotEnd, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index < inventorySlotEnd)
            {
                if (!this.mergeItemStack(itemstack1, 0, backpackSlotCount, false))
                {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.getCount() == 0)
            {
                slot.putStack(ItemStack.EMPTY);
            }
            else
            {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer playerIn)
    {
        if (placed != null) return !placed.isInvalid() && playerIn.world.getTileEntity(placed.getPos()) == placed
                && playerIn.getDistanceSq(placed.getPos()) <= 64;
        return fromBaubles ? Loader.isModLoaded("baubles") && BaublesCompat.getBackpackStackFromBaubles(playerIn) == backpackItemStack
                : playerIn.getHeldItemMainhand() == backpackItemStack;
    }

    @Override
    public void onContainerClosed(@NotNull EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);

        backpackItemStack.getOrCreateSubCompound("BackpackData").setBoolean("IsOpen", false);
        if (placed != null) placed.markDirty();
    }
}