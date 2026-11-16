package net.vyroxes.minersprosperity.block;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.vyroxes.minersprosperity.storage.StorageCrateTier;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ItemBlockStorageCrate extends ItemBlock
{
    public ItemBlockStorageCrate(@NotNull BlockStorageCrate block)
    {
        super(block);
    }

    @Override
    public void addInformation(
            @NotNull ItemStack stack,
            @Nullable World world,
            @NotNull List<String> tooltip,
            @NotNull ITooltipFlag flag)
    {
        BlockStorageCrate crate = (BlockStorageCrate) block;
        tooltip.add(TextFormatting.AQUA + I18n.format("tooltip.storage_crate.capacity_label") + ": "
                + TextFormatting.GRAY + I18n.format("tooltip.storage_crate.stacks",
                crate.tier == StorageCrateTier.CREATIVE ? I18n.format("gui.energy_infinite") : crate.tier.stacks()));
        TileEntityStorageCrate contents = contents(stack);
        tooltip.add(TextFormatting.AQUA + I18n.format("tooltip.storage_crate.compartments") + ": "
                + TextFormatting.GRAY + (crate.compacting ? contents == null ? "1-4" : contents.slots() : crate.compartments));
        if (contents == null) return;
        for (int i = 0; i < contents.slots(); i++)
        {
            ItemStack item = contents.template(i);
            if (item.isEmpty()) continue;
            tooltip.add(TextFormatting.AQUA + item.getDisplayName() + ": " + TextFormatting.GRAY
                    + (contents.creative() ? I18n.format("gui.energy_infinite") : Long.toString(contents.count(i))));
        }
    }

    private @Nullable TileEntityStorageCrate contents(@NotNull ItemStack stack)
    {
        if (!stack.hasTagCompound() || !Objects.requireNonNull(stack.getTagCompound()).hasKey("StorageCrate", 10)) return null;
        BlockStorageCrate crate = (BlockStorageCrate) block;
        TileEntityStorageCrate contents = new TileEntityStorageCrate(crate.tier, crate.compartments, crate.compacting);
        contents.readData(stack.getTagCompound().getCompoundTag("StorageCrate"), false);
        return contents;
    }

    @Override
    public boolean showDurabilityBar(@NotNull ItemStack stack)
    {
        return getDurabilityForDisplay(stack) < 1;
    }

    @Override
    public double getDurabilityForDisplay(@NotNull ItemStack stack)
    {
        TileEntityStorageCrate contents = contents(stack);
        if (contents == null) return 1;
        if (contents.isCompacting()) return 1 - contents.fill(0);
        double fill = 0;
        for (int i = 0; i < contents.slots(); i++) fill += contents.fill(i);
        return 1 - fill / contents.slots();
    }

    @Override
    public int getRGBDurabilityForDisplay(@NotNull ItemStack stack)
    {
        return 0x35B9FF;
    }
}