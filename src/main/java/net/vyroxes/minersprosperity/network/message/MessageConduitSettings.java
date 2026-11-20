package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.vyroxes.minersprosperity.inventory.container.ContainerConduit;
import net.vyroxes.minersprosperity.transport.ConduitPort;

public class MessageConduitSettings implements IMessage
{
    private int window;
    private int resource;
    private int filter;
    private NBTTagCompound settings;

    public MessageConduitSettings()
    {
    }

    public MessageConduitSettings(int window, int resource, int filter, NBTTagCompound settings)
    {
        this.window = window;
        this.resource = resource;
        this.filter = filter;
        this.settings = settings;
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        window = buffer.readInt();
        resource = buffer.readUnsignedByte();
        filter = buffer.readByte();
        if (buffer.readableBytes() > 32768) throw new IllegalArgumentException("Conduit settings packet too large");
        settings = ByteBufUtils.readTag(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        buffer.writeInt(window);
        buffer.writeByte(resource);
        buffer.writeByte(filter);
        ByteBufUtils.writeTag(buffer, settings);
    }

    public static class Handler implements IMessageHandler<MessageConduitSettings, IMessage>
    {
        @Override
        public IMessage onMessage(MessageConduitSettings message, MessageContext context)
        {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerConduit container) || container.windowId != message.window || !container.canInteractWith(player)) return;
                if (container.tile.conduit() == null || message.resource >= container.tile.ports[container.side].length
                        || !container.tile.conduit().type.supports(message.resource)) return;
                ConduitPort port = container.tile.ports[container.side][message.resource];
                if (message.filter >= 0 && message.filter < port.filters.length)
                {
                    ItemStack held = message.settings != null && message.settings.hasKey("GhostFilter", 10)
                            ? new ItemStack(message.settings.getCompoundTag("GhostFilter")) : player.inventory.getItemStack();
                    if (message.resource != 1 && message.resource != 2) return;
                    if (message.resource == 2 && !held.isEmpty() && net.minecraftforge.fluids.FluidUtil.getFluidContained(held) == null) return;
                    port.setFilter(message.filter, held);
                }

                else if (message.filter == -1 && message.settings != null)
                {
                    if (net.vyroxes.minersprosperity.transport.SlotSelection.parse(message.settings.getString("SourceSlots")) == null
                            || net.vyroxes.minersprosperity.transport.SlotSelection.parse(message.settings.getString("DestinationSlots")) == null) return;
                    for (net.vyroxes.minersprosperity.util.TextFilterList.Type type : net.vyroxes.minersprosperity.util.TextFilterList.Type.values())
                    {
                        String key = switch (type) { case ORE -> "Ore"; case MOD -> "Mod"; case FLUID -> "Fluid"; };
                        if (net.vyroxes.minersprosperity.util.TextFilterList.normalize(message.settings.getString(key), type) == null) return;
                    }
                    ItemStack[] filters = port.filters.clone();
                    port.read(message.settings, false);
                    for (int i = 0; i < filters.length; i++) port.setFilter(i, filters[i]);
                }
                else return;
                container.tile.changed();
                player.connection.sendPacket(new net.minecraft.network.play.server.SPacketUpdateTileEntity(container.tile.getPos(), 0, container.tile.getUpdateTag()));
            });
            return null;
        }
    }
}