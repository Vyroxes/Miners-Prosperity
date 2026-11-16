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
import net.vyroxes.minersprosperity.tile.TileEntityTrashCan;
import org.jetbrains.annotations.NotNull;

public class BlockMachineTrashCan extends BlockMachine
{
    public BlockMachineTrashCan()
    {
        super("trash_can", Material.IRON, MapColor.GRAY, SoundType.METAL, MinersProsperity.blocks_tab,
                "pickaxe", 0, 5, 10, GuiHandler.GuiTypes.TRASH_CAN);
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void addInformation(@NotNull net.minecraft.item.ItemStack stack, @javax.annotation.Nullable World world,
                               @NotNull java.util.List<String> tooltip, @NotNull net.minecraft.client.util.ITooltipFlag flag)
    {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.trash_can"));
    }

    @Override
    public TileEntityMachine getTileEntity(World world, BlockPos pos)
    {
        return (TileEntityTrashCan) world.getTileEntity(pos);
    }

    @Override
    public TileEntity createNewTileEntity(@NotNull World world, int meta)
    {
        return new TileEntityTrashCan();
    }

    @Override
    public int getLightValue(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos)
    {
        return 0;
    }
}