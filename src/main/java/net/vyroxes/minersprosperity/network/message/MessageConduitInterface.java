package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import net.vyroxes.minersprosperity.transport.ConduitType;

public class MessageConduitInterface implements IMessage
{
    private BlockPos pos;
    private int side;
    private int direction;

    public MessageConduitInterface()
    {
    }

    public MessageConduitInterface(BlockPos pos, int side, int direction)
    {
        this.pos = pos;
        this.side = side;
        this.direction = direction;
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        pos = BlockPos.fromLong(buffer.readLong());
        side = buffer.readUnsignedByte();
        direction = buffer.readByte();
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        buffer.writeLong(pos.toLong());
        buffer.writeByte(side);
        buffer.writeByte(direction);
    }

    public static class Handler implements IMessageHandler<MessageConduitInterface, IMessage>
    {
        @Override
        public IMessage onMessage(MessageConduitInterface message, MessageContext context)
        {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.side >= 6 || Math.abs(message.direction) != 1 || player.isSpectator() || !player.isSneaking()) return;
                if (!player.world.isBlockLoaded(message.pos) || player.getDistanceSq(message.pos) > 64
                        || !player.world.isBlockModifiable(player, message.pos)) return;
                if (!(player.world.getTileEntity(message.pos) instanceof TileEntityConduit tile)
                        || tile.conduit() == null || tile.conduit().type != ConduitType.UNIVERSAL || !tile.facade.isEmpty()) return;
                Vec3d start = player.getPositionEyes(1);
                Vec3d end = start.add(player.getLook(1).scale(player.getEntityAttribute(EntityPlayerMP.REACH_DISTANCE).getAttributeValue()));
                RayTraceResult hit = player.world.rayTraceBlocks(start, end, false, false, false);
                if (hit == null || !message.pos.equals(hit.getBlockPos()) || hit.subHit != message.side) return;
                if (!player.canPlayerEdit(message.pos, hit.sideHit, player.getHeldItemMainhand())) return;
                tile.cycleInterfaceResource(message.side, message.direction);
            });
            return null;
        }
    }
}