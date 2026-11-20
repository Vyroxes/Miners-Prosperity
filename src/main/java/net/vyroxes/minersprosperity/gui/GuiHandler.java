package net.vyroxes.minersprosperity.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.vyroxes.minersprosperity.client.gui.*;
import net.vyroxes.minersprosperity.inventory.container.*;
import net.minecraft.util.math.BlockPos;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.tile.*;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;
import java.util.Objects;

public class GuiHandler implements IGuiHandler
{
    public enum GuiTypes
    {
        MACHINE_CONFIGURATION,
        MACHINE_SLOT_CONFIGURATION,
        MACHINE_UPGRADES,
        ALLOY_FURNACE,
        GRINDER,
        CRYSTALLIZER,
        PULVERIZER,
        METAL_PRESS,
        ELECTRIC_FURNACE,
        SOLAR_PANEL,
        BACKPACK,
        BACKPACK_CONFIGURATION,
        CHEST,
        SOLID_FUEL_GENERATOR,
        LIQUID_FUEL_GENERATOR,
        FLUID_TANK,
        ENERGY_STORAGE,
        CONDUIT_DOWN, CONDUIT_UP, CONDUIT_NORTH, CONDUIT_SOUTH, CONDUIT_WEST, CONDUIT_EAST, PLACED_BACKPACK, ROCK_CRUSHER, STORAGE_CRATE, STORAGE_CRATE_CONTROLLER, WORKTABLE, TRASH_CAN, SEPARATOR;

