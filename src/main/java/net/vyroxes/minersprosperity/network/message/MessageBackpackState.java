package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;

public class MessageBackpackState implements IMessage
{
    private int value;
    private String state;
    private boolean fromBaubles;

    public MessageBackpackState()
    {
    }

    public MessageBackpackState(boolean value, String state, boolean fromBaubles)
    {
        this.value = value ? 1 : 0;
        this.state = state;
        this.fromBaubles = fromBaubles;
    }

    public MessageBackpackState(int value, String state, boolean fromBaubles)
    {
        this.value = value;
        this.state = state;
        this.fromBaubles = fromBaubles;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.value = buf.readInt();
        this.state = ByteBufUtils.readUTF8String(buf);
        this.fromBaubles = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.value);
        ByteBufUtils.writeUTF8String(buf, this.state);
        buf.writeBoolean(this.fromBaubles);
    }

    public static class Handler implements IMessageHandler<MessageBackpackState, IMessage>
    {
        @Override
        public IMessage onMessage(MessageBackpackState message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (net.vyroxes.minersprosperity.item.backpack.BackpackSettings.limit(message.state) == 0) return;
                ItemStack stack = message.fromBaubles && Loader.isModLoaded("baubles")
                        ? BaublesCompat.getStackInSlot(player, 5) : player.getHeldItemMainhand();
                if (!(stack.getItem() instanceof net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase)) return;
                net.vyroxes.minersprosperity.item.backpack.BackpackSettings.set(stack, message.state, message.value);
                if (message.fromBaubles && Loader.isModLoaded("baubles")) BaublesCompat.setChanged(player, 5);
            });

            return null;
        }
    }
}