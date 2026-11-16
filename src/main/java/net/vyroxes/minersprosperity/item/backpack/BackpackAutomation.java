package net.vyroxes.minersprosperity.item.backpack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.enchantment.event.AutoCollectHandler;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class BackpackAutomation
{
    private BackpackAutomation()
    {
    }

    public static boolean same(ItemStack a, ItemStack b)
    {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static int count(IItemHandler inventory, ItemStack item)
    {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++)
            if (same(inventory.getStackInSlot(i), item)) count += inventory.getStackInSlot(i).getCount();
        return count;
    }

    public static boolean acceptsFilter(ItemStack backpack, ItemStack item)
    {
        NBTTagCompound data = BackpackSettings.data(backpack);
        IItemHandler filter = ItemBackpackBase.getConfigurationInventory(backpack);
        boolean matched = BackpackSettings.matchesTextFilter(backpack, item);
        for (int i = 0; !matched && i < filter.getSlots(); i++)
        {
            if (AutoCollectHandler.matchesFilter(filter.getStackInSlot(i), item, data.getBoolean("Metadata"),
                    data.getBoolean("NBT"), data.getBoolean("Damage"), data.getBoolean("OreDict"), data.getBoolean("ModID")))
            {
                matched = true;
                break;
            }
        }
        return data.getBoolean("WhitelistOrBlacklist") == matched;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || event.player.ticksExisted % 20 != 0) return;
        for (ItemStack backpack : ItemBackpackBase.getAllBackpacksFromPlayer(event.player)) process(backpack, event.player.world);
    }

    public static void process(ItemStack backpack, World world)
    {
        NBTTagCompound data = BackpackSettings.data(backpack);
        if (!data.getBoolean("AutomationDirty")) return;
        data.setBoolean("AutomationDirty", false);
        ItemStackHandler inventory = ItemBackpackBase.getBackpackInventory(backpack);
        List<ItemStack> types = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || BackpackSettings.locked(backpack, i)) continue;
            boolean seen = false;
            for (ItemStack type : types) if (same(type, stack)) { seen = true; break; }
            if (!seen) types.add(stack.copy());
        }
        int compression = BackpackSettings.get(backpack, "Compression");
        for (ItemStack type : types)
        {
            if (compression == 2 || compression == 3)
                if (compress(backpack, inventory, type, world, 3)) continue;
            if (compression == 1 || compression == 3) compress(backpack, inventory, type, world, 2);
        }
        int voidMode = BackpackSettings.get(backpack, "Void");
        if (voidMode == 0) return;

        List<ItemStack> voidTypes = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            ItemStack item = inventory.getStackInSlot(i);
            if (item.isEmpty() || BackpackSettings.locked(backpack, i) || !acceptsFilter(backpack, item)) continue;
            boolean full = true;
            ItemStack probe = item.copy();
            probe.setCount(1);
            for (int j = 0; j < inventory.getSlots(); j++)
                if (!BackpackSettings.locked(backpack, j) && inventory.insertItem(j, probe, true).isEmpty()) { full = false; break; }
            if (voidMode == 2 || full) voidTypes.add(item.copy());
        }
        for (ItemStack item : voidTypes)
            remove(backpack, inventory, item, Math.max(0, count(inventory, item) - BackpackSettings.keep(backpack, "VoidKeep", item)));
    }

    private static void remove(ItemStack backpack, ItemStackHandler inventory, ItemStack item, int amount)
    {
        for (int i = inventory.getSlots() - 1; i >= 0 && amount > 0; i--)
        {
            if (BackpackSettings.locked(backpack, i) || !same(inventory.getStackInSlot(i), item)) continue;
            amount -= inventory.extractItem(i, amount, false).getCount();
        }
    }

    private static InventoryCrafting grid(int size, ItemStack item)
    {
        InventoryCrafting grid = new InventoryCrafting(new Container()
        {
            @Override
            public boolean canInteractWith(@NotNull EntityPlayer player)
            {
                return false;
            }
        }, size, size);
        for (int i = 0; i < size * size; i++)
        {
            ItemStack unit = item.copy();
            unit.setCount(1);
            grid.setInventorySlotContents(i, unit);
        }
        return grid;
    }

    private static boolean safeIngredient(ItemStack item)
    {
        return !item.hasTagCompound() && !item.isItemStackDamageable() && !item.getItem().hasContainerItem(item)
                && !item.serializeNBT().hasKey("ForgeCaps");
    }

    private static boolean compress(ItemStack backpack, ItemStackHandler inventory, ItemStack type, World world, int size)
    {
        if (!safeIngredient(type)) return false;
        int units = size * size;
        int available = 0;
        for (int i = 0; i < inventory.getSlots(); i++)
            if (!BackpackSettings.locked(backpack, i) && same(inventory.getStackInSlot(i), type)) available += inventory.getStackInSlot(i).getCount();
        available = Math.min(available, count(inventory, type) - BackpackSettings.keep(backpack, "CompressionKeep", type));
        if (available < units) return false;
        InventoryCrafting grid = grid(size, type);
        IRecipe recipe = CraftingManager.findMatchingRecipe(grid, world);
        if (recipe == null || recipe.isDynamic()) return false;
        ItemStack output = recipe.getCraftingResult(grid);
        if (output.isEmpty() || output.getCount() != 1 || same(output, type) || !safeIngredient(output)) return false;
        for (ItemStack remainder : recipe.getRemainingItems(grid)) if (!remainder.isEmpty()) return false;
        if (BackpackSettings.get(backpack, "Reversible") != 0)
        {
            InventoryCrafting reverse = grid(1, output);
            IRecipe reverseRecipe = CraftingManager.findMatchingRecipe(reverse, world);
            if (reverseRecipe == null || reverseRecipe.isDynamic()) return false;
            ItemStack restored = reverseRecipe.getCraftingResult(reverse);
            if (!same(restored, type) || restored.getCount() != units) return false;
            for (ItemStack remainder : reverseRecipe.getRemainingItems(reverse)) if (!remainder.isEmpty()) return false;
        }

        ItemStackHandler planned = new ItemStackHandler(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) planned.setStackInSlot(i, inventory.getStackInSlot(i).copy());
        int crafts = Math.min(available / units, output.getMaxStackSize());
        remove(backpack, planned, type, crafts * units);
        ItemStack remaining = output.copy();
        remaining.setCount(crafts);
        for (int i = 0; i < planned.getSlots() && !remaining.isEmpty(); i++)
            if (!BackpackSettings.locked(backpack, i) && inventory.isItemValid(i, output)) remaining = planned.insertItem(i, remaining, false);
        if (!remaining.isEmpty()) return false;
        for (int i = 0; i < planned.getSlots(); i++)
            if (!ItemStack.areItemStacksEqual(inventory.getStackInSlot(i), planned.getStackInSlot(i))) inventory.setStackInSlot(i, planned.getStackInSlot(i));
        return true;
    }

    @SubscribeEvent
    public static void deposit(PlayerInteractEvent.LeftClickBlock event)
    {
        ItemStack backpack = event.getEntityPlayer().getHeldItemMainhand();
        if (!(backpack.getItem() instanceof ItemBackpackBase)) return;
        if (BackpackSettings.get(backpack, "Deposit") == 0) return;
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (tile == null) return;
        IItemHandler target = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, event.getFace());
        if (target == null) return;
        event.setCanceled(true);
        if (event.getWorld().isRemote) return;
        depositInto(backpack, target);
        tile.markDirty();
        event.getEntityPlayer().inventory.markDirty();
    }
    static void depositInto(ItemStack backpack, IItemHandler target)
    {
        if (BackpackSettings.get(backpack, "Deposit") == 0) return;
        if (target == backpack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) return;
        ItemStackHandler source = ItemBackpackBase.getBackpackInventory(backpack);
        List<ItemStack> existing = new ArrayList<>();
        for (int i = 0; i < target.getSlots(); i++) if (!target.getStackInSlot(i).isEmpty()) existing.add(target.getStackInSlot(i).copy());
        for (int i = 0; i < source.getSlots(); i++)
        {
            ItemStack item = source.getStackInSlot(i);
            if (item.isEmpty() || BackpackSettings.locked(backpack, i)) continue;
            if (BackpackSettings.get(backpack, "Deposit") == 1)
            {
                boolean found = false;
                for (ItemStack present : existing) if (same(item, present)) { found = true; break; }
                if (!found) continue;
            }
            int amount = Math.min(item.getCount(), count(source, item) - BackpackSettings.keep(backpack, "DepositKeep", item));
            if (amount <= 0) continue;
            ItemStack offered = item.copy();
            offered.setCount(amount);
            for (int j = 0; j < target.getSlots() && !offered.isEmpty(); j++) offered = target.insertItem(j, offered, false);
            source.extractItem(i, amount - offered.getCount(), false);
        }
    }
}