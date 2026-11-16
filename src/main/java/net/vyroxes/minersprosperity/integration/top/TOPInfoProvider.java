package net.vyroxes.minersprosperity.integration.top;

import mcjty.theoneprobe.api.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraftforge.energy.IEnergyStorage;
import net.vyroxes.minersprosperity.machine.FluidTankTier;
import net.vyroxes.minersprosperity.tile.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.vyroxes.minersprosperity.fluid.CustomFluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.item.Item;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.vyroxes.minersprosperity.config.ModConfig;

public class TOPInfoProvider implements IProbeInfoProvider, IProbeConfigProvider
{
    private static final int BAR_WIDTH = 140;
    private static final String[] FLUID_UNITS = {"mB", "B", "kB", "MB"};
    private static final String[] ENERGY_UNITS = {"FE", "kFE", "MFE", "GFE"};

    @Override
    public String getID() {
        return "minersprosperity:alloy_furnace";
    }

    public float calculateLevelsFromXP(int xp)
    {
        int level = 0;

        while (xp > 0)
        {
            int xpToNextLevel;

            if (level < 16)
            {
                xpToNextLevel = 2 * level + 7;
            }
            else if (level < 31)
            {
                xpToNextLevel = 5 * level - 38;
            }
            else
            {
                xpToNextLevel = 9 * level - 158;
            }

            if (xp >= xpToNextLevel)
            {
                xp -= xpToNextLevel;
                level++;
            }
            else
            {
                return level + (float) xp / xpToNextLevel;
            }
        }

        return level;
    }

    @Override
    public void getProbeConfig(IProbeConfig config, EntityPlayer player, World world, Entity entity, IProbeHitEntityData hit)
    {
    }

    @Override
    public void getProbeConfig(IProbeConfig config, EntityPlayer player, World world, IBlockState state, IProbeHitData hit)
    {
        if (!ModConfig.INTEGRATION.theOneProbeCompat || !(world.getTileEntity(hit.getPos()) instanceof TileEntityMachine)) return;
        config.setRFMode(0);
        config.showTankSetting(IProbeConfig.ConfigMode.NOT);
        if (world.getTileEntity(hit.getPos()) instanceof TileEntityTrashCan) config.showChestContents(IProbeConfig.ConfigMode.NOT);
    }

    private String formatAmount(int amount, long divisor)
    {
        BigDecimal value = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(divisor), 3, RoundingMode.HALF_UP);
        if (amount > 0 && value.signum() == 0) return "<0.001";
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatStorage(int amount, int capacity, String[] units, boolean showCapacity)
    {
        int reference = showCapacity ? capacity : amount;
        long divisor = 1;
        int unit = 0;
        while (unit < units.length - 1 && reference / divisor >= 1000)
        {
            divisor *= 1000;
            unit++;
        }
        String text = this.formatAmount(amount, divisor);
        if (showCapacity) text += " / " + this.formatAmount(capacity, divisor);
        text += " " + units[unit];
        if (showCapacity) text += " (" + (capacity > 0 ? (long) amount * 100 / capacity : 0) + "%)";
        return text;
    }

    @SuppressWarnings("deprecation")
    private void addEnergyInfo(IProbeInfo info, IEnergyStorage energy, boolean creative)
    {
        if (energy == null || energy.getMaxEnergyStored() <= 0) return;
        int capacity = energy.getMaxEnergyStored();
        int amount = Math.max(0, Math.min(energy.getEnergyStored(), capacity));
        info.progress(amount, (long) capacity, info.defaultProgressStyle()
                .filledColor(mcjty.theoneprobe.config.Config.rfbarFilledColor)
                .alternateFilledColor(mcjty.theoneprobe.config.Config.rfbarAlternateFilledColor)
                .borderColor(mcjty.theoneprobe.config.Config.rfbarBorderColor)
                .numberFormat(NumberFormat.NONE).prefix(creative ? net.minecraft.util.text.translation.I18n.translateToLocal("gui.energy_infinite") : this.formatStorage(amount, capacity, ENERGY_UNITS, true))
                .suffix("").showText(true).width(BAR_WIDTH));
    }

