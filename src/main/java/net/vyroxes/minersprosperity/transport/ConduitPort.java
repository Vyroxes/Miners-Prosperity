package net.vyroxes.minersprosperity.transport;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;
import net.vyroxes.minersprosperity.util.TextFilterList;
import net.minecraftforge.oredict.OreDictionary;

public final class ConduitPort
{
    public int mode = 3;
    public int channel;
    public int priority;
    public int redstone;
    public int batch = 64;
    public int interval = 20;
    public int keep;
    public int extraction;
    public int distribution;
    public int insertion;
    public int insertionCursor;
    public int limit;
    public boolean blacklist;
    public boolean metadata = true;
    public boolean nbt;
    public boolean damage;
    public String ore = "";
    public String mod = "";
    public String fluid = "";
    public static final int FILTER_COLUMNS = 10;
    public static final int FILTER_ROWS = 5;
    public final ItemStack[] filters = new ItemStack[FILTER_COLUMNS * FILTER_ROWS];
    public boolean useOreDict;
    public boolean useModID;
    public boolean useFluid = true;
    private SlotSelection sourceSlots = SlotSelection.ALL;
    private SlotSelection destinationSlots = SlotSelection.ALL;
    private String cachedOre = "", cachedMod = "", cachedFluid = "";
    private String[] oreEntries = new String[0], modEntries = new String[0], fluidEntries = new String[0];
    private final String[] filterFluids = new String[FILTER_COLUMNS * FILTER_ROWS];
    public long lastVisualTransfer = -1;
    public int scanOffset;
    public int cursor;
    public int destination;
    public long nextOperation;
    public ItemStack pendingItem = ItemStack.EMPTY;
    public FluidStack pendingFluid;
    public int pendingEnergy;
    public final java.util.Deque<ConduitTransit> transits = new java.util.ArrayDeque<>();

    public ConduitPort()
    {
        java.util.Arrays.fill(filters, ItemStack.EMPTY);
    }

    public String getSourceSlots()
    {
        return sourceSlots.text;
    }

    public String getDestinationSlots()
    {
        return destinationSlots.text;
    }

    public boolean setSourceSlots(String text)
    {
        if (sourceSlots.text.equals(text)) return true;
        SlotSelection selection = SlotSelection.parse(text);
        if (selection == null) return false;
        sourceSlots = selection;
        cursor = 0;
        return true;
    }

    public boolean setDestinationSlots(String text)
    {
        if (destinationSlots.text.equals(text)) return true;
        SlotSelection selection = SlotSelection.parse(text);
        if (selection == null) return false;
        destinationSlots = selection;
        insertionCursor = 0;
        return true;
    }

    public IItemHandler sourceInventory(IItemHandler inventory)
    {
        return sourceSlots.apply(inventory);
    }

    public IItemHandler destinationInventory(IItemHandler inventory)
    {
        return destinationSlots.apply(inventory);
    }

    public String getTextFilter(TextFilterList.Type type)
    {
        return switch (type) { case ORE -> ore; case MOD -> mod; case FLUID -> fluid; };
    }

    public boolean setTextFilter(TextFilterList.Type type, String text)
    {
        if (getTextFilter(type).equals(text)) return true;
        String normalized = TextFilterList.normalize(text, type);
        if (normalized == null) return false;
        switch (type)
        {
            case ORE -> ore = normalized;
            case MOD -> mod = normalized;
            case FLUID -> fluid = normalized;
        }
        return true;
    }

    private void prepareTextFilters()
    {
        if (!cachedOre.equals(ore))
        {
            cachedOre = ore;
            oreEntries = TextFilterList.entries(ore);
        }
        if (!cachedMod.equals(mod))
        {
            cachedMod = mod;
            modEntries = TextFilterList.entries(mod);
        }
        if (!cachedFluid.equals(fluid))
        {
            cachedFluid = fluid;
            fluidEntries = TextFilterList.entries(fluid);
        }
    }

    public void setFilter(int index, ItemStack stack)
    {
        filters[index] = stack.copy();
        if (!filters[index].isEmpty()) filters[index].setCount(1);
        FluidStack contained = FluidUtil.getFluidContained(filters[index]);
        filterFluids[index] = contained == null ? null : contained.getFluid().getName();
    }

    public boolean matches(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        prepareTextFilters();
        boolean configured = oreEntries.length > 0 || modEntries.length > 0;
        String namespace = stack.getItem().getRegistryName() == null ? "" : stack.getItem().getRegistryName().getNamespace();
        boolean found = false;
        for (String entry : modEntries) if (entry.equals(namespace)) { found = true; break; }
        int[] oreIds = oreEntries.length > 0 || useOreDict ? OreDictionary.getOreIDs(stack) : null;
        for (String pattern : oreEntries)
            for (int id : oreIds)
                if (TextFilterList.matchesOreName(pattern, OreDictionary.getOreName(id))) found = true;
        for (ItemStack filter : filters)
        {
            if (filter.isEmpty()) continue;
            configured = true;
            if (found) break;
            if (metadata && !stack.isItemStackDamageable() && filter.getMetadata() != stack.getMetadata()) continue;
            if (damage && stack.isItemStackDamageable() && filter.getItemDamage() != stack.getItemDamage()) continue;
            if (nbt && !ItemStack.areItemStackTagsEqual(filter, stack)) continue;
            if (filter.getItem() == stack.getItem()) found = true;
            if (useModID && filter.getItem().getRegistryName() != null
                    && namespace.equals(filter.getItem().getRegistryName().getNamespace())) found = true;
            if (useOreDict)
                for (int filterId : OreDictionary.getOreIDs(filter))
                    for (int id : oreIds) if (filterId == id) return !blacklist;
        }
        return !configured || blacklist != found;
    }

