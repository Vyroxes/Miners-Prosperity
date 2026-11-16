package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;

public class FTBUtilitiesMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient() || !Loader.isModLoaded("ftbutilities") || !Loader.isModLoaded("ftblib"))
            return Collections.emptyList();
        return Collections.singletonList("mixins.minersprosperity.ftbutilities.json");
    }
}