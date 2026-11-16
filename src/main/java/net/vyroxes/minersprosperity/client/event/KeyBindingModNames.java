package net.vyroxes.minersprosperity.client.event;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public final class KeyBindingModNames
{
    private static final Map<KeyBinding, String> NAMES = new IdentityHashMap<>();

    private KeyBindingModNames()
    {
    }

    public static void register(KeyBinding key)
    {
        ModContainer mod = Loader.instance().activeModContainer();
        if (mod != null) NAMES.put(key, mod.getName());
    }

    public static String name(KeyBinding key)
    {
        String name = NAMES.get(key);
        return name != null ? name : "Minecraft / Unknown";
    }
}