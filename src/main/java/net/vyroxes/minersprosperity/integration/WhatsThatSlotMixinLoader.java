package net.vyroxes.minersprosperity.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;

public class WhatsThatSlotMixinLoader implements ILateMixinLoader
{
    @Override
    public List<String> getMixinConfigs()
    {
        if (!FMLLaunchHandler.side().isClient() || !Loader.isModLoaded("wts")) return Collections.emptyList();
        return Loader.isModLoaded("itemzoom")
                ? java.util.Arrays.asList("mixins.minersprosperity.wts.json", "mixins.minersprosperity.wts_itemzoom.json")
                : Collections.singletonList("mixins.minersprosperity.wts.json");
    }
}