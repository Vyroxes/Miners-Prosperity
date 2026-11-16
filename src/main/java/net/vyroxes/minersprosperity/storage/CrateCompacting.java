package net.vyroxes.minersprosperity.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CrateCompacting
{
    private CrateCompacting()
    {
    }

    public static boolean same(@NotNull ItemStack first, @NotNull ItemStack second)
    {
        return !first.isEmpty()
                && !second.isEmpty()
                && ItemHandlerHelper.canItemStacksStack(first, second);
    }

    private static boolean safe(@NotNull ItemStack item)
    {
        return !item.isEmpty()
                && !item.hasTagCompound()
                && !item.isItemStackDamageable()
                && !item.getItem().hasContainerItem(item)
                && !item.serializeNBT().hasKey("ForgeCaps");
    }

    private static @NotNull ItemStack craft(
            @NotNull ItemStack input, int size, @Nullable World world)
    {
        InventoryCrafting grid =
                new InventoryCrafting(
                        new Container()
                        {
                            @Override
                            public boolean canInteractWith(@NotNull EntityPlayer player)
                            {
                                return false;
                            }
                        },
                        size,
                        size);
        for (int i = 0; i < size * size; i++)
        {
            ItemStack copy = input.copy();
            copy.setCount(1);
            grid.setInventorySlotContents(i, copy);
        }
        IRecipe recipe = CraftingManager.findMatchingRecipe(grid, world);
        if (recipe == null || recipe.isDynamic()) return ItemStack.EMPTY;
        for (ItemStack remainder : recipe.getRemainingItems(grid))
            if (!remainder.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = recipe.getCraftingResult(grid);
        return safe(result) ? result.copy() : ItemStack.EMPTY;
    }

    private static @NotNull ItemStack lower(@NotNull ItemStack input, @Nullable World world)
    {
        ItemStack result = craft(input, 1, world);
        if (result.isEmpty()
                || (result.getCount() != 4 && result.getCount() != 9)
                || same(result, input)) return ItemStack.EMPTY;
        ItemStack reverse = craft(result, result.getCount() == 4 ? 2 : 3, world);
        return same(reverse, input) && reverse.getCount() == 1 ? result : ItemStack.EMPTY;
    }

    public static @NotNull List<ItemStack> family(@NotNull ItemStack seed, @Nullable World world)
    {
        List<ItemStack> ascending = new ArrayList<>();
        ItemStack lowest = seed.copy();
        lowest.setCount(1);
        if (!safe(lowest))
        {
            ascending.add(lowest);
            return ascending;
        }
        for (int i = 0; i < 3; i++)
        {
            ItemStack next = lower(lowest, world);
            if (next.isEmpty()) break;
            lowest = next.copy();
            lowest.setCount(1);
        }
        ascending.add(lowest);
        while (ascending.size() < 4)
        {
            ItemStack top = ascending.get(ascending.size() - 1);
            ItemStack next = ItemStack.EMPTY;
            for (int size : new int[] {3, 2})
            {
                ItemStack candidate = craft(top, size, world);
                if (candidate.isEmpty() || candidate.getCount() != 1 || same(candidate, top))
                    continue;
                ItemStack restored = lower(candidate, world);
                if (same(restored, top) && restored.getCount() == size * size)
                {
                    next = candidate;
                    break;
                }
            }
            if (next.isEmpty()) break;
            boolean cycle = false;
            for (ItemStack entry : ascending)
            {
                if (same(entry, next)) cycle = true;
            }
            if (cycle) break;
            ascending.add(next);
        }
        Collections.reverse(ascending);
        return ascending;
    }

    public static int ratio(
            @NotNull ItemStack upper, @NotNull ItemStack lower, @Nullable World world)
    {
        ItemStack result = lower(upper, world);
        return same(result, lower) ? result.getCount() : 1;
    }
}