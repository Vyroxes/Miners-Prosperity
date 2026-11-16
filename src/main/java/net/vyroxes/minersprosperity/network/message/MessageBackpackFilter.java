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
import net.vyroxes.minersprosperity.inventory.container.ContainerBackpackConfiguration;
import org.jetbrains.annotations.NotNull;

public class MessageBackpackFilter implements IMessage
{
    private int window;
    private int slot;
    private ItemStack ingredient = ItemStack.EMPTY;

    public MessageBackpackFilter()
    {
    }

    public MessageBackpackFilter(int window, int slot, @NotNull ItemStack ingredient)
    {
        this.window = window;
        this.slot = slot;
        this.ingredient = ingredient.copy();
        if (!this.ingredient.isEmpty()) this.ingredient.setCount(1);
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer)
    {
        window = buffer.readInt();
        slot = buffer.readInt();
        ingredient = ByteBufUtils.readItemStack(buffer);
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer)
    {
        buffer.writeInt(window);
        buffer.writeInt(slot);
        ByteBufUtils.writeItemStack(buffer, ingredient);
    }

    public static class Handler implements IMessageHandler<MessageBackpackFilter, IMessage>
    {
        @Override
        public IMessage onMessage(@NotNull MessageBackpackFilter message, @NotNull MessageContext context)
        {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
            {
                if (!(player.openContainer instanceof ContainerBackpackConfiguration container)
                        || container.windowId != message.window || !container.canInteractWith(player)
                        || message.ingredient.isEmpty() || !container.setFilter(message.slot, message.ingredient)) return;
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