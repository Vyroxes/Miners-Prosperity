package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.vyroxes.minersprosperity.Tags;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class InventoryTweaksTooltipHandler
{
    private static String[] pending;

    private InventoryTweaksTooltipHandler()
    {
    }

    public static void queue(String[] lines)
    {
        pending = lines;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeDraw(GuiScreenEvent.DrawScreenEvent.Pre event)
    {
        pending = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        String[] lines = pending;
        pending = null;
        if (lines == null || lines.length == 0) return;
        GuiUtils.drawHoveringText(Arrays.asList(lines), event.getMouseX(), event.getMouseY(),
                event.getGui().width, event.getGui().height, -1, Minecraft.getMinecraft().fontRenderer);
    }
}