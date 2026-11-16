package net.vyroxes.minersprosperity.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import org.jetbrains.annotations.NotNull;

public class EnchantmentQuickMining extends Enchantment
{

    public EnchantmentQuickMining(Rarity rarityIn, EnumEnchantmentType typeIn, EntityEquipmentSlot[] slots) {
        super(rarityIn, typeIn, slots);
        this.setRegistryName("quick_mining");
        this.setName("quick_mining");
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 15;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return 100;
    }

    @Override
    public boolean canApply(@NotNull ItemStack stack) {
        return stack.getItem() == Items.BOOK || QuickMiningLoader.isSupportedTool(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack)
    {
        return QuickMiningLoader.isSupportedTool(stack);
    }
}