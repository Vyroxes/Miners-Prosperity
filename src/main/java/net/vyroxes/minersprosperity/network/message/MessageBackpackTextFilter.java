package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;
import net.vyroxes.minersprosperity.inventory.container.ContainerBackpackConfiguration;

public class MessageBackpackTextFilter implements IMessage
{
    private int window;
    private boolean ore;
    private String text;

    public MessageBackpackTextFilter()
    {
    }

    public MessageBackpackTextFilter(int window, boolean ore, String text)
    {
        this.window = window;
        this.ore = ore;
        this.text = text;
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        if (buffer.readableBytes() > 8192) throw new IllegalArgumentException("Backpack filter packet too large");
        window = buffer.readInt();
        ore = buffer.readBoolean();
        text = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        buffer.writeInt(window);
        buffer.writeBoolean(ore);
        ByteBufUtils.writeUTF8String(buffer, text);
    }

    public static class Handler implements IMessageHandler<MessageBackpackTextFilter, IMessage>
    {
        @Override
        public IMessage onMessage(MessageBackpackTextFilter message, MessageContext context)
        {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
            {
                if (!(player.openContainer instanceof ContainerBackpackConfiguration container)
                        || container.windowId != message.window || !container.canInteractWith(player)
                        || !container.setFilterText(message.ore, message.text)) return;
                player.inventory.markDirty();
                if (Loader.isModLoaded("baubles")
                        && container.isOpenBackpack(BaublesCompat.getStackInSlot(player, 5), player))
                    BaublesCompat.setChanged(player, 5);
                container.detectAndSendChanges();
            });
            return null;
        }
    }
}