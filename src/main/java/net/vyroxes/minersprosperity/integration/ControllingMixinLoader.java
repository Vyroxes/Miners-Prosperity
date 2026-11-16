package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class ControllingMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient() || !Loader.isModLoaded("controlling")) return Collections.emptyList();
        return Collections.singletonList("mixins.minersprosperity.controlling.json");
    }
}