    public boolean matches(FluidStack stack)
    {
        if (stack == null) return false;
        prepareTextFilters();
        String name = stack.getFluid().getName();
        boolean configured = fluidEntries.length > 0;
        boolean found = false;
        for (String entry : fluidEntries) if (entry.equals(name)) { found = true; break; }
        if (useFluid)
            for (String filter : filterFluids)
            {
                if (filter == null) continue;
                configured = true;
                if (filter.equals(name)) { found = true; break; }
            }
        return !configured || blacklist != found;
    }

    public NBTTagCompound write(boolean buffers)
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("Values", new int[]{mode, channel, priority, redstone, batch, interval, keep, extraction, distribution, limit});
        tag.setInteger("Insertion", insertion);
        tag.setString("SourceSlots", sourceSlots.text);
        tag.setString("DestinationSlots", destinationSlots.text);
        tag.setBoolean("UseOreDict", useOreDict);
        tag.setBoolean("UseModID", useModID);
        tag.setBoolean("UseFluid", useFluid);
        tag.setBoolean("Blacklist", blacklist);
        tag.setBoolean("Metadata", metadata);
        tag.setBoolean("NBT", nbt);
        tag.setBoolean("Damage", damage);
        tag.setString("Ore", ore);
        tag.setString("Mod", mod);
        tag.setString("Fluid", fluid);
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : filters) list.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tag.setTag("Filters", list);
        if (buffers)
        {
            tag.setInteger("InsertionCursor", insertionCursor);
            tag.setTag("PendingItem", pendingItem.writeToNBT(new NBTTagCompound()));
            if (pendingFluid != null) tag.setTag("PendingFluid", pendingFluid.writeToNBT(new NBTTagCompound()));
            tag.setInteger("PendingEnergy", pendingEnergy);
            NBTTagList deliveries = new NBTTagList();
            for (ConduitTransit transit : transits) deliveries.appendTag(transit.write());
            tag.setTag("Transits", deliveries);
        }
        return tag;
    }

    public void read(NBTTagCompound tag, boolean buffers)
    {
        int[] v = tag.getIntArray("Values");
        if (v.length == 10)
        {
            mode = MathHelper.clamp(v[0], 0, 3);
            channel = MathHelper.clamp(v[1], 0, 9999);
            priority = MathHelper.clamp(v[2], 0, 9999);
            redstone = MathHelper.clamp(v[3], 0, 2);
            batch = MathHelper.clamp(v[4], 1, 64);
            interval = MathHelper.clamp(v[5], 1, 1200);
            keep = MathHelper.clamp(v[6], 0, 1000000000);
            extraction = MathHelper.clamp(v[7], 0, 3);
            distribution = MathHelper.clamp(v[8], 0, 5);
            limit = MathHelper.clamp(v[9], 0, 1000000000);
        }
        insertion = MathHelper.clamp(tag.getInteger("Insertion"), 0, 4);
        setSourceSlots(tag.getString("SourceSlots"));
        setDestinationSlots(tag.getString("DestinationSlots"));
        useOreDict = tag.getBoolean("UseOreDict");
        useModID = tag.getBoolean("UseModID");
        useFluid = !tag.hasKey("UseFluid") || tag.getBoolean("UseFluid");
        blacklist = tag.getBoolean("Blacklist");
        metadata = !tag.hasKey("Metadata") || tag.getBoolean("Metadata");
        nbt = tag.getBoolean("NBT");
        damage = tag.getBoolean("Damage");
        setTextFilter(TextFilterList.Type.ORE, tag.getString("Ore"));
        setTextFilter(TextFilterList.Type.MOD, tag.getString("Mod"));
        setTextFilter(TextFilterList.Type.FLUID, tag.getString("Fluid"));
        NBTTagList list = tag.getTagList("Filters", 10);
        for (int i = 0; i < filters.length; i++)
        {
            setFilter(i, new ItemStack(list.getCompoundTagAt(i)));
        }
        if (buffers)
        {
            insertionCursor = Math.max(0, tag.getInteger("InsertionCursor"));
            pendingItem = new ItemStack(tag.getCompoundTag("PendingItem"));
            pendingFluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("PendingFluid"));
            pendingEnergy = Math.max(0, tag.getInteger("PendingEnergy"));
            transits.clear();
            NBTTagList deliveries = tag.getTagList("Transits", 10);
            for (int i = 0; i < deliveries.tagCount(); i++) transits.addLast(ConduitTransit.read(deliveries.getCompoundTagAt(i)));
        }
    }
}