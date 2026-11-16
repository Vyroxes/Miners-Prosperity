package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.vyroxes.minersprosperity.inventory.container.ContainerStorageCrate;

import org.jetbrains.annotations.NotNull;

public class MessageStorageCrate implements IMessage
{
    private int window, action, value;

    public MessageStorageCrate()
    {
    }

    public MessageStorageCrate(int window, int action, int value)
    {
        this.window = window;
        this.action = action;
        this.value = value;
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer)
    {
        buffer.writeInt(window);
        buffer.writeByte(action);
        buffer.writeInt(value);
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer)
    {
        window = buffer.readInt();
        action = buffer.readUnsignedByte();
        value = buffer.readInt();
    }

    public static class Handler implements IMessageHandler<MessageStorageCrate, IMessage>
    {
        @Override
        public IMessage onMessage(
                @NotNull MessageStorageCrate message, @NotNull MessageContext context)
        {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld()
                    .addScheduledTask(
                            () ->
                            {
                                if (!(player.openContainer
                                                instanceof ContainerStorageCrate container)
                                        || container.windowId != message.window
                                        || !container.canInteractWith(player)) return;
                                if (message.action == 0) container.tile.toggleLock(message.value);
                                else if (message.action == 1
                                        && (message.value >= -1 && message.value <= 1))
                                    container.tile.cycleRedstone(message.value);
                                else if (message.action == 2)
                                    container.tile.cycleSide(message.value);
                                player.connection.sendPacket(container.tile.getUpdatePacket());
                            });
            return null;
        }
    }
}