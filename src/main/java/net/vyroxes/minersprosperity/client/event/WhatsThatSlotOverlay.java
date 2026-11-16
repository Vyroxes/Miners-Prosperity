package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.Tags;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import java.awt.Rectangle;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class WhatsThatSlotOverlay
{
    private static boolean visible;
    private static boolean zoomDrawn;
    private static ItemStack hovered = ItemStack.EMPTY;
    private static GuiScreen hoverScreen;
    private static final Rectangle hoverArea = new Rectangle();

    private WhatsThatSlotOverlay()
    {
    }

    public static void setVisible(boolean value)
    {
        visible = value;
    }
    public static boolean isVisible()
    {
        return visible && ModConfig.INTEGRATION.whatsThatSlotPatch;
    }
    public static void zoomDrawn()
    {
        zoomDrawn = true;
    }
    public static boolean isZoomDrawn()
    {
        return zoomDrawn;
    }

    public static Rectangle getHoveredArea()
    {
        return new Rectangle(hoverArea);
    }

    public static void beginPanelDraw()
    {
        hovered = ItemStack.EMPTY;
        hoverScreen = null;
    }

    public static void hovered(ItemStack stack, int x, int y)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isVisible() || zoomDrawn || !(mc.currentScreen instanceof GuiContainer)) return;
        int offset = Math.max(0, (((GuiContainer) mc.currentScreen).getGuiLeft() - 117) / 2);
        hovered = stack;
        hoverScreen = mc.currentScreen;
        hoverArea.setBounds(x + offset, y, 18, 18);
    }

    public static ItemStack getHoveredItem()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isVisible() || zoomDrawn || hoverScreen != mc.currentScreen || mc.currentScreen == null) return ItemStack.EMPTY;
        int x = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
        int y = mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
        return hoverArea.contains(x, y) ? hovered : ItemStack.EMPTY;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeDraw(GuiScreenEvent.DrawScreenEvent.Pre event)
    {
        zoomDrawn = false;
    }
}