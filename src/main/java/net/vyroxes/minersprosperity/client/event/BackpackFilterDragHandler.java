package net.vyroxes.minersprosperity.client.event;

import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.config.Config;
import mezz.jei.JustEnoughItems;
import mezz.jei.gui.TooltipRenderer;
import mezz.jei.network.packets.PacketDeletePlayerItem;
import mezz.jei.gui.ghost.GhostIngredientDrag;
import mezz.jei.gui.ghost.GhostIngredientReturning;
import mezz.jei.util.CommandUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.vyroxes.minersprosperity.client.gui.GuiBackpackConfiguration;
import net.vyroxes.minersprosperity.integration.jei.BackpackGhostIngredientHandler;
import net.vyroxes.minersprosperity.integration.jei.JEICompat;
import org.lwjgl.input.Mouse;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class BackpackFilterDragHandler
{
    private static BackpackFilterDragHandler instance;
    private final IIngredientRegistry ingredients;
    private final BackpackGhostIngredientHandler handler = new BackpackGhostIngredientHandler();
    private final List<GhostIngredientReturning<ItemStack>> returning = new ArrayList<>();
    private GhostIngredientDrag<ItemStack> dragged;
    private GhostIngredientDrag<ItemStack> preview;
    private GuiBackpackConfiguration source;
    private GuiBackpackConfiguration previewScreen;
    private ItemStack previewIngredient = ItemStack.EMPTY;
    private boolean consumeRelease;
    private boolean draggedFromWhatsThatSlot;

    public BackpackFilterDragHandler(IIngredientRegistry ingredients)
    {
        this.ingredients = ingredients;
        instance = this;
    }

    public static boolean isDragging()
    {
        return instance != null && instance.dragged != null && !Config.isCheatItemsEnabled();
    }

    public static boolean isDraggingFromWhatsThatSlot()
    {
        return isDragging() && instance.draggedFromWhatsThatSlot
                && instance.source == Minecraft.getMinecraft().currentScreen;
    }

    public static boolean showDeleteTooltip()
    {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.currentScreen instanceof GuiContainer && mc.player != null
                && (isDragging() || Config.isCheatItemsEnabled() && !mc.player.inventory.getItemStack().isEmpty()
                && (!WhatsThatSlotOverlay.getHoveredItem().isEmpty() || !JEICompat.getHoveredBookmark().isEmpty()
                || JEICompat.IS_HEI && !JEICompat.getHoveredListItem().isEmpty()));
    }

    private static boolean isOverIngredient()
    {
        return !WhatsThatSlotOverlay.getHoveredItem().isEmpty() || !JEICompat.getHoveredBookmark().isEmpty()
                || !JEICompat.getHoveredListItem().isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (JEICompat.IS_HEI) return;
        handleMouse(event);
    }

    public static void handleHEIMouse(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (instance != null) instance.handleMouse(event);
    }

    private void handleMouse(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(event.getGui() instanceof GuiContainer container) || mc.player == null) return;
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return;
        int x = Mouse.getX() * container.width / mc.displayWidth;
        int y = container.height - Mouse.getY() * container.height / mc.displayHeight - 1;
        if (!Mouse.getEventButtonState() && consumeRelease)
        {
            consumeRelease = false;
            event.setCanceled(true);
            return;
        }
        if (Config.isCheatItemsEnabled())
        {
            clearGhosts();

            if (Mouse.getEventButtonState())
            {
                ItemStack stack = WhatsThatSlotOverlay.getHoveredItem();
                if (JEICompat.IS_HEI && stack.isEmpty()) stack = JEICompat.getHoveredBookmark();
                if (JEICompat.IS_HEI && stack.isEmpty()) stack = JEICompat.getHoveredListItem();
                ItemStack carried = mc.player.inventory.getItemStack();
                if (!carried.isEmpty() && (!stack.isEmpty() || !JEICompat.getHoveredBookmark().isEmpty()))
                {
                    mc.player.inventory.setItemStack(ItemStack.EMPTY);
                    JustEnoughItems.getProxy().sendPacketToServer(new PacketDeletePlayerItem(carried));
                    consumeRelease = true;
                    event.setCanceled(true);
                    return;
                }
                if (!stack.isEmpty())
                {
                    CommandUtil.giveStack(stack, button);
                    consumeRelease = true;
                    event.setCanceled(true);
                }
            }
            return;
        }
        if (!(container instanceof GuiBackpackConfiguration gui)) return;
        if (dragged != null)
        {
            if (Mouse.getEventButtonState() && isOverIngredient())
            {
                clearGhosts();
                consumeRelease = true;
                event.setCanceled(true);
                return;
            }
            boolean finish = Mouse.getEventButtonState();
            if (!finish && source == gui)
            {
                for (mezz.jei.api.gui.IGhostIngredientHandler.Target<ItemStack> target
                        : handler.getTargets(gui, dragged.getIngredient(), false))
                {
                    if (!target.getArea().contains(x, y)) continue;
                    finish = true;
                    break;
                }
            }
            if (finish)
            {
                if (source != gui || !mc.player.inventory.getItemStack().isEmpty() || !dragged.onClick(x, y))
                {
                    GhostIngredientReturning<ItemStack> animation = GhostIngredientReturning.create(dragged, x, y);
                    if (animation != null) returning.add(animation);
                }
                dragged = null;
                draggedFromWhatsThatSlot = false;
                preview = null;
                previewIngredient = ItemStack.EMPTY;
                consumeRelease = Mouse.getEventButtonState();
            }
            event.setCanceled(true);
            return;
        }
        if (!Mouse.getEventButtonState() || !mc.player.inventory.getItemStack().isEmpty()) return;
        ItemStack stack = WhatsThatSlotOverlay.getHoveredItem();
        Rectangle origin;
        draggedFromWhatsThatSlot = !stack.isEmpty();
        if (!stack.isEmpty()) origin = WhatsThatSlotOverlay.getHoveredArea();
        else
        {
            stack = JEICompat.getHoveredBookmark();
            origin = JEICompat.getHoveredBookmarkArea(x, y);
        }
        if (stack.isEmpty())
        {
            stack = JEICompat.getHoveredListItem();
            origin = JEICompat.getHoveredListArea(x, y);
        }
        if (stack.isEmpty()) return;
        ItemStack ghost = stack.copy();
        ghost.setCount(1);
        if (origin == null) origin = new Rectangle(x - 8, y - 8, 16, 16);
        dragged = new GhostIngredientDrag<>(handler, handler.getTargets(gui, ghost, true),
                ingredients.getIngredientRenderer(ghost), ghost, origin);
        source = gui;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void open(GuiOpenEvent event)
    {
        clearGhosts();
        consumeRelease = false;
    }

    private void clearGhosts()
    {
        draggedFromWhatsThatSlot = false;
        dragged = null;
        preview = null;
        source = null;
        previewScreen = null;
        previewIngredient = ItemStack.EMPTY;
        returning.clear();
    }

    @SubscribeEvent
    public void foreground(GuiContainerEvent.DrawForeground event)
    {
        if (!(event.getGuiContainer() instanceof GuiBackpackConfiguration gui)) return;
        if (Config.isCheatItemsEnabled())
        {
            clearGhosts();
            return;
        }
        GhostIngredientDrag<ItemStack> highlights = dragged;
        if (highlights == null)
        {
            ItemStack hovered = WhatsThatSlotOverlay.getHoveredItem();
            if (hovered.isEmpty()) hovered = JEICompat.getHoveredBookmark();
            if (hovered.isEmpty())
            {
                preview = null;
                previewIngredient = ItemStack.EMPTY;
                return;
            }
            if (preview == null || previewScreen != gui || !ItemStack.areItemStacksEqual(previewIngredient, hovered))
            {
                previewIngredient = hovered.copy();
                previewScreen = gui;
                preview = new GhostIngredientDrag<>(handler, handler.getTargets(gui, hovered, false),
                        ingredients.getIngredientRenderer(hovered), hovered, null);
            }
            highlights = preview;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(-gui.getGuiLeft(), -gui.getGuiTop(), 0);
        highlights.drawTargets(event.getMouseX(), event.getMouseY());
        GlStateManager.popMatrix();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!(event.getGui() instanceof GuiContainer)) return;
        if (Config.isCheatItemsEnabled())
        {
            clearGhosts();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (dragged != null && source == event.getGui())
            dragged.drawItem(mc, event.getMouseX(), event.getMouseY());
        for (GhostIngredientReturning<ItemStack> animation : returning) animation.drawItem(mc);
        returning.removeIf(GhostIngredientReturning::isComplete);
        if (showDeleteTooltip() && isOverIngredient())
            TooltipRenderer.drawHoveringText(mc, net.minecraft.client.resources.I18n.format("jei.tooltip.delete.item"),
                    event.getMouseX(), event.getMouseY());
    }
}