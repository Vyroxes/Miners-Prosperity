package net.vyroxes.minersprosperity.enchantment.event;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;

import java.util.Objects;

@Mod.EventBusSubscriber
public class AutoCollectHandler
{
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event)
    {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (Loader.isModLoaded("baubles"))
        {
            if (BaublesCompat.tryPickupToBackpackFromBaubles(player, event))
                return;
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++)
        {
            ItemStack itemStack = player.inventory.getStackInSlot(i);

            if (!itemStack.isEmpty() && itemStack.getItem() instanceof ItemBackpackBase)
            {
                if (tryPickupToBackpack(player, itemStack, event))
                {
                    return;
                }
            }
        }
    }

    public static boolean tryPickupToBackpack(EntityPlayer player, ItemStack backpackStack, EntityItemPickupEvent event)
    {
        NBTTagCompound compound = backpackStack.getTagCompound();
        if (compound == null)
        {
            return false;
        }
        NBTTagCompound backpackData = compound.getCompoundTag("BackpackData");
        boolean autoCollect = backpackData.getBoolean("AutoCollect");

        if (autoCollect)
        {
            EntityItem entityItem = event.getItem();
            ItemStack itemStackToPickup = entityItem.getItem();

            if (addToBackpack(player, backpackStack, itemStackToPickup))
            {
                event.setCanceled(true);
                entityItem.setDead();
                return true;
            }
        }
        return false;
    }

    public static boolean matchesFilter(ItemStack filterStack, ItemStack input, boolean useMetadata, boolean useNBT, boolean useDamage, boolean useOreDict, boolean useModID)
    {
        if (filterStack.isEmpty() || input.isEmpty())
            return false;

        boolean itemMatches = filterStack.getItem() == input.getItem();

        if (!useOreDict && !useModID && !itemMatches)
            return false;

        if (useMetadata && filterStack.getMetadata() != input.getMetadata())
            return false;

        if (useDamage && filterStack.getItemDamage() != input.getItemDamage())
            return false;

        if (useNBT && !ItemStack.areItemStackTagsEqual(filterStack, input))
            return false;

        if (useOreDict)
        {
            int[] filterIDs = OreDictionary.getOreIDs(filterStack);
            int[] inputIDs = OreDictionary.getOreIDs(input);

            boolean anyMatch = false;
            for (int id1 : filterIDs)
            {
                for (int id2 : inputIDs)
                {
                    if (id1 == id2)
                    {
                        anyMatch = true;
                        break;
                    }
                }
                if (anyMatch)
                    break;
            }
            if (!anyMatch)
                return false;
        }

        if (useModID)
        {
            String modFilter = filterStack.getItem().getRegistryName().getNamespace();
            String modInput = input.getItem().getRegistryName().getNamespace();
            return modFilter.equals(modInput);
        }

        return true;
    }

    public static boolean addToBackpack(EntityPlayer player, ItemStack backpack, ItemStack itemStackToAdd)
    {
        NBTTagCompound backpackData = net.vyroxes.minersprosperity.item.backpack.BackpackSettings.data(backpack);
        if (!backpackData.getBoolean("AutoCollect")) return false;

        boolean autoCollectMode = backpackData.getBoolean("AutoCollectMode");
        boolean useMetadata = backpackData.getBoolean("Metadata");
        boolean useNBT = backpackData.getBoolean("NBT");
        boolean useDamage = backpackData.getBoolean("Damage");
        boolean useOreDict = backpackData.getBoolean("OreDict");
        boolean useModID = backpackData.getBoolean("ModID");

        ItemStackHandler backpackInventory = ItemBackpackBase.getBackpackInventory(backpack);

        if (autoCollectMode)
        {
            boolean matchedExisting = false;

            for (int i = 0; i < backpackInventory.getSlots(); i++)
            {
                ItemStack existing = backpackInventory.getStackInSlot(i);
                if (matchesFilter(existing, itemStackToAdd, useMetadata, useNBT, useDamage, useOreDict, useModID))
                {
                    matchedExisting = true;
                    break;
                }
            }

            if (!matchedExisting)
                return false;
        }

        if (!net.vyroxes.minersprosperity.item.backpack.BackpackAutomation.acceptsFilter(backpack, itemStackToAdd)) return false;

        net.minecraftforge.items.IItemHandler automation = backpack.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        for (int i = 0; i < Objects.requireNonNull(automation).getSlots() && !itemStackToAdd.isEmpty(); i++)
        {
            ItemStack remainder = automation.insertItem(i, itemStackToAdd.copy(), false);
            itemStackToAdd.setCount(remainder.getCount());
        }
        if (itemStackToAdd.isEmpty()) return true;

        player.sendMessage(new TextComponentTranslation("message.backpack.full").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
        return itemStackToAdd.isEmpty();
    }
}