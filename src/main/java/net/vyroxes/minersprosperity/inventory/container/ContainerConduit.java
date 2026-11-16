package net.vyroxes.minersprosperity.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import org.jetbrains.annotations.NotNull;

public class ContainerConduit extends Container
{
    public final TileEntityConduit tile;
    public final int side;
    public int connectedSlots;

    public ContainerConduit(InventoryPlayer inventory, TileEntityConduit tile, int side)
    {
        this.tile = tile;
        this.side = side;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, 9 + row * 9 + column, 44 + column * 18, 166 + row * 18));
        for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column, 44 + column * 18, 224));
    }

    @Override
    public void addListener(@NotNull IContainerListener listener)
    {
        super.addListener(listener);
        int count = countConnectedSlots();
        listener.sendWindowProperty(this, 0, count & 65535);
        listener.sendWindowProperty(this, 1, count >>> 16);
        if (listener instanceof EntityPlayerMP player) player.connection.sendPacket(new net.minecraft.network.play.server.SPacketUpdateTileEntity(tile.getPos(), 0, tile.getUpdateTag()));
    }

    private int countConnectedSlots()
    {
        net.minecraft.util.EnumFacing face = net.minecraft.util.EnumFacing.byIndex(side);
        net.minecraft.util.math.BlockPos pos = tile.getPos().offset(face);
        if (!tile.getWorld().isBlockLoaded(pos)) return 0;
        net.minecraft.tileentity.TileEntity target = tile.getWorld().getTileEntity(pos);
        net.minecraftforge.items.IItemHandler items = target == null ? null
                : target.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
        return items == null ? 0 : items.getSlots();
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();
        if (tile.getWorld().isRemote) return;
        int count = countConnectedSlots();
        if (count == connectedSlots) return;
        connectedSlots = count;
        for (IContainerListener listener : listeners)
        {
            listener.sendWindowProperty(this, 0, count & 65535);
            listener.sendWindowProperty(this, 1, count >>> 16);
        }
    }

    @Override
    public void updateProgressBar(int id, int value)
    {
        if (id == 0) connectedSlots = (connectedSlots & 0xFFFF0000) | (value & 65535);
        else if (id == 1) connectedSlots = (connectedSlots & 65535) | (value << 16);
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player)
    {
        return !tile.isInvalid() && player.world == tile.getWorld() && player.getDistanceSq(tile.getPos()) <= 64
                && player.world.isBlockLoaded(tile.getPos()) && player.world.getTileEntity(tile.getPos()) == tile;
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index)
    {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (!mergeItemStack(stack, index < 27 ? 27 : 0, index < 27 ? 36 : 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}