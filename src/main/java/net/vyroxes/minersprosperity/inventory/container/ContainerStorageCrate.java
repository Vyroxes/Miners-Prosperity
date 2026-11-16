package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.vyroxes.minersprosperity.block.BlockStorageCrate;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;

import org.jetbrains.annotations.NotNull;

public class ContainerStorageCrate extends Container
{
    public final TileEntityStorageCrate tile;
    private final int storageSlots;
    private int revision = -1;

    public static int slotX(int slots, int slot)
    {
        if (slots == 1 || slots == 3 && slot == 0) return 80;
        return 70 + 20 * (slots == 3 ? slot - 1 : slot % 2);
    }

    public static int slotY(int slots, int slot)
    {
        if (slots <= 2) return 39;
        if (slots == 3) return slot == 0 ? 29 : 49;
        return 29 + 20 * (slot / 2);
    }

    public ContainerStorageCrate(
            @NotNull InventoryPlayer inventory, @NotNull TileEntityStorageCrate tile)
    {
        this.tile = tile;
        this.storageSlots = tile.isCompacting() ? 4 : tile.slots();
        InventoryBasic display = new InventoryBasic("crate", false, storageSlots);

        for (int i = 0; i < storageSlots; i++)
        {
            final int compartment = i;
            addSlotToContainer(
                    new Slot(display, i, 0, 0)
                    {
                        @Override
                        public @NotNull ItemStack getStack()
                        {
                            return tile.template(compartment);
                        }

                        @Override
                        public void putStack(@NotNull ItemStack stack)
                        {
                        }

                        @Override
                        public boolean isItemValid(@NotNull ItemStack stack)
                        {
                            return false;
                        }

                        @Override
                        public boolean canTakeStack(@NotNull EntityPlayer player)
                        {
                            return false;
                        }
                    });
        }

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
                addSlotToContainer(
                        new Slot(inventory, column + row * 9 + 9, 8 + 18 * column, 84 + 18 * row));
        }

        for (int column = 0; column < 9; column++)
            addSlotToContainer(new Slot(inventory, column, 8 + 18 * column, 142));

        refreshSlots();
    }

    public void refreshSlots()
    {
        for (int i = 0; i < storageSlots; i++)
        {
            Slot slot = inventorySlots.get(i);
            slot.xPos = i < tile.slots() ? slotX(tile.slots(), i) : -10000;
            slot.yPos = i < tile.slots() ? slotY(tile.slots(), i) : -10000;
        }
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player)
    {
        return !tile.isInvalid()
                && tile.getWorld().isBlockLoaded(tile.getPos())
                && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public boolean canDragIntoSlot(@NotNull Slot slot)
    {
        return slot.slotNumber >= storageSlots;
    }

    @Override
    public boolean canMergeSlot(@NotNull ItemStack stack, @NotNull Slot slot)
    {
        return slot.slotNumber >= storageSlots;
    }

    @Override
    public void detectAndSendChanges()
    {
        refreshSlots();
        super.detectAndSendChanges();

        if (revision != tile.revision)
        {
            for (IContainerListener listener : listeners)
            {
                if (listener instanceof EntityPlayerMP player)
                    player.connection.sendPacket(tile.getUpdatePacket());
            }
            revision = tile.revision;
        }
    }

    @Override
    public @NotNull ItemStack slotClick(
            int slot, int button, @NotNull ClickType type, @NotNull EntityPlayer player)
    {
        if (type == ClickType.QUICK_MOVE && slot >= 0 && slot < inventorySlots.size())
        {
            if (!tile.getWorld().isRemote)
            {
                transferStackInSlot(player, slot);
                synchronizeCursor(player);
            }
            return ItemStack.EMPTY;
        }
        if (slot < 0 || slot >= storageSlots) return super.slotClick(slot, button, type, player);
        if (slot >= tile.slots() || tile.getWorld().isRemote) return ItemStack.EMPTY;

        if (type == ClickType.PICKUP)
        {
            ItemStack cursor = player.inventory.getItemStack();
            if (cursor.isEmpty())
            {
                ItemStack available = tile.extract(slot, 64, true);
                int amount = button == 1 ? (available.getCount() + 1) / 2 : available.getCount();
                player.inventory.setItemStack(tile.extract(slot, amount, false));
            }
            else
            {
                ItemStack offered = cursor.copy();
                offered.setCount(button == 1 ? 1 : cursor.getCount());
                ItemStack remainder = tile.insert(slot, offered, false);
                cursor.shrink(offered.getCount() - remainder.getCount());
                player.inventory.setItemStack(cursor);
            }
        }
        else if (type == ClickType.CLONE
                && player.isCreative()
                && player.inventory.getItemStack().isEmpty())
        {
            ItemStack stack = tile.template(slot);
            if (!stack.isEmpty())
            {
                stack.setCount(stack.getMaxStackSize());
                player.inventory.setItemStack(stack);
            }
        }
        else if (type == ClickType.THROW && player.inventory.getItemStack().isEmpty())
            player.dropItem(tile.extract(slot, button == 0 ? 1 : 64, false), false);

        synchronizeCursor(player);
        return ItemStack.EMPTY;
    }

    private void synchronizeCursor(@NotNull EntityPlayer player)
    {
        detectAndSendChanges();
        if (player instanceof EntityPlayerMP serverPlayer)
            serverPlayer.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index)
    {
        if (tile.getWorld().isRemote || index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        if (index < storageSlots)
        {
            if (index < tile.slots()) BlockStorageCrate.take(tile, player, index, 64, false);
            return ItemStack.EMPTY;
        }

        Slot slot = inventorySlots.get(index);
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        ItemStack remainder = tile.insertAny(stack, false);
        if (remainder.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.putStack(remainder);
        return original;
    }
}