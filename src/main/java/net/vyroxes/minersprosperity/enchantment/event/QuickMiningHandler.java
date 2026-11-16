package net.vyroxes.minersprosperity.enchantment.event;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.vyroxes.minersprosperity.enchantment.QuickMiningLoader;
import net.vyroxes.minersprosperity.init.ModEnchantments;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.config.ModConfig;

import java.util.*;

@Mod.EventBusSubscriber
public class QuickMiningHandler
{
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        World world = event.getWorld();
        EntityPlayer player = event.getPlayer();
        BlockPos origin = event.getPos();

        if (event.isCanceled() || world.isRemote || player == null || player.isSneaking() || !ModConfig.ENCHANTMENTS.quickMining)
            return;

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty())
            return;

        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.QUICK_MINING, held) > 0)
        {
            IBlockState targetState = world.getBlockState(origin);
            Block targetBlock = targetState.getBlock();
            if (!QuickMiningLoader.canQuickMine(targetState, held))
                return;

            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> toCheck = new LinkedList<>();
            toCheck.add(origin);

            int limit = ModConfig.ENCHANTMENTS.quickMiningBlockLimit;
            int totalXp = 0;
            List<ItemStack> drops = new LinkedList<>();

            while (!toCheck.isEmpty() && visited.size() < limit && !held.isEmpty())
            {
                BlockPos pos = toCheck.poll();
                if (!visited.add(pos))
                    continue;

                if (!world.isBlockLoaded(pos)) continue;
                IBlockState state = world.getBlockState(pos);
                Block block = state.getBlock();
                if (!QuickMiningLoader.canQuickMine(state, held))
                    continue;

                if (player.canHarvestBlock(state))
                {
                    block.onBlockHarvested(world, pos, state, player);

                    NonNullList<ItemStack> blockDrops = NonNullList.create();
                    block.getDrops(blockDrops, world, pos, state, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, held));
                    drops.addAll(blockDrops);

                    int xp = block.getExpDrop(state, world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, held));
                    int insightLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.INSIGHT, held);
                    if (insightLevel > 0)
                    {
                        float multiplier = 1.0F + (0.2F + 0.1F * insightLevel);
                        int bonusXp = Math.round(xp * (multiplier - 1));
                        xp += bonusXp;
                    }
                    totalXp += xp;

                    world.setBlockToAir(pos);
                    held.damageItem(1, player);
                }

                for (EnumFacing face : EnumFacing.values())
                {
                    BlockPos neighbor = pos.offset(face);
                    if (!visited.contains(neighbor))
                        toCheck.add(neighbor);
                }
            }

            if (!player.isCreative())
            {
                ItemStack boots = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
                NBTTagCompound tag = boots.getTagCompound();
                boolean vacuumEnabled = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VACUUM, boots) > 0 && tag != null && tag.getBoolean("Vacuum");
                List<ItemStack> backpacks = ItemBackpackBase.getAllBackpacksFromPlayer(player);
                boolean hasBackpack = !backpacks.isEmpty();

                for (ItemStack drop : drops)
                {
                    if (drop.isEmpty())
                        continue;

                    ItemStack copy = drop.copy();
                    boolean stored = false;

                    if (vacuumEnabled && hasBackpack)
                    {
                        for (ItemStack backpack : backpacks)
                        {
                            NBTTagCompound compound = backpack.getTagCompound();
                            if (compound == null)
                                continue;

                            NBTTagCompound backpackData = compound.getCompoundTag("BackpackData");
                            boolean autoCollect = backpackData.getBoolean("AutoCollect");

                            if (autoCollect && AutoCollectHandler.addToBackpack(player, backpack, copy))
                            {
                                stored = true;
                                break;
                            }
                        }
                    }

                    if (vacuumEnabled && !stored)
                    {
                        boolean added = player.inventory.addItemStackToInventory(copy);
                        if (!added)
                        {
                            Block.spawnAsEntity(world, origin, copy);
                        }
                    }
                    else if (!stored)
                    {
                        Block.spawnAsEntity(world, origin, copy);
                    }
                }

                if (totalXp > 0)
                {
                    if (vacuumEnabled)
                    {
                        player.addExperience(totalXp);
                        event.setExpToDrop(0);
                    }
                    else
                    {
                        targetBlock.dropXpOnBlockBreak(world, origin, totalXp);
                    }
                }
            }
        }
    }
}