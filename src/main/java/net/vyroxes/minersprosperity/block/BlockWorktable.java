package net.vyroxes.minersprosperity.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Rotation;
import net.minecraft.util.Mirror;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.tile.TileEntityWorktable;
import org.jetbrains.annotations.NotNull;

public class BlockWorktable extends BlockBase
{
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

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

    public BlockWorktable()
    {
        super("worktable", Material.WOOD, MapColor.WOOD, true);
        setHardness(2.5F);
        setLightOpacity(0);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
    }

    @Override
    protected @NotNull BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, FACING);
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
    public @NotNull IBlockState withRotation(IBlockState state, Rotation rotation)
    {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState withMirror(@NotNull IBlockState state, Mirror mirror)
    {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public @NotNull IBlockState getStateForPlacement(@NotNull World world, @NotNull BlockPos pos, @NotNull EnumFacing side,
                                                     float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, @NotNull EnumHand hand)
    {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean hasTileEntity(@NotNull IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@NotNull World world, @NotNull IBlockState state)
    {
        return new TileEntityWorktable();
    }

    @Override
    public boolean onBlockActivated(World world, @NotNull BlockPos pos, @NotNull IBlockState state, @NotNull EntityPlayer player,
                                              @NotNull EnumHand hand, @NotNull EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote) player.openGui(MinersProsperity.INSTANCE, GuiHandler.GuiTypes.WORKTABLE.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World world, @NotNull BlockPos pos, @NotNull IBlockState state)
    {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileEntityWorktable)
            InventoryHelper.dropInventoryItems(world, pos, ((TileEntityWorktable) tile).grid);
        super.breakBlock(world, pos, state);
    }
}