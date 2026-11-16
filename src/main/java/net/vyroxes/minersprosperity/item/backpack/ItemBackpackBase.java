package net.vyroxes.minersprosperity.item.backpack;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.item.ItemBase;
import org.jetbrains.annotations.NotNull;

public abstract class ItemBackpackBase extends ItemBase
{
    public ItemBackpackBase(String name)
    {
        super(name);
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || !ItemStack.areItemsEqual(oldStack, newStack);
    }

    @Override
    public ICapabilityProvider initCapabilities(@NotNull ItemStack stack, @Nullable NBTTagCompound nbt)
    {
        BackpackCapabilityProvider provider = new BackpackCapabilityProvider(stack, getInventorySize(), getConfigurationSize());
        if (nbt != null && nbt.hasKey("Inventory"))
        {
            provider.deserializeNBT(nbt);
        }
        return provider;
    }

    public abstract int getInventorySize();

    public abstract int getInventoryColumns();

    public abstract int getConfigurationSize();

    public abstract ResourceLocation getTexture();

    public abstract ResourceLocation getTexture2();

    public abstract ResourceLocation getConfigurationTexture();

    public abstract ResourceLocation getConfigurationTexture2();

    public abstract BackpackTier getTier();

    @Override
    public int getItemStackLimit(@NotNull ItemStack stack)
    {
        return 1;
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World worldIn, EntityPlayer playerIn, @NotNull EnumHand handIn)
    {
        ItemStack itemStack = playerIn.getHeldItem(handIn);

        if (handIn == EnumHand.MAIN_HAND) {
            if (!worldIn.isRemote) {
                if (playerIn.isSneaking()) {
                    playerIn.openGui(MinersProsperity.INSTANCE, GuiHandler.GuiTypes.BACKPACK_CONFIGURATION.ordinal(), worldIn, (int) playerIn.posX, 0, (int) playerIn.posZ);
                } else {
                    playerIn.openGui(MinersProsperity.INSTANCE, GuiHandler.GuiTypes.BACKPACK.ordinal(), worldIn, (int) playerIn.posX, 0, (int) playerIn.posZ);
                }
                itemStack.getOrCreateSubCompound("BackpackData").setBoolean("IsOpen", true);
            }
            playerIn.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
        }

        return new ActionResult<>(EnumActionResult.FAIL, itemStack);
    }

    @Override
    public @NotNull EnumActionResult onItemUse(EntityPlayer player, @NotNull World world, net.minecraft.util.math.@NotNull BlockPos pos,
                                               @NotNull EnumHand hand, @NotNull EnumFacing face, float hitX, float hitY, float hitZ)
    {
        if (!player.isSneaking() || hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS;
        ItemStack stack = player.getHeldItem(hand);
        net.minecraft.util.math.BlockPos target = world.getBlockState(pos).getBlock().isReplaceable(world, pos) ? pos : pos.offset(face);
        net.minecraft.block.Block block = net.vyroxes.minersprosperity.init.ModBlocks.PLACED_BACKPACK;
        if (!player.canPlayerEdit(target, face, stack) || !world.mayPlace(block, target, false, face, player)) return EnumActionResult.FAIL;
        if (!world.isRemote && world.setBlockState(target, block.getDefaultState().withProperty(net.vyroxes.minersprosperity.block.BlockBackpack.FACING, player.getHorizontalFacing().getOpposite()), 3))
        {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(target);
            if (tile instanceof net.vyroxes.minersprosperity.tile.TileEntityBackpack)
            {
                ItemStack placed = stack.copy();
                placed.setCount(1);
                ((net.vyroxes.minersprosperity.tile.TileEntityBackpack) tile).setBackpack(placed);
                stack.shrink(1);
                world.playSound(null, target, SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    public static ItemStackHandler getBackpackInventory(ItemStack stack)
    {
        IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof BackpackCapabilityProvider.Inventory)
            return ((BackpackCapabilityProvider.Inventory) handler).manual;
        return new ItemStackHandler(0);
    }

    public static ItemStackHandler getConfigurationInventory(ItemStack stack)
    {
        IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        if (handler instanceof ItemStackHandler)
        {
            return (ItemStackHandler) handler;
        }
        return new ItemStackHandler(0);
    }

    public static List<ItemStack> getAllBackpacksFromPlayer(EntityPlayer player)
    {
        List<ItemStack> backpacks = new ArrayList<>();

        if (Loader.isModLoaded("baubles"))
        {
            backpacks.addAll(BaublesCompat.getBackpackFromBaubles(player));
        }

        for (ItemStack stack : player.inventory.offHandInventory)
        {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBackpackBase)
            {
                backpacks.add(stack);
            }
        }

        for (ItemStack stack : player.inventory.mainInventory)
        {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBackpackBase)
            {
                backpacks.add(stack);
            }
        }

        return backpacks;
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, @Nullable World worldIn, @NotNull List<String> tooltip, @NotNull ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(I18n.format("tooltip.inventory_slots", getInventorySize()));

        NBTTagCompound backpackData = BackpackSettings.data(stack);
        boolean autoCollect = backpackData.getBoolean("AutoCollect");
        String statusTooltip = TextFormatting.GRAY + I18n.format("tooltip.backpack.auto_collect") + " " + (autoCollect ? TextFormatting.GREEN + I18n.format("tooltip.on") : TextFormatting.RED + I18n.format("tooltip.off"));
        tooltip.add(statusTooltip);
        int deposit = BackpackSettings.get(stack, "Deposit");
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.backpack.hit_deposit") + " "
                + (deposit == 0 ? TextFormatting.RED : TextFormatting.GREEN)
                + I18n.format("gui.backpack.value.Deposit." + deposit));
    }
}