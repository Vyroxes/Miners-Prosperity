package net.vyroxes.minersprosperity.init;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.tile.*;

public final class ModTileEntities
{
    private ModTileEntities()
    {
    }

    public static void register() {
        register(TileEntityWorktable.class, "worktable");
        register(TileEntityTrashCan.class, "trash_can");
        register(TileEntityStorageCrateController.class, "storage_crate_controller");
        if (ModConfig.BLOCKS.enableStorageCrates) register(TileEntityStorageCrate.class, "storage_crate");
        register(TileEntityBackpack.class, "backpack");
        register(TileEntityConduit.class, "conduit");
        if (ModConfig.MACHINES.enableAlloyFurnace) register(TileEntityAlloyFurnace.class, "alloy_furnace");
        if (ModConfig.MACHINES.enableRockCrusher) register(TileEntityRockCrusher.class, "rock_crusher");
        if (ModConfig.MACHINES.enablePulverizer) register(TileEntityPulverizer.class, "pulverizer");
        if (ModConfig.MACHINES.enableCrystallizer) register(TileEntityCrystallizer.class, "crystallizer");
        if (ModConfig.MACHINES.enableGrinder) register(TileEntityGrinder.class, "grinder");
        if (ModConfig.MACHINES.enableSeparator) register(TileEntitySeparator.class, "separator");
        if (ModConfig.MACHINES.enableMetalPress) register(TileEntityMetalPress.class, "metal_press");
        if (ModConfig.MACHINES.enableElectricFurnace) register(TileEntityElectricFurnace.class, "electric_furnace");
        if (ModConfig.MACHINES.enableSolidFuelGenerator) register(TileEntitySolidFuelGenerator.class, "solid_fuel_generator");
        if (ModConfig.MACHINES.enableLiquidFuelGenerator) register(TileEntityLiquidFuelGenerator.class, "liquid_fuel_generator");
        if (ModConfig.MACHINES.enableFluidTanks) register(TileEntityFluidTank.class, "fluid_tank");
        if (ModConfig.MACHINES.enableEnergyStorages) register(TileEntityEnergyStorage.class, "energy_storage");
        if (ModConfig.MACHINES.enableSolarPanels) register(TileEntitySolarPanel.class, "solar_panel");
        if (ModConfig.BLOCKS.enableCustomChests) register(TileEntityCustomChest.class, "chest");
    }

    private static void register(Class<? extends net.minecraft.tileentity.TileEntity> type, String name) {
        GameRegistry.registerTileEntity(type, new ResourceLocation(Tags.MODID, name));
    }
}