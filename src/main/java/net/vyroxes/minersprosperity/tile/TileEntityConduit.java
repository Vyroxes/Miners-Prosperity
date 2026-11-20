package net.vyroxes.minersprosperity.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.block.BlockConduit;
import net.vyroxes.minersprosperity.transport.*;
import org.jetbrains.annotations.NotNull;

public class TileEntityConduit extends TileEntity
{
    public final ConduitPort[][] ports = new ConduitPort[6][4];
    private final int[] interfaceResources = new int[6];
    public ItemStack facade = ItemStack.EMPTY;
    public boolean transparentFacade;
    public final int[] signals = new int[6];
    public int settingsRevision;
    public int connectionMask;
    public boolean redstoneActive;
    public long activeUntil;
    public final long[] resourceUntil = new long[3];
    public final long[] fluidSideUntil = new long[6];
    public final int[] resourceFrom = new int[3];
    public final int[] resourceTo = new int[]{1, 1, 1};
    public ItemStack animationItem = ItemStack.EMPTY;
    public String animationFluid = "";
    public int animationFrom;
    public int animationTo = 1;
    public long lastAnimation = -100;
    public long transportSavedAt = -1;
    public final java.util.Deque<NBTTagCompound> itemTransits = new java.util.ArrayDeque<>();

    public final java.util.Deque<NBTTagCompound> fluidTransits = new java.util.ArrayDeque<>();

    public void recordItemTransit(ItemStack stack, double start, double duration, int from, int to, long visualId)
    {
        long now = world.getTotalWorldTime();
        itemTransits.removeIf(event -> event.getDouble("Start") + event.getDouble("Duration") + 20 <= now);
        if (itemTransits.size() >= 128) return;
        NBTTagCompound event = new NBTTagCompound();
        ItemStack icon = stack.copy();
        icon.setCount(1);
        event.setTag("Item", icon.writeToNBT(new NBTTagCompound()));
        event.setLong("TransitId", visualId);
        event.setDouble("Start", start);
        event.setDouble("Duration", duration);
        event.setInteger("From", from);
        event.setInteger("To", to);
        itemTransits.addLast(event);
    }

    public void recordFluidTransit(net.minecraftforge.fluids.FluidStack fluid, double start, double duration, int from, int to, long visualId)
    {
        long now = world.getTotalWorldTime();
        fluidTransits.removeIf(event -> event.getDouble("Start") + event.getDouble("Duration") + 20 <= now);
        if (fluidTransits.size() >= 128 || fluid == null) return;
        NBTTagCompound event = new NBTTagCompound();
        event.setString("Fluid", fluid.getFluid().getName());
        event.setLong("TransitId", visualId);
        event.setDouble("Start", start);
        event.setDouble("Duration", duration);
        event.setInteger("From", from);
        event.setInteger("To", to);
        fluidTransits.addLast(event);
    }

    public TileEntityConduit()
    {
        for (int side = 0; side < 6; side++) for (int resource = 0; resource < 4; resource++)
        {
            ports[side][resource] = new ConduitPort();
            ports[side][resource].mode = resource == 0 || resource == 3 ? 3 : 0;
            if (resource == 0 || resource == 3) ports[side][resource].interval = 1;
        }
    }

    public BlockConduit conduit()
    {
        return getBlockType() instanceof BlockConduit block ? block : null;
    }

    public int getInterfaceResource(int side)
    {
        BlockConduit block = conduit();
        return block != null && block.type != ConduitType.UNIVERSAL ? block.type.resourceIndex() : interfaceResources[side];
    }

