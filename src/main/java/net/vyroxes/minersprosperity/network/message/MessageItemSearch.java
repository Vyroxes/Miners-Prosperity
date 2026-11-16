package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.world.ILockableContainer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class MessageItemSearch implements IMessage
{
    private ItemStack ingredient = ItemStack.EMPTY;
    private int dimension;
    private BlockPos[] positions = new BlockPos[0];

    public MessageItemSearch()
    {
    }

    public MessageItemSearch(@NotNull ItemStack ingredient)
    {
        this.ingredient = ingredient.copy();
        this.ingredient.setCount(1);
    }

    private MessageItemSearch(int dimension, @NotNull List<BlockPos> positions)
    {
        this.dimension = dimension;
        this.positions = positions.toArray(new BlockPos[0]);
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer)
    {
        ingredient = ByteBufUtils.readItemStack(buffer);
        dimension = buffer.readInt();
        int size = buffer.readUnsignedByte();
        if (size > 128) throw new IllegalArgumentException("Too many item search results");
        positions = new BlockPos[size];
        for (int i = 0; i < size; i++) positions[i] = BlockPos.fromLong(buffer.readLong());
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer)
    {
        ByteBufUtils.writeItemStack(buffer, ingredient);
        buffer.writeInt(dimension);
        buffer.writeByte(positions.length);
        for (BlockPos pos : positions) buffer.writeLong(pos.toLong());
    }

    public static boolean containsItem(@NotNull IItemHandler handler, @NotNull ItemStack ingredient, int[] budget)
    {
        for (int i = 0; i < handler.getSlots() && budget[0] > 0; i++)
        {
            budget[0]--;
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ingredient.getItem()
                    && (ingredient.getMetadata() == OreDictionary.WILDCARD_VALUE
                    || stack.getMetadata() == ingredient.getMetadata())) return true;
        }
        return false;
    }

    private static boolean contains(@NotNull TileEntity tile, @NotNull ItemStack ingredient,
            @NotNull EntityPlayerMP player, int[] budget)
    {
        if (tile instanceof TileEntityStorageCrate crate)
        {
            for (int i = 0; i < crate.slots(); i++)
            {
                ItemStack stored = crate.template(i);
                if (crate.count(i) > 0 && stored.getItem() == ingredient.getItem()
                        && (ingredient.getMetadata() == OreDictionary.WILDCARD_VALUE
                        || stored.getMetadata() == ingredient.getMetadata())) return true;
            }
            return false;
        }
        Set<IItemHandler> inspected = Collections.newSetFromMap(new IdentityHashMap<>());
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler != null)
        {
            inspected.add(handler);
            if (containsItem(handler, ingredient, budget)) return true;
        }
        for (EnumFacing face : EnumFacing.VALUES)
        {
            handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            if (handler != null && inspected.add(handler) && containsItem(handler, ingredient, budget)) return true;
        }
        if (tile instanceof IInventory inventory)
            return containsItem(new InvWrapper(inventory), ingredient, budget);
        if (tile instanceof TileEntityEnderChest)
            return containsItem(new InvWrapper(player.getInventoryEnderChest()), ingredient, budget);
        return false;
    }

    private static void search(@NotNull MessageItemSearch message, @NotNull EntityPlayerMP player)
    {
        if (message.ingredient.isEmpty() || message.positions.length != 0 || player.isSpectator()) return;
        WorldServer world = player.getServerWorld();
        long now = world.getTotalWorldTime();
        long last = player.getEntityData().getLong("MinersProsperityItemSearch");
        if (player.getEntityData().hasKey("MinersProsperityItemSearch") && now >= last && now - last < 20) return;
        player.getEntityData().setLong("MinersProsperityItemSearch", now);
        int radius = Math.max(1, Math.min(64, ModConfig.OTHER.itemSearchRadius));
        BlockPos origin = player.getPosition();
        List<BlockPos> found = new ArrayList<>();
        int[] budget = {65536};
        outer:
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++)
        {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++)
            {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk == null) continue;
                for (TileEntity tile : new ArrayList<>(chunk.getTileEntityMap().values()))
                {
                    BlockPos pos = tile.getPos();
                    if (tile.isInvalid() || pos.distanceSq(origin) > radius * radius
                            || !world.isBlockModifiable(player, pos)
                            || !player.canPlayerEdit(pos, EnumFacing.UP, ItemStack.EMPTY)) continue;
                    if (tile instanceof ILockableContainer locked && !player.canOpen(locked.getLockCode())) continue;
                    if (contains(tile, message.ingredient, player, budget)) found.add(pos.toImmutable());
                    if (found.size() >= 128 || budget[0] <= 0) break outer;
                }
            }
        }
        NetworkHandler.NETWORK.sendTo(new MessageItemSearch(player.dimension, found), player);
        player.sendStatusMessage(new TextComponentTranslation("message.item_search.results",
                new TextComponentString(message.ingredient.getDisplayName())
                        .setStyle(new Style().setColor(TextFormatting.AQUA)), found.size(), radius)
                        .setStyle(new Style().setColor(TextFormatting.GRAY)), false);
        if (found.size() >= 128 || budget[0] <= 0)
            player.sendStatusMessage(new TextComponentTranslation("message.item_search.limit"), false);
    }

    public static class Handler implements IMessageHandler<MessageItemSearch, IMessage>
    {
        @Override
        public IMessage onMessage(@NotNull MessageItemSearch message, @NotNull MessageContext context)
        {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
            {
                if (context.side == Side.SERVER) search(message, context.getServerHandler().player);
                else MinersProsperity.proxy.showItemSearchResults(message.dimension, message.positions);
            });
            return null;
        }
    }
}