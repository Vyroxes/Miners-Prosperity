package net.vyroxes.minersprosperity.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ItemConduitProbe extends ItemBase
{
    public ItemConduitProbe()
    {
        super("conduit_probe", 1, 0);
    }

    @Override
    public @NotNull EnumActionResult onItemUseFirst(@NotNull EntityPlayer player, World world, net.minecraft.util.math.@NotNull BlockPos pos, @NotNull EnumFacing side, float x, float y, float z, @NotNull EnumHand hand)
    {
        if (!(world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityConduit tile)) return EnumActionResult.PASS;
        if (!player.isSneaking())
        {
            if (tile.facade.isEmpty()) return EnumActionResult.PASS;
            if (!player.canPlayerEdit(pos, side, player.getHeldItem(hand))) return EnumActionResult.FAIL;
            if (!world.isRemote)
            {
                tile.transparentFacade = !tile.transparentFacade;
                tile.changed();
                player.sendStatusMessage(new TextComponentTranslation(tile.transparentFacade ? "gui.conduit.facade.transparent" : "gui.conduit.facade.opaque"), true);
            }
            return EnumActionResult.SUCCESS;
        }
        if (!player.canPlayerEdit(pos, side, player.getHeldItem(hand))) return EnumActionResult.FAIL;
        if (!world.isRemote)
        {
            ItemStack material = player.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
            if (tile.facade.isEmpty() && material.getItem() instanceof net.minecraft.item.ItemBlock block
                    && !(block.getBlock() instanceof net.vyroxes.minersprosperity.block.BlockConduit) && block.getBlock().getDefaultState().isFullCube())
            {
                tile.facade = material.copy();
                tile.facade.setCount(1);
                if (!player.isCreative()) material.shrink(1);
            }
            else if (!tile.facade.isEmpty())
            {
                net.minecraft.block.Block.spawnAsEntity(world, pos, tile.facade);
                tile.facade = ItemStack.EMPTY;
            }
            tile.changed();
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @NotNull EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote)
        {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
            String key = player.isSneaking() ? "Tier" : "Type";
            int value = (tag.getInteger(key) + 1) % (player.isSneaking() ? 7 : 6);
            tag.setInteger(key, value);
            player.sendStatusMessage(new TextComponentTranslation("gui.conduit.probe." + key.toLowerCase(java.util.Locale.ROOT), new TextComponentTranslation("gui.conduit.probe." + key.toLowerCase(java.util.Locale.ROOT) + "." + value)).setStyle(new Style().setColor(TextFormatting.GRAY)), true);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}