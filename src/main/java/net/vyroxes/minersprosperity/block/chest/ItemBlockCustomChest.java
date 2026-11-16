package net.vyroxes.minersprosperity.block.chest;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nullable;
import java.util.List;
import net.vyroxes.minersprosperity.client.render.ItemRenderCustomChest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemBlockCustomChest extends ItemBlock {
    private final ChestTier tier;

    public ItemBlockCustomChest(Block block, ChestTier tier) {
        super(block);
        this.tier = tier;
        this.setRegistryName(Objects.requireNonNull(block.getRegistryName()));

        this.setTileEntityItemStackRenderer(new ItemRenderCustomChest());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@NotNull ItemStack stack, @Nullable World world, @NotNull List<String> tooltip, @NotNull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format("tooltip.inventory_slots", tier.inventorySize));
    }

    public ChestTier getTier() {
        return tier;
    }
}