        public static GuiTypes fromId(int id)
        {
            if (id < 0 || id >= values().length)
            {
                throw new IllegalArgumentException("Invalid GUI ID: " + id);
            }
            return values()[id];
        }
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (id < 0 || id >= GuiTypes.values().length) return null;
        GuiTypes type = GuiTypes.fromId(id);
        BlockPos pos = new BlockPos(x, y, z);
        if (type == GuiTypes.BACKPACK || type == GuiTypes.BACKPACK_CONFIGURATION)
        {
            ItemStack backpack = y == 1 ? getBackpackFromBaubles(player) : getBackpackFromHand(player);
            if (backpack.isEmpty() || !(backpack.getItem() instanceof ItemBackpackBase)) return null;
        }
        else
        {
            if (!world.isBlockLoaded(pos) || player.getDistanceSq(pos) > 64) return null;
            TileEntity tile = world.getTileEntity(pos);
            boolean valid = switch (type)
            {
                case MACHINE_CONFIGURATION, MACHINE_SLOT_CONFIGURATION, MACHINE_UPGRADES -> tile instanceof TileEntityMachine;
                case ALLOY_FURNACE -> tile instanceof TileEntityAlloyFurnace;
                case GRINDER -> tile instanceof TileEntityGrinder;
            case SEPARATOR -> tile instanceof TileEntitySeparator;
                case CRYSTALLIZER -> tile instanceof TileEntityCrystallizer;
                case PULVERIZER -> tile instanceof TileEntityPulverizer;
                case METAL_PRESS -> tile instanceof TileEntityMetalPress;
                case ELECTRIC_FURNACE -> tile instanceof TileEntityElectricFurnace;
                case SOLAR_PANEL -> tile instanceof TileEntitySolarPanel;
                case CHEST -> tile instanceof TileEntityCustomChest;
                case SOLID_FUEL_GENERATOR -> tile instanceof TileEntitySolidFuelGenerator;
                case LIQUID_FUEL_GENERATOR -> tile instanceof TileEntityLiquidFuelGenerator;
                case FLUID_TANK -> tile instanceof TileEntityFluidTank;
                case ENERGY_STORAGE -> tile instanceof TileEntityEnergyStorage;
                case PLACED_BACKPACK -> tile instanceof TileEntityBackpack;
                case ROCK_CRUSHER -> tile instanceof TileEntityRockCrusher;
                case STORAGE_CRATE -> tile instanceof TileEntityStorageCrate;
                case STORAGE_CRATE_CONTROLLER -> tile instanceof TileEntityStorageCrateController;
                case WORKTABLE -> tile instanceof TileEntityWorktable;
                case TRASH_CAN -> tile instanceof TileEntityTrashCan;
                case CONDUIT_DOWN, CONDUIT_UP, CONDUIT_NORTH, CONDUIT_SOUTH, CONDUIT_WEST, CONDUIT_EAST -> tile instanceof TileEntityConduit;
                default -> false;
            };
            if (!valid) return null;
        }
        return switch (type)
        {
            case TRASH_CAN -> new ContainerTrashCan(player.inventory, (TileEntityTrashCan) world.getTileEntity(pos));
            case WORKTABLE -> world.isBlockLoaded(pos) && player.getDistanceSq(pos) <= 64 && world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityWorktable table ? new ContainerWorktable(player.inventory, table) : null;
            case STORAGE_CRATE_CONTROLLER -> world.isBlockLoaded(pos) && player.getDistanceSq(pos) <= 64 && world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController controller ? new ContainerInventory(player.inventory, controller) : null;
            case STORAGE_CRATE -> world.isBlockLoaded(pos) && player.getDistanceSq(pos) <= 64 && world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrate crate ? new ContainerStorageCrate(player.inventory, crate) : null;
            case CONDUIT_DOWN, CONDUIT_UP, CONDUIT_NORTH, CONDUIT_SOUTH, CONDUIT_WEST, CONDUIT_EAST ->
                world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityConduit tile
                    ? new ContainerConduit(player.inventory, tile, id - GuiTypes.CONDUIT_DOWN.ordinal()) : null;
            case MACHINE_CONFIGURATION, MACHINE_SLOT_CONFIGURATION -> new ContainerInventory(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case MACHINE_UPGRADES -> new ContainerUpgrades(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ALLOY_FURNACE -> new ContainerAlloyFurnace(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ROCK_CRUSHER -> new ContainerRockCrusher(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case PULVERIZER -> new ContainerPulverizer(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case CRYSTALLIZER -> new ContainerCrystallizer(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case SEPARATOR -> new ContainerSeparator(player.inventory, (TileEntitySeparator) Objects.requireNonNull(world.getTileEntity(pos)));
            case GRINDER -> new ContainerGrinder(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case METAL_PRESS -> new ContainerMetalPress(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ELECTRIC_FURNACE -> new ContainerElectricFurnace(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case SOLID_FUEL_GENERATOR -> new ContainerSolidFuelGenerator(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case LIQUID_FUEL_GENERATOR -> new ContainerLiquidFuelGenerator(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case FLUID_TANK -> new ContainerFluidTank(player.inventory, (net.vyroxes.minersprosperity.tile.TileEntityFluidTank) Objects.requireNonNull(world.getTileEntity(pos)));
            case ENERGY_STORAGE -> new ContainerEnergyStorage(player.inventory, (net.vyroxes.minersprosperity.tile.TileEntityEnergyStorage) Objects.requireNonNull(world.getTileEntity(pos)));
            case SOLAR_PANEL -> new ContainerSolarPanel(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case PLACED_BACKPACK -> player.getDistanceSq(pos) <= 64 && world.isBlockLoaded(pos) && world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityBackpack tile
                    && tile.getBackpack().getItem() instanceof ItemBackpackBase ? new ContainerBackpack(player.inventory, tile) : null;
            case BACKPACK -> new ContainerBackpack(player.inventory, y == 1 ? getBackpackFromBaubles(player) : getBackpackFromHand(player), y == 1);
            case BACKPACK_CONFIGURATION -> new ContainerBackpackConfiguration(player.inventory, y == 1 ? getBackpackFromBaubles(player) : getBackpackFromHand(player), y == 1);
            case CHEST -> new ContainerCustomChest(player.inventory, (TileEntityCustomChest) Objects.requireNonNull(world.getTileEntity(pos)));
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        BlockPos pos = new BlockPos(x, y, z);
        return switch (GuiTypes.fromId(id)) {
            case TRASH_CAN -> new GuiTrashCan(player.inventory, (TileEntityTrashCan) world.getTileEntity(pos));
            case WORKTABLE -> world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityWorktable table ? new GuiWorktable(player.inventory, table) : null;
            case STORAGE_CRATE_CONTROLLER -> world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController controller ? new GuiStorageCrateController(player.inventory, controller) : null;
            case STORAGE_CRATE -> world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityStorageCrate crate ? new GuiStorageCrate(player.inventory, crate) : null;
            case CONDUIT_DOWN, CONDUIT_UP, CONDUIT_NORTH, CONDUIT_SOUTH, CONDUIT_WEST, CONDUIT_EAST ->
                world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityConduit tile
                    ? new GuiConduit(player.inventory, tile, id - GuiTypes.CONDUIT_DOWN.ordinal()) : null;
            case MACHINE_CONFIGURATION ->
                    new GuiMachineConfiguration(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case MACHINE_SLOT_CONFIGURATION ->
                    new GuiMachineSlotConfiguration(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case MACHINE_UPGRADES ->
                    new GuiMachineUpgrades(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ALLOY_FURNACE ->
                    new GuiAlloyFurnace(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ROCK_CRUSHER -> new GuiRockCrusher(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case PULVERIZER ->
                    new GuiPulverizer(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case CRYSTALLIZER ->
                    new GuiCrystallizer(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case SEPARATOR -> new GuiSeparator(player.inventory, (TileEntitySeparator) Objects.requireNonNull(world.getTileEntity(pos)));
            case GRINDER ->
                    new GuiGrinder(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case METAL_PRESS ->
                    new GuiMetalPress(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case ELECTRIC_FURNACE ->
                    new GuiElectricFurnace(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case SOLID_FUEL_GENERATOR ->
                    new GuiSolidFuelGenerator(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case LIQUID_FUEL_GENERATOR ->
                    new GuiLiquidFuelGenerator(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case FLUID_TANK ->
                    new GuiFluidTank(player.inventory, (net.vyroxes.minersprosperity.tile.TileEntityFluidTank) Objects.requireNonNull(world.getTileEntity(pos)));
            case ENERGY_STORAGE ->
                    new GuiEnergyStorage(player.inventory, (net.vyroxes.minersprosperity.tile.TileEntityEnergyStorage) Objects.requireNonNull(world.getTileEntity(pos)));
            case SOLAR_PANEL ->
                    new GuiSolarPanel(player.inventory, (TileEntityMachine) Objects.requireNonNull(world.getTileEntity(pos)));
            case PLACED_BACKPACK -> {
                if (!(world.getTileEntity(pos) instanceof net.vyroxes.minersprosperity.tile.TileEntityBackpack tile)
                        || !(tile.getBackpack().getItem() instanceof ItemBackpackBase item)) yield null;
                yield new GuiBackpack(new ContainerBackpack(player.inventory, tile), tile.getBackpack(), false, item.getTexture(), item.getTexture2());
            }
            case BACKPACK -> {
                ItemStack stack = y == 1 ? getBackpackFromBaubles(player) : getBackpackFromHand(player);
                ItemBackpackBase item = (ItemBackpackBase) stack.getItem();
                ResourceLocation texture = item.getTexture();
                ResourceLocation texture2 = item.getTexture2();
                yield new GuiBackpack(new ContainerBackpack(player.inventory, stack, y == 1), stack, y == 1, texture, texture2);
            }
            case BACKPACK_CONFIGURATION -> {
                ItemStack stack = y == 1 ? getBackpackFromBaubles(player) : getBackpackFromHand(player);
                ItemBackpackBase item = (ItemBackpackBase) stack.getItem();
                ResourceLocation texture = item.getConfigurationTexture();
                ResourceLocation texture2 = item.getConfigurationTexture2();
                yield new GuiBackpackConfiguration(new ContainerBackpackConfiguration(player.inventory, stack, y == 1), stack, y == 1, texture, texture2);
            }
            case CHEST -> new GuiCustomChest(new ContainerCustomChest(player.inventory, (TileEntityCustomChest) Objects.requireNonNull(world.getTileEntity(pos))), (TileEntityCustomChest) Objects.requireNonNull(world.getTileEntity(pos)));
        };
    }

    private static ItemStack getBackpackFromHand(EntityPlayer player)
    {
        ItemStack main = player.getHeldItemMainhand();
        if (!main.isEmpty() && main.getItem() instanceof ItemBackpackBase)
        {
            return main;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getBackpackFromBaubles(EntityPlayer player)
    {
        if (Loader.isModLoaded("baubles"))
        {
            return BaublesCompat.getBackpackStackFromBaubles(player);
        }

        return ItemStack.EMPTY;
    }
}