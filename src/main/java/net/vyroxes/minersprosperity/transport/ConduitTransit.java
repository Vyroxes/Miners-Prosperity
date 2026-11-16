package net.vyroxes.minersprosperity.transport;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

public final class ConduitTransit
{
    public BlockPos target;
    public EnumFacing side;
    public long[] route;
    public long arrival;
    public BlockPos origin;
    public EnumFacing originSide;
    public int channel;
    public int[] durations = new int[0];
    public long started;
    public int index;
    public long nextStepAt;
    public EnumFacing entrySide;
    public EnumFacing exitSide;
    public boolean recovering;
    public boolean returning;
    public int redirects;
    public long visualId = java.util.concurrent.ThreadLocalRandom.current().nextLong();
    public long retryAt;
    public ItemStack item = ItemStack.EMPTY;
    public FluidStack fluid;

    public ConduitTransit(BlockPos target, EnumFacing side, long[] route, long arrival)
    {
        this.target = target;
        this.side = side;
        this.route = route;
        this.arrival = arrival;
        this.retryAt = arrival;
        this.index = Math.max(0, route.length - 1);
    }

    public NBTTagCompound write()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Target", target.toLong());
        tag.setInteger("Side", side.getIndex());
        NBTTagList positions = new NBTTagList();
        for (long position : route) positions.appendTag(new NBTTagLong(position));
        tag.setTag("Route", positions);
        tag.setLong("Arrival", arrival);
        if (origin != null)
        {
            tag.setLong("Origin", origin.toLong());
            tag.setInteger("OriginSide", originSide.getIndex());
            tag.setInteger("Channel", channel);
            tag.setIntArray("Durations", durations);
            tag.setLong("Started", started);
            tag.setInteger("Index", index);
            tag.setLong("NextStepAt", nextStepAt);
            tag.setInteger("EntrySide", entrySide.getIndex());
            tag.setInteger("ExitSide", exitSide.getIndex());
            tag.setBoolean("Recovering", recovering);
            tag.setBoolean("Returning", returning);
            tag.setInteger("Redirects", redirects);
            tag.setLong("VisualId", visualId);
        }
        if (!item.isEmpty()) tag.setTag("Item", item.writeToNBT(new NBTTagCompound()));
        if (fluid != null) tag.setTag("Fluid", fluid.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    public static ConduitTransit read(NBTTagCompound tag)
    {
        NBTTagList positions = tag.getTagList("Route", 4);
        long[] route = new long[positions.tagCount()];
        for (int i = 0; i < route.length; i++) route[i] = ((NBTTagLong) positions.get(i)).getLong();
        ConduitTransit transit = new ConduitTransit(BlockPos.fromLong(tag.getLong("Target")),
                EnumFacing.byIndex(tag.getInteger("Side")), route, tag.getLong("Arrival"));
        if (tag.hasKey("Origin"))
        {
            transit.origin = BlockPos.fromLong(tag.getLong("Origin"));
            transit.originSide = EnumFacing.byIndex(tag.getInteger("OriginSide"));
            transit.channel = tag.getInteger("Channel");
            transit.durations = tag.getIntArray("Durations");
            transit.started = tag.getLong("Started");
            transit.index = Math.max(0, Math.min(route.length - 1, tag.getInteger("Index")));
            transit.nextStepAt = tag.getLong("NextStepAt");
            transit.entrySide = EnumFacing.byIndex(tag.getInteger("EntrySide"));
            transit.exitSide = EnumFacing.byIndex(tag.getInteger("ExitSide"));
            transit.recovering = tag.getBoolean("Recovering");
            transit.returning = tag.getBoolean("Returning");
            transit.redirects = Math.max(0, tag.getInteger("Redirects"));
            transit.visualId = tag.getLong("VisualId");
        }
        transit.item = new ItemStack(tag.getCompoundTag("Item"));
        transit.fluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("Fluid"));
        return transit;
    }
}