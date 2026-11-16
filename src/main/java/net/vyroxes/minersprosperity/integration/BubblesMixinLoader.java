package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BubblesMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient()) return Collections.emptyList();
        ModContainer baubles = Loader.instance().getIndexedModList().get("baubles");
        if (baubles == null) return Collections.emptyList();
        boolean bubbles = "Bubbles".equals(baubles.getName());
        if (!bubbles && !"Baubles".equals(baubles.getName())) return Collections.emptyList();
        String guiConfig = bubbles ? "mixins.minersprosperity.bubbles.json" : "mixins.minersprosperity.baubles.json";
        return Loader.isModLoaded("inventorytweaks")
                ? Arrays.asList(guiConfig, "mixins.minersprosperity.bubbles_inventorytweaks.json")
                : Collections.singletonList(guiConfig);
    }
}