    public void cycleInterfaceResource(int side, int direction)
    {
        interfaceResources[side] = Math.floorMod(interfaceResources[side] + direction, ConduitType.RESOURCE_COUNT);
        markDirty();
        if (world == null) return;
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    public void cycleInterfaceMode(int side)
    {
        int resource = getInterfaceResource(side);
        if (resource == ConduitType.LOGIC_RESOURCE) return;
        ConduitPort port = ports[side][resource];
        port.mode = (port.mode + 1) % 4;
        changed();
    }

    public boolean isActive(long now)
    {
        if (redstoneActive) return true;
        for (int signal : signals) if (signal > 0) return true;
        for (long until : resourceUntil) if (until > now) return true;
        return false;
    }

    private void readInterfaceResources(NBTTagCompound tag)
    {
        int[] resources = tag.getIntArray("InterfaceResources");
        for (int side = 0; side < 6; side++)
            interfaceResources[side] = resources.length == 6 ? Math.floorMod(resources[side], ConduitType.RESOURCE_COUNT) : 0;
    }

    public boolean enabled(EnumFacing side, int resource)
    {
        ConduitPort port = ports[side.getIndex()][resource];
        if (port.redstone == 0) return true;
        boolean powered = false;
        for (EnumFacing face : EnumFacing.VALUES)
        {
            BlockPos neighbor = pos.offset(face);
            if (world.isBlockLoaded(neighbor) && !(world.getBlockState(neighbor).getBlock() instanceof BlockConduit)
                    && world.getRedstonePower(neighbor, face) > 0) powered = true;
        }
        return (port.redstone==1) == powered;
    }

    public void refreshConnections()
    {
        if (conduit() == null) return;
        int mask = 0;
        for (EnumFacing side : EnumFacing.VALUES) if (conduit().connects(world, pos, side)) mask |= 1 << side.getIndex();
        if (mask != connectionMask)
        {
            connectionMask = mask;
            ConduitNetworks.invalidate(world, pos);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public void changed()
    {
        markDirty();
        if (world == null) return;
        ConduitNetworks.invalidate(world, pos);
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public void onLoad()
    {
        refreshConnections();
        ConduitNetworks.add(this);
    }

    @Override
    public void invalidate()
    {
        ConduitNetworks.remove(this);
        super.invalidate();
    }

    @Override
    public void onChunkUnload()
    {
        ConduitNetworks.pause(this);
        ConduitNetworks.remove(this);
        super.onChunkUnload();
    }

    @Override
    public boolean shouldRefresh(@NotNull World world, @NotNull BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (ConduitPort[] side : ports) for (ConduitPort port : side) list.appendTag(port.write(true));
        tag.setTag("Ports", list);
        if (world != null) tag.setLong("TransportSavedAt", world.getTotalWorldTime());
        tag.setTag("Facade", facade.writeToNBT(new NBTTagCompound()));
        tag.setBoolean("TransparentFacade", transparentFacade);
        tag.setIntArray("InterfaceResources", interfaceResources.clone());
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        settingsRevision++;
        transportSavedAt = tag.hasKey("TransportSavedAt") ? tag.getLong("TransportSavedAt") : -1;
        readInterfaceResources(tag);
        NBTTagList list = tag.getTagList("Ports", 10);
        for (int i = 0; i < Math.min(24, list.tagCount()); i++) ports[i / 4][i % 4].read(list.getCompoundTagAt(i), true);
        facade = new ItemStack(tag.getCompoundTag("Facade"));
        transparentFacade = tag.getBoolean("TransparentFacade");
        if (tag.hasKey("ActiveUntil")) readVisual(tag);
    }

    private void readVisual(NBTTagCompound tag)
    {
        readInterfaceResources(tag);
        {
            activeUntil = tag.getLong("ActiveUntil");
            for (int side = 0; side < 6; side++) fluidSideUntil[side] = tag.getLong("FluidSide" + side);
            itemTransits.clear();
            NBTTagList events = tag.getTagList("ItemTransits", 10);
            for (int i = 0; i < Math.min(128, events.tagCount()); i++) itemTransits.addLast(events.getCompoundTagAt(i).copy());
            fluidTransits.clear();
            NBTTagList fluids = tag.getTagList("FluidTransits", 10);
            for (int i = 0; i < Math.min(128, fluids.tagCount()); i++) fluidTransits.addLast(fluids.getCompoundTagAt(i).copy());
            redstoneActive = tag.getBoolean("RedstoneActive");
            for (int i = 0; i < 3; i++)
            {
                resourceUntil[i] = tag.getLong("Until" + i);
                resourceFrom[i] = Math.floorMod(tag.getInteger("From" + i), 6);
                resourceTo[i] = Math.floorMod(tag.getInteger("To" + i), 6);
            }
            int[] modes = tag.getIntArray("Modes");
            if (modes.length == 24) for (int i = 0; i < 24; i++) ports[i / 4][i % 4].mode = modes[i];
            facade = new ItemStack(tag.getCompoundTag("Facade"));
            transparentFacade = tag.getBoolean("TransparentFacade");
            animationItem = new ItemStack(tag.getCompoundTag("AnimationItem"));
            animationFluid = tag.getString("AnimationFluid");
            animationFrom = Math.floorMod(tag.getInteger("From"), 6);
            animationTo = Math.floorMod(tag.getInteger("To"), 6);
            int[] values = tag.getIntArray("Signals");
            if (values.length == 6) System.arraycopy(values, 0, signals, 0, 6);
        }
    }

    @Override
    public @NotNull NBTTagCompound getUpdateTag()
    {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.merge(visualTag());
        return tag;
    }

    private NBTTagCompound visualTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Facade", facade.writeToNBT(new NBTTagCompound()));
        tag.setBoolean("TransparentFacade", transparentFacade);
        tag.setIntArray("InterfaceResources", interfaceResources.clone());
        tag.setBoolean("RedstoneActive", redstoneActive);
        NBTTagList events = new NBTTagList();
        long now = world == null ? 0 : world.getTotalWorldTime();
        for (NBTTagCompound event : itemTransits) if (event.getDouble("Start") + event.getDouble("Duration") + 20 > now) events.appendTag(event.copy());
        tag.setTag("ItemTransits", events);
        NBTTagList fluids = new NBTTagList();
        for (NBTTagCompound event : fluidTransits) if (event.getDouble("Start") + event.getDouble("Duration") + 20 > now) fluids.appendTag(event.copy());
        tag.setTag("FluidTransits", fluids);
        int[] modes = new int[24];
        for (int i = 0; i < 24; i++) modes[i] = ports[i / 4][i % 4].mode;
        tag.setIntArray("Modes", modes);
        tag.setLong("ActiveUntil", activeUntil);
        for (int side = 0; side < 6; side++) tag.setLong("FluidSide" + side, fluidSideUntil[side]);
        for (int i = 0; i < 3; i++)
        {
            tag.setLong("Until" + i, resourceUntil[i]);
            tag.setInteger("From" + i, resourceFrom[i]);
            tag.setInteger("To" + i, resourceTo[i]);
        }
        tag.setTag("AnimationItem", animationItem.writeToNBT(new NBTTagCompound()));
        tag.setString("AnimationFluid", animationFluid);
        tag.setInteger("From", animationFrom);
        tag.setInteger("To", animationTo);
        tag.setIntArray("Signals", signals);
        return tag;
    }

    @Override
    public boolean shouldRenderInPass(int pass)
    {
        if (pass == 0) return true;
        if (pass != 1) return false;
        BlockConduit block = conduit();
        return block != null && (block.type == ConduitType.ITEM || block.type == ConduitType.FLUID
                || block.type == ConduitType.UNIVERSAL);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 1, visualTag());
    }

    @Override
    public void onDataPacket(@NotNull NetworkManager manager, SPacketUpdateTileEntity packet)
    {
        if (packet.getTileEntityType() == 1) readVisual(packet.getNbtCompound());
        else readFromNBT(packet.getNbtCompound());
        world.markBlockRangeForRenderUpdate(pos, pos);
    }
}