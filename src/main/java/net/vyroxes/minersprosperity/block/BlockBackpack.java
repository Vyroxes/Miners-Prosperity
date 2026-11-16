package net.vyroxes.minersprosperity.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.tile.TileEntityBackpack;
import org.jetbrains.annotations.NotNull;

public class BlockBackpack extends Block
{
    public static final net.minecraft.block.properties.PropertyDirection FACING = net.minecraft.block.properties.PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public BlockBackpack()
    {
        super(Material.CLOTH);
        setRegistryName(Tags.MODID, "placed_backpack");
        setTranslationKey("placed_backpack");
        setHardness(0.8F);
        setSoundType(net.minecraft.block.SoundType.CLOTH);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected net.minecraft.block.state.@NotNull BlockStateContainer createBlockState()
    {
        return new net.minecraft.block.state.BlockStateContainer(this, FACING);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rotation)
    {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState withMirror(@NotNull IBlockState state, net.minecraft.util.Mirror mirror)
    {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public boolean hasTileEntity(@NotNull IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@NotNull World world, @NotNull IBlockState state)
    {
        return new TileEntityBackpack();
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
    public @NotNull EnumBlockRenderType getRenderType(@NotNull IBlockState state)
    {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull AxisAlignedBB getBoundingBox(IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos)
    {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z
                ? new AxisAlignedBB(3.0/16, 0, 2.0/16, 13.0/16, 13.0/16, 14.0/16)
                : new AxisAlignedBB(2.0/16, 0, 3.0/16, 14.0/16, 13.0/16, 13.0/16);
    }

    @Override
    public boolean onBlockActivated(World world, @NotNull BlockPos pos, @NotNull IBlockState state, @NotNull EntityPlayer player,
                                    @NotNull EnumHand hand, @NotNull EnumFacing face, float x, float y, float z)
    {
        if (!world.isRemote)
        {
            player.openGui(MinersProsperity.INSTANCE, GuiHandler.GuiTypes.PLACED_BACKPACK.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            world.playSound(null, pos, net.minecraft.init.SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                    net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.2F);
        }
        return true;
    }

    @Override
    public void getDrops(@NotNull NonNullList<ItemStack> drops, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                         @NotNull IBlockState state, int fortune)
    {
    }

    @Override
    public void breakBlock(World world, @NotNull BlockPos pos, @NotNull IBlockState state)
    {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileEntityBackpack)
        {
            ItemStack stack = ((TileEntityBackpack) tile).getBackpack();
            if (!stack.isEmpty()) spawnAsEntity(world, pos, stack.copy());
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public @NotNull ItemStack getPickBlock(@NotNull IBlockState state, net.minecraft.util.math.@NotNull RayTraceResult target, World world, @NotNull BlockPos pos,
                                           @NotNull EntityPlayer player)
    {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityBackpack ? ((TileEntityBackpack) tile).getBackpack().copy() : ItemStack.EMPTY;
    }
}