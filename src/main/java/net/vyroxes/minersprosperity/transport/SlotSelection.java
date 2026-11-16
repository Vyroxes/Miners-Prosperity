package net.vyroxes.minersprosperity.transport;

import java.util.Arrays;
import java.util.TreeSet;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SlotSelection
{
    public static final SlotSelection ALL = new SlotSelection("", new int[0]);
    public final String text;
    private final int[] slots;

    private SlotSelection(String text, int[] slots)
    {
        this.text = text;
        this.slots = slots;
    }

    @Nullable
    public static SlotSelection parse(String text)
    {
        if (text.length() > 512) return null;
        String normalized = text.replaceAll("\\s", "");
        if (normalized.isEmpty()) return ALL;
        TreeSet<Integer> selected = new TreeSet<>();
        try
        {
            for (String part : normalized.split(",", -1))
            {
                if (!part.matches("[0-9]+(-[0-9]+)?")) return null;
                String[] range = part.split("-");
                int first = Integer.parseInt(range[0]);
                int last = range.length == 1 ? first : Integer.parseInt(range[1]);
                if (first < 1 || last < first || last > 1_000_000 || last - first >= 4096) return null;
                for (int i = first; i <= last; i++)
                {
                    selected.add(i - 1);
                    if (selected.size() > 4096) return null;
                }
            }
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
        int[] indices = new int[selected.size()];
        int index = 0;
        for (int slot : selected) indices[index++] = slot;
        return new SlotSelection(normalized, indices);
    }

    public IItemHandler apply(IItemHandler inventory)
    {
        if (inventory == null || slots.length == 0) return inventory;
        int count = Arrays.binarySearch(slots, inventory.getSlots());
        return new SelectedInventory(inventory, slots, count >= 0 ? count : -count - 1);
    }

    private static final class SelectedInventory implements IItemHandler
    {
        private final IItemHandler inventory;
        private final int[] slots;
        private final int count;

        private SelectedInventory(IItemHandler inventory, int[] slots, int count)
        {
            this.inventory = inventory;
            this.slots = slots;
            this.count = count;
        }

        @Override
        public int getSlots()
        {
            return count;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot)
        {
            return inventory.getStackInSlot(slots[slot]);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
        {
            return inventory.insertItem(slots[slot], stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return inventory.extractItem(slots[slot], amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return inventory.getSlotLimit(slots[slot]);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return inventory.isItemValid(slots[slot], stack);
        }
    }
}