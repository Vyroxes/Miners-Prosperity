package net.vyroxes.minersprosperity.block;

import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.init.*;
import net.vyroxes.minersprosperity.storage.*;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;

import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class BlockStorageCrate extends BlockBase
{
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyInteger FORMS = PropertyInteger.create("forms", 1, 4);
    public final StorageCrateTier tier;
    public final int compartments;
    public final boolean compacting;

    public BlockStorageCrate(
            @NotNull String name,
            @NotNull StorageCrateTier tier,
            int compartments,
            boolean compacting)
    {
        super(name, Material.IRON, MapColor.STONE, false);
        this.tier = tier;
        this.compartments = compartments;
        this.compacting = compacting;
        setHardness(2.5F);
        setResistance(10);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(
                blockState
                        .getBaseState()
                        .withProperty(FACING, EnumFacing.NORTH)
                        .withProperty(FORMS, 1));
        ModItems.ITEMS.add(new ItemBlockStorageCrate(this).setRegistryName(getRegistryName()));
    }

    @Override
    protected @NotNull BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, FACING, FORMS);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(FORMS, compacting ? ((meta >> 2) & 3) + 1 : 1);
    }

    @Override
    public int getMetaFromState(@NotNull IBlockState state)
    {
        return state.getValue(FACING).getHorizontalIndex()
                | (compacting ? (state.getValue(FORMS) - 1) << 2 : 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull IBlockState withRotation(@NotNull IBlockState state, Rotation rotation)
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
    public boolean hasTileEntity(@NotNull IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@NotNull World world, @NotNull IBlockState state)
    {
        return new TileEntityStorageCrate(tier, compartments, compacting);
    }

    @Override
    public void onBlockPlacedBy(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull IBlockState state,
            @NotNull EntityLivingBase placer,
            @NotNull ItemStack stack)
    {
        world.setBlockState(
                pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityStorageCrate crate && stack.hasTagCompound())
        {
            crate.readData(stack.getTagCompound().getCompoundTag("StorageCrate"), false);
            crate.changed();
        }
    }

    public static int hitSlot(
            @NotNull TileEntityStorageCrate crate,
            EnumFacing facing,
            double x,
            double y,
            double z)
    {
        double u =
                facing == EnumFacing.NORTH
                        ? 1 - x
                        : facing == EnumFacing.SOUTH ? x : facing == EnumFacing.WEST ? z : 1 - z;
        if (crate.slots() == 1) return 0;
        if (crate.slots() == 2) return y >= .5 ? 0 : 1;
        if (crate.slots() == 3) return y >= .5 ? 0 : u < .5 ? 1 : 2;
        int slot = (y >= .5 ? 0 : 2) + (u >= .5 ? 1 : 0);
        return Math.min(crate.slots() - 1, slot);
    }

    @Override
    public boolean onBlockActivated(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull IBlockState state,
            @NotNull EntityPlayer player,
            @NotNull EnumHand hand,
            @NotNull EnumFacing face,
            float x,
            float y,
            float z)
    {
        if (player.isSneaking() && player.getHeldItem(hand).getItem() instanceof ItemBlock) return false;
        if (hand != EnumHand.MAIN_HAND) return true;
        if (world.isRemote) return true;
        if (!(world.getTileEntity(pos) instanceof TileEntityStorageCrate crate)) return false;
        if (player.isSneaking() || face != state.getValue(FACING))
        {
            player.openGui(
                    MinersProsperity.INSTANCE,
                    GuiHandler.GuiTypes.STORAGE_CRATE.ordinal(),
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
            return true;
        }
        int revision = crate.revision;
        int slot = hitSlot(crate, state.getValue(FACING), x, y, z);
        ItemStack held = player.getHeldItem(hand);
        long tick = world.getTotalWorldTime();
        boolean twice =
                player.getUniqueID().equals(crate.lastPlayer)
                        && tick - crate.lastClick >= 0
                        && tick - crate.lastClick <= 10
                        && slot == crate.lastSlot;
        if (twice && !crate.lastItem.isEmpty())
        {
            for (int i = 0; i < player.inventory.mainInventory.size(); i++)
            {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (CrateCompacting.same(stack, crate.lastItem))
                    player.inventory.setInventorySlotContents(i, crate.insert(slot, stack, false));
            }
            crate.lastClick = -100;
        }
        else if (!held.isEmpty())
        {
            ItemStack remainder = crate.insert(slot, held, false);
            if (remainder.getCount() != held.getCount())
            {
                player.setHeldItem(hand, remainder);
            }
        }
        if (!twice)
        {
            crate.lastItem = crate.template(slot);
            crate.lastPlayer = player.getUniqueID();
            crate.lastSlot = slot;
            crate.lastClick = tick;
        }
        if (revision != crate.revision) playTransferSound(crate, true);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return true;
    }

    public static void take(
            @NotNull TileEntityStorageCrate crate,
            @NotNull EntityPlayer player,
            int slot,
            int amount,
            boolean playSound)
    {
        ItemStack planned = crate.extract(slot, amount, true);
        if (planned.isEmpty()) return;
        ItemStack remainder =
                ItemHandlerHelper.insertItemStacked(
                        new InvWrapper(player.inventory), planned, true);
        int accepted = planned.getCount() - remainder.getCount();
        if (accepted > 0)
        {
            ItemHandlerHelper.insertItemStacked(
                    new InvWrapper(player.inventory), crate.extract(slot, accepted, false), false);
            if (playSound) playTransferSound(crate, false);
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }

    public static void playTransferSound(@NotNull TileEntityStorageCrate crate, boolean inserting)
    {
        if (crate.getWorld().isRemote) return;
        crate.getWorld().playSound(null, crate.getPos(),
                net.minecraft.init.SoundEvents.ENTITY_ITEM_PICKUP,
                net.minecraft.util.SoundCategory.BLOCKS, 0.35F, inserting ? 1.2F : 0.8F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event)
    {
        if (!event.isCanceled()
                && event.getHand() == EnumHand.MAIN_HAND
                && event.getEntityPlayer().isSneaking()
                && event.getWorld().getBlockState(event.getPos()).getBlock()
                        instanceof BlockStorageCrate)
        {
            boolean placingBlock = event.getEntityPlayer().getHeldItem(event.getHand()).getItem() instanceof ItemBlock;
            event.setUseBlock(placingBlock ? Event.Result.DENY : Event.Result.ALLOW);
            event.setUseItem(placingBlock ? Event.Result.DEFAULT : Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event)
    {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (event.isCanceled()
                || !(world.getBlockState(pos).getBlock() instanceof BlockStorageCrate)
                || event.getFace() != world.getBlockState(pos).getValue(FACING)) return;
        EntityPlayer player = event.getEntityPlayer();
        event.setCanceled(true);
        if (world.isRemote
                || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(
                        pos,
                        event.getFace() == null ? EnumFacing.UP : event.getFace(),
                        player.getHeldItemMainhand())) return;
        if (!(world.getTileEntity(pos) instanceof TileEntityStorageCrate crate)) return;
        if (player.getHeldItemMainhand().getItem() == ModItems.WRENCH)
        {
            spawnAsEntity(
                    world,
                    pos,
                    ((BlockStorageCrate) world.getBlockState(pos).getBlock()).drop(crate));
            world.setBlockToAir(pos);
            player.getHeldItemMainhand().damageItem(1, player);
            return;
        }
        Vec3d hit = event.getHitVec();
        int slot =
                hit == null
                        ? 0
                        : hitSlot(
                                crate,
                                world.getBlockState(pos).getValue(FACING),
                                hit.x - pos.getX(),
                                hit.y - pos.getY(),
                                hit.z - pos.getZ());
        take(crate, player, slot, player.isSneaking() ? 64 : 1, true);
    }

    private @NotNull ItemStack drop(@NotNull TileEntityStorageCrate crate)
    {
        ItemStack stack = new ItemStack(this);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("StorageCrate", crate.data());
        stack.setTagCompound(tag);
        return stack;
    }

    @Override
    public void getDrops(
            NonNullList<ItemStack> drops,
            @NotNull IBlockAccess world,
            @NotNull BlockPos pos,
            @NotNull IBlockState state,
            int fortune)
    {
        TileEntity tile = world.getTileEntity(pos);
        drops.add(
                tile instanceof TileEntityStorageCrate
                        ? drop((TileEntityStorageCrate) tile)
                        : new ItemStack(this));
    }

    @Override
    public boolean removedByPlayer(
            @NotNull IBlockState state,
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull EntityPlayer player,
            boolean willHarvest)
    {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(
            @NotNull World world,
            @NotNull EntityPlayer player,
            @NotNull BlockPos pos,
            @NotNull IBlockState state,
            TileEntity tile,
            @NotNull ItemStack tool)
    {
        super.harvestBlock(world, player, pos, state, tile, tool);
        world.setBlockToAir(pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(@NotNull IBlockState state)
    {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(
            @NotNull IBlockState state,
            @NotNull IBlockAccess world,
            @NotNull BlockPos pos,
            @NotNull EnumFacing side)
    {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityStorageCrate
                ? ((TileEntityStorageCrate) tile).signal()
                : 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(@NotNull IBlockState state)
    {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(
            @NotNull IBlockState state, @NotNull World world, @NotNull BlockPos pos)
    {
        return getWeakPower(state, world, pos, EnumFacing.UP);
    }
}