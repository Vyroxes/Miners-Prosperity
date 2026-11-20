package net.vyroxes.minersprosperity.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.client.gui.FluidFormatter;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.init.ModItems;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import net.vyroxes.minersprosperity.transport.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockConduit extends BlockBase
{
    public static final PropertyBool[] CONNECTIONS = new PropertyBool[]{PropertyBool.create("down"), PropertyBool.create("up"), PropertyBool.create("north"), PropertyBool.create("south"), PropertyBool.create("west"), PropertyBool.create("east")};
    private static final AxisAlignedBB CORE = new AxisAlignedBB(0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875);
    public final ConduitType type;
    public final ConduitTier tier;

    public BlockConduit(String name, ConduitType type, ConduitTier tier)
    {
        super(name, Material.IRON, net.minecraft.block.material.MapColor.GRAY, true);
        this.type = type;
        this.tier = tier;
        setHardness(1.5F);
        setResistance(6);
        IBlockState state = blockState.getBaseState();
        for (PropertyBool property : CONNECTIONS) state = state.withProperty(property, false);
        setDefaultState(state);
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override
    public void addInformation(@NotNull ItemStack stack, @javax.annotation.Nullable World world,
                               @NotNull List<String> tooltip, @NotNull net.minecraft.client.util.ITooltipFlag flag)
    {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.conduit." + type.id));
        for (int resource = 0; resource < 4; resource++)
        {
            if (!type.supports(resource)) continue;
            String value = switch (resource)
            {
                case 0 -> net.vyroxes.minersprosperity.client.gui.EnergyFormatter.usage(tier.rate(0), false);
                case 1 -> net.minecraft.client.resources.I18n.format("tooltip.conduit.items_per_operation", tier.rate(1));
                case 2 -> net.minecraft.client.resources.I18n.format("tooltip.conduit.fluid_per_operation", tier.rate(2));
                default -> "0-15";
            };
            tooltip.add(net.minecraft.util.text.TextFormatting.AQUA + net.minecraft.client.resources.I18n.format("tooltip.conduit.rate." + resource)
                    + net.minecraft.util.text.TextFormatting.GRAY + ": " + value);
        }
        if (type == ConduitType.UNIVERSAL)
            tooltip.add(TextFormatting.AQUA + I18n.format("gui.conduit.resource." + ConduitType.LOGIC_RESOURCE));
        if (type == ConduitType.ITEM || type == ConduitType.FLUID || type == ConduitType.UNIVERSAL)
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.conduit.interval"));
        NBTTagCompound data = stack.getSubCompound("BlockEntityTag");
        if (type.supports(2) && data != null)
        {
            NBTTagList ports = data.getTagList("Ports", 10);
            Map<String, Long> contents = new LinkedHashMap<>();
            for (int i = 2; i < ports.tagCount(); i += 4)
            {
                NBTTagCompound port = ports.getCompoundTagAt(i);
                addStoredFluid(contents, port.getCompoundTag("PendingFluid"));
                for (net.minecraft.nbt.NBTBase entry : port.getTagList("Transits", 10))
                    addStoredFluid(contents, ((NBTTagCompound) entry).getCompoundTag("Fluid"));
            }
            for (Map.Entry<String, Long> entry : contents.entrySet())
            {
                long amount = entry.getValue();
                tooltip.add(TextFormatting.AQUA + I18n.format("gui.fluid") + TextFormatting.GRAY + ": " + entry.getKey());
                tooltip.add(TextFormatting.AQUA + I18n.format("gui.fluid_stored") + TextFormatting.GRAY + ": "
                        + FluidFormatter.amount(amount));
            }
        }
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    private static void addStoredFluid(Map<String, Long> contents, NBTTagCompound data)
    {
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(data);
        if (fluid != null && fluid.amount > 0) contents.merge(fluid.getLocalizedName(), (long) fluid.amount, Long::sum);
    }

    @Override
    protected @NotNull BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, CONNECTIONS);
    }

    @Override
    public int getMetaFromState(@NotNull IBlockState state)
    {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(@NotNull IBlockState state)
    {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(@NotNull IBlockState state)
    {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public net.minecraft.block.state.@NotNull BlockFaceShape getBlockFaceShape(@NotNull IBlockAccess world, @NotNull IBlockState state, @NotNull BlockPos pos, @NotNull EnumFacing face)
    {
        return net.minecraft.block.state.BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean hasTileEntity(@NotNull IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@NotNull World world, @NotNull IBlockState state)
    {
        return new TileEntityConduit();
    }

    public boolean connects(IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        BlockPos target = pos.offset(side);
        if (world instanceof World level && !level.isBlockLoaded(target)) return false;
        Block block = world.getBlockState(target).getBlock();
        if (block instanceof BlockConduit other)
            return other.type == type || type.supports(ConduitType.LOGIC_RESOURCE) && other.type.supports(ConduitType.LOGIC_RESOURCE);
        if (type == ConduitType.LOGIC) return false;
        if (type.supports(3) && block != net.minecraft.init.Blocks.AIR) return true;
        TileEntity tile = world.getTileEntity(target);
        if (tile == null) return false;
        EnumFacing face = side.getOpposite();
        return type.supports(0) && tile.hasCapability(CapabilityEnergy.ENERGY, face)
                || type.supports(1) && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face)
                || type.supports(2) && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState getActualState(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos)
    {
        for (EnumFacing side : EnumFacing.VALUES) state = state.withProperty(CONNECTIONS[side.getIndex()], connects(world, pos, side));
        return state;
    }

    public EnumFacing straightDirection(IBlockAccess world, BlockPos pos)
    {
        EnumFacing first = null;
        int count = 0;
        for (EnumFacing side : EnumFacing.VALUES)
        {
            if (!connects(world, pos, side)) continue;
            if (!(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)) return null;
            if (first == null) first = side;
            else if (side != first.getOpposite()) return null;
            if (++count > 2) return null;
        }
        return count == 2 ? first : null;
    }

    public AxisAlignedBB coreShape(IBlockAccess world, BlockPos pos)
    {
        EnumFacing straight = straightDirection(world, pos);
        if (straight == null) return CORE;
        return new AxisAlignedBB(straight.getAxis() == EnumFacing.Axis.X ? 0 : 0.375,
                straight.getAxis() == EnumFacing.Axis.Y ? 0 : 0.375,
                straight.getAxis() == EnumFacing.Axis.Z ? 0 : 0.375,
                straight.getAxis() == EnumFacing.Axis.X ? 1 : 0.625,
                straight.getAxis() == EnumFacing.Axis.Y ? 1 : 0.625,
                straight.getAxis() == EnumFacing.Axis.Z ? 1 : 0.625);
    }

    public static AxisAlignedBB arm(EnumFacing side)
    {
        return arm(side, false);
    }

    public static AxisAlignedBB arm(EnumFacing side, boolean interfaceSide)
    {
        double start = interfaceSide ? 5.5 / 16 : 6 / 16D;
        double end = 1 - start;
        double[] min = {start, start, start};
        double[] max = {end, end, end};
        int axis = side.getAxis() == EnumFacing.Axis.X ? 0 : side.getAxis() == EnumFacing.Axis.Y ? 1 : 2;
        if (side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE) min[axis] = 0.6875;
        else max[axis] = 0.3125;
        if (side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE) max[axis] = 1;
        else min[axis] = 0;
        return new AxisAlignedBB(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    public static AxisAlignedBB adapter(EnumFacing side)
    {
        double x = 0.5 + side.getXOffset() * 0.46875;
        double y = 0.5 + side.getYOffset() * 0.46875;
        double z = 0.5 + side.getZOffset() * 0.46875;
        double sx = side.getAxis() == EnumFacing.Axis.X ? 0.03125 : 0.3125;
        double sy = side.getAxis() == EnumFacing.Axis.Y ? 0.03125 : 0.3125;
        double sz = side.getAxis() == EnumFacing.Axis.Z ? 0.03125 : 0.3125;
        return new AxisAlignedBB(x - sx, y - sy, z - sz, x + sx, y + sy, z + sz);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull AxisAlignedBB getBoundingBox(@NotNull IBlockState state, IBlockAccess world, @NotNull BlockPos pos)
    {
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile && !tile.facade.isEmpty()) return FULL_BLOCK_AABB;
        AxisAlignedBB box = coreShape(world, pos);
        for (EnumFacing side : EnumFacing.VALUES) if (connects(world, pos, side))
        {
            box = box.union(arm(side, !(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)));
            if (!(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)) box = box.union(adapter(side));
        }
        return box;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(@NotNull IBlockState state, World world, @NotNull BlockPos pos, @NotNull AxisAlignedBB entityBox, @NotNull List<AxisAlignedBB> boxes, Entity entity, boolean actual)
    {
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile && !tile.facade.isEmpty())
        {
            addCollisionBoxToList(pos, entityBox, boxes, FULL_BLOCK_AABB);
            return;
        }
        addCollisionBoxToList(pos, entityBox, boxes, coreShape(world, pos));
        for (EnumFacing side : EnumFacing.VALUES) if (connects(world, pos, side))
        {
            addCollisionBoxToList(pos, entityBox, boxes, arm(side, !(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)));
            if (!(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)) addCollisionBoxToList(pos, entityBox, boxes, adapter(side));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public RayTraceResult collisionRayTrace(@NotNull IBlockState state, World world, @NotNull BlockPos pos, @NotNull Vec3d start, @NotNull Vec3d end)
    {
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile && !tile.facade.isEmpty()) return rayTrace(pos, start, end, FULL_BLOCK_AABB);
        RayTraceResult closest = rayTrace(pos, start, end, coreShape(world, pos));
        if (closest != null) closest.subHit = -1;
        for (EnumFacing side : EnumFacing.VALUES)
        {
            if (!connects(world, pos, side)) continue;
            RayTraceResult hit = rayTrace(pos, start, end, arm(side, !(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit)));
            if (hit != null) hit.subHit = world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit ? -1 : side.getIndex();
            if (!(world.getTileEntity(pos.offset(side)) instanceof TileEntityConduit))
            {
                RayTraceResult plate = rayTrace(pos, start, end, adapter(side));
                if (plate != null) plate.subHit = side.getIndex();
                if (plate != null && (hit == null || start.squareDistanceTo(plate.hitVec) < start.squareDistanceTo(hit.hitVec))) hit = plate;
            }
            if (hit != null && (closest == null || start.squareDistanceTo(hit.hitVec) < start.squareDistanceTo(closest.hitVec)))
            {
                closest = hit;

            }
        }
        return closest;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@NotNull IBlockState state, World world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos from)
    {
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile) tile.refreshConnections();
    }

    @Override
    public boolean onBlockActivated(World world, @NotNull BlockPos pos, @NotNull IBlockState state, @NotNull EntityPlayer player, @NotNull EnumHand hand, @NotNull EnumFacing face, float x, float y, float z)
    {
        if (!(world.getTileEntity(pos) instanceof TileEntityConduit tile)) return false;
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && held.getItem() instanceof ItemBlock item && !(item.getBlock() instanceof BlockConduit) && item.getBlock().getDefaultState().isFullCube())
        {
            if (!world.isRemote && tile.facade.isEmpty())
            {
                tile.facade = held.copy();
                tile.facade.setCount(1);
                if (!player.isCreative()) held.shrink(1);
                tile.changed();
            }
            return true;
        }
        if (player.isSneaking() && held.getItem() instanceof net.vyroxes.minersprosperity.item.ItemConduitProbe)
        {
            if (!world.isRemote && !tile.facade.isEmpty())
            {
                spawnAsEntity(world, pos, tile.facade);
                tile.facade = ItemStack.EMPTY;
                tile.changed();
            }
            return true;
        }
        RayTraceResult hit = collisionRayTrace(state, world, pos, player.getPositionEyes(1), player.getPositionEyes(1).add(player.getLookVec().scale(6)));
        if (!tile.facade.isEmpty() || hit == null || hit.subHit < 0) return false;
        if (!world.isRemote)
        {
            int selected = hit.subHit;
            if (held.getItem() == ModItems.WRENCH || player.isSneaking() && held.isEmpty())
            {
                if (tile.getInterfaceResource(selected) == ConduitType.LOGIC_RESOURCE) return true;
                tile.cycleInterfaceMode(selected);
                world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.3F, 1);
                return true;
            }
            player.openGui(MinersProsperity.INSTANCE, GuiHandler.GuiTypes.CONDUIT_DOWN.ordinal() + selected, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void onBlockClicked(@NotNull World world, @NotNull BlockPos pos, @NotNull EntityPlayer player)
    {
        ItemStack wrench = player.getHeldItemMainhand();
        if (wrench.getItem() != ModItems.WRENCH || !player.getHeldItemOffhand().isEmpty()) return;
        if (world.isRemote || !world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, EnumFacing.UP, wrench)) return;
        NonNullList<ItemStack> drops = NonNullList.create();
        getDrops(drops, world, pos, world.getBlockState(pos), 0);
        for (ItemStack drop : drops) spawnAsEntity(world, pos, drop);
        world.setBlockToAir(pos);
        wrench.damageItem(1, player);
        world.playSound(null, pos, SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS, 1, 1);
    }

    @Override
    public boolean removedByPlayer(@NotNull IBlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull EntityPlayer player, boolean willHarvest)
    {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(@NotNull World world, @NotNull EntityPlayer player, @NotNull BlockPos pos, @NotNull IBlockState state, TileEntity tile, @NotNull ItemStack tool)
    {
        super.harvestBlock(world, player, pos, state, tile, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void onBlockPlacedBy(World world, @NotNull BlockPos pos, @NotNull IBlockState state, net.minecraft.entity.@NotNull EntityLivingBase placer, @NotNull ItemStack stack)
    {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityConduit tile) tile.changed();
    }

    @Override
    public void breakBlock(@NotNull World world, @NotNull BlockPos pos, @NotNull IBlockState state)
    {
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile) ConduitNetworks.broken(tile, type);
        super.breakBlock(world, pos, state);
    }

    @Override
    public void getDrops(@NotNull NonNullList<ItemStack> drops, IBlockAccess world, @NotNull BlockPos pos, @NotNull IBlockState state, int fortune)
    {
        ItemStack stack = new ItemStack(this);
        if (world.getTileEntity(pos) instanceof TileEntityConduit tile)
        {
            NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
            ConduitNetworks.prepareDrop(data, tile, type);
            if (!tile.facade.isEmpty()) drops.add(tile.facade.copy());
            NBTTagList payloads = new NBTTagList();
            boolean hasPayload = false;
            for (net.minecraft.nbt.NBTBase entry : data.getTagList("Ports", 10))
            {
                NBTTagCompound port = (NBTTagCompound) entry;
                NBTTagCompound payload = new NBTTagCompound();
                if (!new ItemStack(port.getCompoundTag("PendingItem")).isEmpty())
                    payload.setTag("PendingItem", port.getCompoundTag("PendingItem"));
                if (port.hasKey("PendingFluid", 10)) payload.setTag("PendingFluid", port.getCompoundTag("PendingFluid"));
                if (port.getInteger("PendingEnergy") > 0) payload.setInteger("PendingEnergy", port.getInteger("PendingEnergy"));
                NBTTagList transits = port.getTagList("Transits", 10);
                if (!transits.isEmpty()) payload.setTag("Transits", transits);
                if (!payload.isEmpty()) hasPayload = true;
                payloads.appendTag(payload);
            }
            if (hasPayload)
            {
                NBTTagCompound contents = new NBTTagCompound();
                contents.setTag("Ports", payloads);
                stack.setTagInfo("BlockEntityTag", contents);
            }
        }
        drops.add(stack);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(@NotNull IBlockState state)
    {
        return type != null && type.supports(3);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(@NotNull IBlockState state, IBlockAccess world, @NotNull BlockPos pos, @NotNull EnumFacing side)
    {
        return world.getTileEntity(pos) instanceof TileEntityConduit tile ? tile.signals[side.getOpposite().getIndex()] : 0;
    }

    @Override
    public boolean canConnectRedstone(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos, EnumFacing side)
    {
        return type.supports(3);
    }
}