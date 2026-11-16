package net.vyroxes.minersprosperity.client.event;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;

import java.util.Arrays;
import java.util.Collections;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public class ControllingHoverHandler
{
    private static String category;
    private static String description;
    private static int left, top, right, bottom;
    private static boolean hasBounds;

    private ControllingHoverHandler()
    {
    }

    public static void show(String categoryText, String descriptionText)
    {
        category = categoryText;
        description = descriptionText;
    }

    public static void setBounds(int listLeft, int listTop, int listRight, int listBottom)
    {
        left = listLeft;
        top = listTop;
        right = listRight;
        bottom = listBottom;
        hasBounds = true;
    }

    @SubscribeEvent
    public static void beforeDraw(GuiScreenEvent.DrawScreenEvent.Pre event)
    {
        category = null;
        description = null;
        hasBounds = false;
    }

    @SubscribeEvent
    public static void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (category == null || !hasBounds || event.getMouseX() < left || event.getMouseX() >= right
                || event.getMouseY() <= top || event.getMouseY() >= bottom) return;

        event.getGui().drawHoveringText(description == null ? Collections.singletonList(category) : Arrays.asList(category, TextFormatting.GRAY + description),
                event.getMouseX(), event.getMouseY());
        category = null;
        description = null;
    }
}