package net.vyroxes.minersprosperity.machine.recipe;

import net.vyroxes.minersprosperity.config.ModConfig;

public final class MachineRecipes {
    private MachineRecipes()
    {
    }

    public static void load() {
        if (ModConfig.MACHINES.enableAlloyFurnace) AlloyFurnaceRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enablePulverizer) PulverizerRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enableCrystallizer) CrystallizerRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enableGrinder) GrinderRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enableRockCrusher) RockCrusherRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enableMetalPress) MetalPressRecipeLoader.loadFromJSON();
        if (ModConfig.MACHINES.enableLiquidFuelGenerator) LiquidFuelGeneratorRecipeLoader.loadFromJSON();
    }
}