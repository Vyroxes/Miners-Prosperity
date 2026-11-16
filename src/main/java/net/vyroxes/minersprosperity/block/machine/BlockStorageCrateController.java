package net.vyroxes.minersprosperity.block.machine;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BlockStorageCrateController extends BlockMachine
{
    public BlockStorageCrateController()
    {
        super("storage_crate_controller", Material.IRON, MapColor.STONE, SoundType.METAL,
                MinersProsperity.blocks_tab, "pickaxe", 0, 3, 10, GuiHandler.GuiTypes.STORAGE_CRATE_CONTROLLER);
    }

    @Override
    public TileEntityMachine getTileEntity(World world, BlockPos pos)
    {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityStorageCrateController ? (TileEntityStorageCrateController) tile : null;
    }

    @Override
    public TileEntity createNewTileEntity(@NotNull World world, int meta)
    {
        return new TileEntityStorageCrateController();
    }

    @Override
    public int getLightValue(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos)
    {
        return 0;
    }

    @Override
    public void randomDisplayTick(@NotNull IBlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull Random random)
    {
    }
}