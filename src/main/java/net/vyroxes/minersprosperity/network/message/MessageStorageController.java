package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.vyroxes.minersprosperity.inventory.container.ContainerInventory;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController;

public class MessageStorageController implements IMessage
{
    private BlockPos pos;
    private int window;
    private boolean reset;

    public MessageStorageController()
    {
    }
    public MessageStorageController(BlockPos pos, int window)
    {
        this(pos, window, false);
    }

    public MessageStorageController(BlockPos pos, int window, boolean reset)
    {
        this.pos = pos;
        this.window = window;
        this.reset = reset;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(pos.toLong());
        buf.writeInt(window);
        buf.writeBoolean(reset);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        pos = BlockPos.fromLong(buf.readLong());
        window = buf.readInt();
        reset = buf.readBoolean();
    }

    public static class Handler implements IMessageHandler<MessageStorageController, IMessage>
    {
        @Override
        public IMessage onMessage(MessageStorageController message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
            {
                if (!(player.openContainer instanceof ContainerInventory)
                        || player.openContainer.windowId != message.window
                        || player.getDistanceSq(message.pos) > 64
                        || !player.world.isBlockLoaded(message.pos)
                        || !player.openContainer.canInteractWith(player)) return;
                TileEntity tile = player.world.getTileEntity(message.pos);
                if (tile instanceof TileEntityStorageCrateController controller && ((ContainerInventory) player.openContainer).getTileEntity() == tile)
                {
                    if (!message.reset || controller.assignsEmpty()) controller.toggleAssignEmpty();
                }
            });
            return null;
        }
    }
}