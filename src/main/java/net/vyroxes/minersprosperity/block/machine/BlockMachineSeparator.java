package net.vyroxes.minersprosperity.block.machine;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.init.ModBlocks;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;
import net.vyroxes.minersprosperity.tile.TileEntitySeparator;
import net.vyroxes.minersprosperity.annotation.NonnullByDefault;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import org.jetbrains.annotations.Nullable;
import java.util.Random;

public class BlockMachineSeparator extends BlockMachine {
    public BlockMachineSeparator(String name) {
        super(name, Material.IRON, MapColor.STONE, SoundType.STONE, MinersProsperity.blocks_tab, "pickaxe", 0, 5.0F, 10.0F, GuiHandler.GuiTypes.SEPARATOR);
    }

    @NonnullByDefault
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(ModBlocks.SEPARATOR);
    }

    @Override
    @NonnullByDefault
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!player.isSneaking() && TileEntitySeparator.isFluidContainer(player.getHeldItem(hand)))
        {
            if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntitySeparator separator
                    && FluidUtil.interactWithFluidHandler(player, hand, separator.getContainerFluidHandler()))
            {
                world.notifyBlockUpdate(pos, state, state, 2);
            }
            return true;
        }

        return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public TileEntityMachine getTileEntity(World world, BlockPos pos) {
        return (TileEntitySeparator) world.getTileEntity(pos);
    }

    @Override
    @NonnullByDefault
    public @Nullable TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySeparator();
    }
}