package net.vyroxes.minersprosperity.item.backpack;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Objects;
import net.vyroxes.minersprosperity.util.TextFilterList;

public final class BackpackSettings
{
    private BackpackSettings()
    {
    }

    public static void ensureIdentity(ItemStack stack)
    {
        NBTTagCompound tag = data(stack);
        if (!tag.hasUniqueId("Identity")) tag.setUniqueId("Identity", java.util.UUID.randomUUID());
    }

    public static boolean sameBackpack(ItemStack first, ItemStack second)
    {
        if (first.isEmpty() || second.isEmpty() || first.getItem() != second.getItem()) return false;
        if (first == second) return true;
        NBTTagCompound firstData = first.getSubCompound("BackpackData");
        NBTTagCompound secondData = second.getSubCompound("BackpackData");
        return firstData != null && secondData != null && firstData.hasUniqueId("Identity")
                && secondData.hasUniqueId("Identity") && Objects.equals(firstData.getUniqueId("Identity"), secondData.getUniqueId("Identity"));
    }

    public static NBTTagCompound data(ItemStack stack)
    {
        boolean fresh = stack.getSubCompound("BackpackData") == null;
        NBTTagCompound data = stack.getOrCreateSubCompound("BackpackData");
        if (fresh)
        {
            data.setBoolean("Metadata", true);
            data.setBoolean("NBT", true);
            data.setBoolean("Damage", true);
        }
        if (!data.hasKey("Reversible")) data.setBoolean("Reversible", true);
        return data;
    }

    public static int defaultValue(String key)
    {
        return switch (key) {
            case "Metadata", "NBT", "Damage", "Reversible" -> 1;
            default -> 0;
        };
    }

    public static int limit(String key)
    {
        return switch (key) {
            case "Collect", "Deposit", "DepositKeep", "CompressionKeep", "Void", "VoidKeep" -> 3;
            case "Compression" -> 4;
            case "AutoCollect", "AutoCollectMode", "WhitelistOrBlacklist", "Metadata", "NBT", "Damage", "OreDict",
                 "ModID", "IsOpen", "LockEdit", "Reversible" -> 2;
            default -> 0;
        };
    }

    public static int get(ItemStack stack, String key)
    {
        NBTTagCompound data = data(stack);
        if (key.equals("Collect")) return !data.getBoolean("AutoCollect") ? 0 : data.getBoolean("AutoCollectMode") ? 1 : 2;
        return limit(key) == 2 ? (data.getBoolean(key) ? 1 : 0) : Math.max(0, Math.min(limit(key) - 1, data.getInteger(key)));
    }

    public static void set(ItemStack stack, String key, int value)
    {
        if (value < 0 || value >= limit(key)) return;
        NBTTagCompound data = data(stack);
        if (key.equals("Collect"))
        {
            data.setBoolean("AutoCollect", value != 0);
            data.setBoolean("AutoCollectMode", value == 1);
        }
        else if (limit(key) == 2) data.setBoolean(key, value != 0);
        else data.setInteger(key, value);
        if (!key.equals("IsOpen")) data.setBoolean("AutomationDirty", true);
    }

    public static boolean locked(ItemStack stack, int slot)
    {
        for (int index : data(stack).getIntArray("LockedSlots")) if (index == slot) return true;
        return false;
    }

    public static void toggleLock(ItemStack stack, int slot)
    {
        int[] old = data(stack).getIntArray("LockedSlots");
        boolean remove = locked(stack, slot);
        int[] next = new int[old.length + (remove ? -1 : 1)];
        int i = 0;
        for (int index : old) if (index != slot) next[i++] = index;
        if (!remove) next[i] = slot;
        data(stack).setIntArray("LockedSlots", next);
        data(stack).setBoolean("AutomationDirty", true);
    }

    public static String filterText(ItemStack stack, boolean ore)
    {
        NBTTagList list = data(stack).getTagList(ore ? "OreFilters" : "ModFilters", 8);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < list.tagCount(); i++)
        {
            if (i > 0) text.append(", ");
            text.append(list.getStringTagAt(i));
        }
        return text.toString();
    }

    public static boolean setFilterText(ItemStack stack, boolean ore, String text)
    {
        String normalized = TextFilterList.normalize(text, ore ? TextFilterList.Type.ORE : TextFilterList.Type.MOD);
        if (normalized == null) return false;
        NBTTagList list = new NBTTagList();
        for (String entry : TextFilterList.entries(normalized)) list.appendTag(new NBTTagString(entry));
        NBTTagCompound data = data(stack);
        data.setTag(ore ? "OreFilters" : "ModFilters", list);
        data.setBoolean("AutomationDirty", true);
        return true;
    }

    public static boolean matchesTextFilter(ItemStack backpack, ItemStack item)
    {
        if (item.isEmpty()) return false;
        NBTTagCompound data = data(backpack);
        NBTTagList mods = data.getTagList("ModFilters", 8);
        ResourceLocation id = item.getItem().getRegistryName();
        if (id != null)
            for (int i = 0; i < mods.tagCount(); i++)
                if (mods.getStringTagAt(i).equals(id.getNamespace())) return true;
        NBTTagList ores = data.getTagList("OreFilters", 8);
        if (ores.isEmpty()) return false;
        int[] ids = OreDictionary.getOreIDs(item);
        for (int i = 0; i < ores.tagCount(); i++)
        {
            String pattern = ores.getStringTagAt(i);
            for (int oreId : ids)
                if (TextFilterList.matchesOreName(pattern, OreDictionary.getOreName(oreId))) return true;
        }
        return false;
    }

    public static int keep(ItemStack stack, String key, ItemStack item)
    {
        int mode = get(stack, key);
        return mode == 1 ? 1 : mode == 2 ? item.getMaxStackSize() : 0;
    }
}