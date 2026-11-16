package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;

public class InventoryTweaksMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient() || !Loader.isModLoaded("inventorytweaks")) return Collections.emptyList();
        return Collections.singletonList("mixins.minersprosperity.inventorytweaks.json");
    }
}