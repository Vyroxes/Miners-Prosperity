package net.vyroxes.minersprosperity.init;

import net.minecraft.block.material.Material;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.fluid.FluidBase;

public final class ModFluids
{
    public static Fluid LIQUID_EXPERIENCE;
    public static Fluid CRUDE_OIL;
    public static Fluid DISTILLED_WATER;
    public static Fluid FLOTATION_SOLUTION;
    public static Fluid SULFURIC_ACID;

    private static boolean initialized;

    private ModFluids()
    {
    }

    public static void register()
    {
        if (initialized) return;

        LIQUID_EXPERIENCE = registerFluid(createLiquidExperience());
        CRUDE_OIL = registerFluid(createCrudeOil());

        DISTILLED_WATER = registerFluid(createProcessFluid("distilled_water", 0x2DB0F4));
        FLOTATION_SOLUTION = registerFluid(createProcessFluid("flotation_solution", 0x28B4AD));
        SULFURIC_ACID = registerFluid(createProcessFluid("sulfuric_acid", 0xC4CD39));

        initialized = true;
    }

    private static Fluid createLiquidExperience()
    {
        return new FluidBase("liquid_experience", new ResourceLocation(Tags.MODID, "blocks/liquid_experience_still"), new ResourceLocation(Tags.MODID, "blocks/liquid_experience_flow"), new ResourceLocation(Tags.MODID, "blocks/liquid_experience_overlay")).setMaterial(Material.WATER).setColor(0xFF00FF22).setAlpha(1.0F).setLuminosity(10);
    }

    private static Fluid createCrudeOil()
    {
        return new FluidBase("crude_oil", new ResourceLocation(Tags.MODID, "blocks/crude_oil_still"), new ResourceLocation(Tags.MODID, "blocks/crude_oil_flow"), new ResourceLocation(Tags.MODID, "blocks/crude_oil_overlay")).setMaterial(Material.WATER).setColor(0xFFFFFFFF).setParticleColor(0x1A1A1A).setAlpha(1.0F).setLuminosity(0).setTemperature(300).setDensity(1500).setViscosity(3000);
    }

    private static Fluid createProcessFluid(String name, int particleColor)
    {
        return new FluidBase(name, new ResourceLocation(Tags.MODID, "blocks/" + name + "_still"),
                new ResourceLocation(Tags.MODID, "blocks/" + name + "_flow"),
                new ResourceLocation(Tags.MODID, "blocks/" + name + "_overlay"))
                .setMaterial(Material.WATER).setColor(0xFFFFFFFF).setParticleColor(particleColor).setAlpha(1.0F);
    }

    private static Fluid registerFluid(Fluid candidate)
    {
        FluidRegistry.registerFluid(candidate);
        FluidRegistry.addBucketForFluid(candidate);

        return candidate;
    }

    public static boolean isInitialized()
    {
        return initialized;
    }
}