    @SuppressWarnings("deprecation")
    private void addTankInfo(IProbeInfo info, FluidStack fluid, int capacity, boolean experienceTank, boolean creative)
    {
        if (capacity <= 0) return;
        int amount = fluid == null ? 0 : Math.max(0, Math.min(fluid.amount, capacity));
        String label = experienceTank ? "gui.experience_tank.name" : "gui.fluid";
        String fluidName = fluid == null ? IProbeInfo.STARTLOC + "gui.fluid_empty" + IProbeInfo.ENDLOC : fluid.getLocalizedName();
        info.text(TextFormatting.AQUA + IProbeInfo.STARTLOC + label + IProbeInfo.ENDLOC + TextFormatting.GRAY + ": " + fluidName);
        boolean experienceFluid = experienceTank || fluid != null && fluid.isFluidEqual(TileEntityMachine.getConfiguredXpFluid());
        String text = creative ? net.minecraft.util.text.translation.I18n.translateToLocal("gui.energy_infinite")
                : this.formatStorage(amount, capacity, FLUID_UNITS, !experienceFluid);
        IProgressStyle style = info.defaultProgressStyle()
                .filledColor(experienceFluid ? 0xFF55DD22 : 0xFF3399FF)
                .alternateFilledColor(experienceFluid ? 0xFF2B8811 : 0xFF1B5CB8)
                .borderColor(mcjty.theoneprobe.config.Config.rfbarBorderColor)
                .numberFormat(NumberFormat.NONE).prefix(text).suffix("").showText(true).width(BAR_WIDTH);
        info.progress(amount, (long) capacity, style);
        if (experienceTank && amount > 0 && ModConfig.MACHINES.mbToXpRatio > 0)
        {
            int mbPerXp = Math.max(1, 1000 / ModConfig.MACHINES.mbToXpRatio);
            String levels = String.format(java.util.Locale.ROOT, "%.1f", this.calculateLevelsFromXP(amount / mbPerXp));
            info.text(IProbeInfo.STARTLOC + "gui.xp_levels" + IProbeInfo.ENDLOC + ": " + levels);
        }
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer, World world, IBlockState iBlockState, IProbeHitData iProbeHitData)
    {
        if (ModConfig.INTEGRATION.theOneProbeCompat)
        {
            Block block = iBlockState.getBlock();
            boolean vanillaBlock = block.getClass().getName().startsWith("net.minecraft.block.");

            if (vanillaBlock && !ModConfig.INTEGRATION.theOneProbeVanillaTileEntities)
                return;

            TileEntity te = world.getTileEntity(iProbeHitData.getPos());
            if (te == null || te instanceof TileEntityTrashCan)
                return;

            if (!vanillaBlock && !(te instanceof TileEntityMachine) && !(te instanceof TileEntityBackpack)
                    && !(te instanceof TileEntityCustomChest) && !(te instanceof TileEntityWorktable))
                return;

            if (te instanceof TileEntityMachine machine)
            {
                this.addEnergyInfo(iProbeInfo, machine.getEnergyStorage(), machine instanceof net.vyroxes.minersprosperity.tile.TileEntityEnergyStorage && ((net.vyroxes.minersprosperity.tile.TileEntityEnergyStorage) machine).getTier() == net.vyroxes.minersprosperity.machine.EnergyStorageTier.CREATIVE);
                for (int i = 0; i < machine.getFluidTankCount(); i++)
                {
                    CustomFluidTank tank = machine.getFluidTank(i);
                    if (tank != null) this.addTankInfo(iProbeInfo, tank.getFluid(), tank.getCapacity(), i == machine.getExperienceTankIndex(), machine instanceof TileEntityFluidTank && ((TileEntityFluidTank) machine).getTier() == FluidTankTier.CREATIVE);
                }
            }
            else
            {
                IFluidHandler fluidHandler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                if (fluidHandler != null)
                {
                    for (IFluidTankProperties tank : fluidHandler.getTankProperties())
                    {
                        this.addTankInfo(iProbeInfo, tank.getContents(), tank.getCapacity(), false, false);
                    }
                }
            }

            IItemHandler handler = te instanceof TileEntityWorktable
                    ? new net.minecraftforge.items.wrapper.InvWrapper(((TileEntityWorktable) te).grid)
                    : te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null) return;
            boolean detailed = entityPlayer.isSneaking() || probeMode == ProbeMode.EXTENDED;
            boolean sum = detailed ? ModConfig.INTEGRATION.theOneProbeShiftSumItems : ModConfig.INTEGRATION.theOneProbeSumItems;
            int maxRows = Math.max(1, detailed ? ModConfig.INTEGRATION.theOneProbeShiftMaxRows : ModConfig.INTEGRATION.theOneProbeMaxRows);
            int itemsPerRow = detailed ? 1 : Math.max(1, ModConfig.INTEGRATION.theOneProbeItemsPerRow);
            List<InventoryEntry> entries = te instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrate crate ? crateEntries(crate, sum, maxRows * itemsPerRow) : inventoryEntries(handler, sum, maxRows * itemsPerRow,
                    te instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController controller ? slot -> controller.isCreativeSlot(slot) : slot -> false);
            if (entries.isEmpty()) return;
            IProbeInfo vertical = iProbeInfo.vertical(iProbeInfo.defaultLayoutStyle());
            IProbeInfo row = null;
            for (int i = 0; i < entries.size(); i++)
            {
                if (i % itemsPerRow == 0)
                    row = vertical.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
                InventoryEntry entry = entries.get(i);
                row.element(new TOPItemElement(entry.stack, entry.count));
                if (detailed) row.text(" " + entry.stack.getDisplayName());
            }
        }
    }

    private static List<InventoryEntry> crateEntries(net.vyroxes.minersprosperity.tile.TileEntityStorageCrate crate, boolean sum, int limit)
    {
        List<InventoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < crate.slots(); i++)
        {
            ItemStack template = crate.template(i);
            if (template.isEmpty()) continue;
            long count = crate.creative() ? -1 : crate.count(i);
            InventoryEntry existing = null;
            if (sum && !crate.isCompacting())
                for (InventoryEntry entry : entries) if (ItemHandlerHelper.canItemStacksStack(entry.stack, template)) { existing = entry; break; }
            if (existing != null) existing.count = count < 0 || existing.count < 0 ? -1 : existing.count + count;
            else if (entries.size() < limit) { InventoryEntry entry = new InventoryEntry(template); entry.count = count; entries.add(entry); }
        }
        return entries;
    }

    static List<InventoryEntry> inventoryEntries(IItemHandler handler, boolean sum, int limit)
    {
        return inventoryEntries(handler, sum, limit, slot -> false);
    }

    static List<InventoryEntry> inventoryEntries(IItemHandler handler, boolean sum, int limit,
                                                java.util.function.IntPredicate creativeSlot)
    {
        List<InventoryEntry> entries = new ArrayList<>();
        Map<Item, List<InventoryEntry>> byItem = new IdentityHashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            InventoryEntry existing = null;
            if (sum)
            {
                List<InventoryEntry> candidates = byItem.get(stack.getItem());
                if (candidates != null)
                    for (InventoryEntry candidate : candidates)
                        if (ItemHandlerHelper.canItemStacksStack(candidate.stack, stack)) { existing = candidate; break; }
            }
            long count = creativeSlot.test(slot) ? -1 : stack.getCount();
            if (existing != null) existing.count = count < 0 || existing.count < 0 ? -1 : existing.count + count;
            else if (entries.size() < limit)
            {
                InventoryEntry entry = new InventoryEntry(stack);
                entry.count = count;
                entries.add(entry);
                if (sum) byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(entry);
            }

            if (!sum && entries.size() >= limit) break;
        }
        return entries;
    }

    static class InventoryEntry
    {
        final ItemStack stack;
        long count;

        InventoryEntry(ItemStack stack)
        {
            this.stack = stack.copy();
            this.count = stack.getCount();
            this.stack.setCount(1);
        }
    }
}