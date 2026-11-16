package net.vyroxes.minersprosperity.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.vyroxes.minersprosperity.block.machine.ItemBlockEnergyStorage;
import net.vyroxes.minersprosperity.block.machine.BlockMachineEnergyStorage;
import net.vyroxes.minersprosperity.init.ModItems;
import net.minecraft.client.particle.ParticleBubble;
import net.minecraft.client.particle.ParticleSplash;
import net.minecraft.client.particle.ParticleWaterWake;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.client.event.KeyInputHandler;
import net.vyroxes.minersprosperity.client.registry.ClientModelRegistry;
import net.vyroxes.minersprosperity.client.particle.FluidTintParticleFactory;
import net.vyroxes.minersprosperity.client.render.MachineProgressTESR;
import net.vyroxes.minersprosperity.client.render.FluidTankTESR;
import net.vyroxes.minersprosperity.client.render.EnergyStorageTESR;
import net.vyroxes.minersprosperity.client.render.RenderCustomChest;
import net.vyroxes.minersprosperity.client.sound.MachineSoundManager;
import net.vyroxes.minersprosperity.tile.*;

public class ClientProxy extends CommonProxy
{
    @Override
    public void showItemSearchResults(int dimension, net.minecraft.util.math.BlockPos[] positions)
    {
        net.vyroxes.minersprosperity.client.event.ItemSearchClient.showResults(dimension, positions);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        KeyInputHandler.registerKeyBindings();
        ClientModelRegistry.registerEntityRenderers();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        registerRenderers();
        ClientRegistry.bindTileEntitySpecialRenderer(net.vyroxes.minersprosperity.tile.TileEntityWorktable.class, new net.vyroxes.minersprosperity.client.render.WorktableTESR());
        net.vyroxes.minersprosperity.client.render.BackpackRender.register();
        for (Item item : ModItems.ITEMS)
        {
            if (!(item instanceof ItemBlockEnergyStorage storage) || Item.REGISTRY.getNameForObject(item) == null) continue;
            int color = ((BlockMachineEnergyStorage) storage.getBlock()).getTier().coreColor;
            Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tint) -> tint == 0 ? color : 0xFFFFFF, item);
        }

        registerFluidParticles();
        for (net.vyroxes.minersprosperity.block.BlockConduit block : net.vyroxes.minersprosperity.init.ModBlocks.CONDUITS)
        {
            Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((state, world, pos, tint) -> tint == 0 ? block.type.color : block.tier.color, block);
            Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tint) -> tint == 0 ? block.type.color : block.tier.color, block);
        }
        ClientRegistry.bindTileEntitySpecialRenderer(net.vyroxes.minersprosperity.tile.TileEntityStorageCrate.class, new net.vyroxes.minersprosperity.client.render.StorageCrateTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConduit.class, new net.vyroxes.minersprosperity.client.render.ConduitTESR());
    }

    @Override
    public void updateMachineSound(TileEntityMachine tileEntity) {
        MachineSoundManager.update(tileEntity);
    }

    private void registerRenderers()
    {
        ClientRegistry.bindTileEntitySpecialRenderer(net.vyroxes.minersprosperity.tile.TileEntityCrystallizer.class, new net.vyroxes.minersprosperity.client.render.CrystallizerTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(net.vyroxes.minersprosperity.tile.TileEntityGrinder.class, new net.vyroxes.minersprosperity.client.render.GrinderTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRockCrusher.class, new MachineProgressTESR<TileEntityRockCrusher>(new ResourceLocation(Tags.MODID, "textures/blocks/rock_crusher_front_progress_bar.png"), 41, 4, 4, 10, 10));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAlloyFurnace.class, new MachineProgressTESR<TileEntityAlloyFurnace>(new ResourceLocation(Tags.MODID, "textures/blocks/alloy_furnace_front_progress_bar.png"), 41, 4, 4, 10, 1));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPulverizer.class, new MachineProgressTESR<TileEntityPulverizer>(new ResourceLocation(Tags.MODID, "textures/blocks/pulverizer_front_progress_bar.png"), 41, 4, 4, 10, 10));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityElectricFurnace.class, new MachineProgressTESR<TileEntityElectricFurnace>(new ResourceLocation(Tags.MODID, "textures/blocks/electric_furnace_front_progress_bar.png"), 41, 4, 2, 10, 3));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMetalPress.class, new MachineProgressTESR<TileEntityMetalPress>(new ResourceLocation(Tags.MODID, "textures/blocks/metal_press_front_progress_bar.png"), 41, 4, 4, 10, 1));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySolidFuelGenerator.class, new MachineProgressTESR<TileEntitySolidFuelGenerator>(new ResourceLocation(Tags.MODID, "textures/blocks/solid_fuel_generator_front_progress_bar.png"), 41, 4, 2, 10, 3));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLiquidFuelGenerator.class, new MachineProgressTESR<TileEntityLiquidFuelGenerator>(new ResourceLocation(Tags.MODID, "textures/blocks/liquid_fuel_generator_front_progress_bar.png"), 41, 4, 4, 10, 1));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFluidTank.class, new FluidTankTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityEnergyStorage.class, new EnergyStorageTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCustomChest.class, new RenderCustomChest());
    }

    private void registerFluidParticles()
    {
        Minecraft minecraft = Minecraft.getMinecraft();

        minecraft.effectRenderer.registerParticle(EnumParticleTypes.WATER_SPLASH.getParticleID(), new FluidTintParticleFactory(FluidTintParticleFactory.Type.SPLASH, new ParticleSplash.Factory()));
        minecraft.effectRenderer.registerParticle(EnumParticleTypes.WATER_BUBBLE.getParticleID(), new FluidTintParticleFactory(FluidTintParticleFactory.Type.BUBBLE, new ParticleBubble.Factory()));
        minecraft.effectRenderer.registerParticle(EnumParticleTypes.WATER_WAKE.getParticleID(), new FluidTintParticleFactory(FluidTintParticleFactory.Type.WAKE, new ParticleWaterWake.Factory()));
    }
}