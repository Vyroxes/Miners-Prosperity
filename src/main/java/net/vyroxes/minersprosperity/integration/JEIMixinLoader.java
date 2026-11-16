package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class JEIMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient() || !Loader.isModLoaded("jei")) return Collections.emptyList();
        if (JEIMixinLoader.class.getResource("/mezz/jei/bookmarks/BookmarkItem.class") != null)
            return java.util.Arrays.asList("mixins.minersprosperity.jei.json", "mixins.minersprosperity.hei.json");
        return Collections.singletonList("mixins.minersprosperity.jei.json");
    }
}