package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.integration.jei.JEICompat;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageItemSearch;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class ItemSearchClient
{
    private static final KeyBinding SEARCH = new KeyBinding("key.find_item", KeyConflictContext.GUI,
            Keyboard.KEY_Y, "key.categories.miners_prosperity");
    private static BlockPos[] results = new BlockPos[0];
    private static int dimension;
    private static long expires;
    private static Object resultWorld;
    private static long lastRequest;

    private ItemSearchClient()
    {
    }

    public static void registerKeyBinding()
    {
        ClientRegistry.registerKeyBinding(SEARCH);
    }

    private static boolean search(int key)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !SEARCH.isActiveAndMatches(key)) return false;
        ItemStack stack = WhatsThatSlotOverlay.getHoveredItem();
        if (stack.isEmpty() && Loader.isModLoaded("jei")) stack = JEICompat.getHoveredItem();
        if (stack.isEmpty() && mc.currentScreen instanceof GuiContainer gui)
        {
            Slot slot = gui.getSlotUnderMouse();
            if (slot != null) stack = slot.getStack();
        }
        if (stack.isEmpty()) return false;
        long now = System.nanoTime();
        if (now - lastRequest >= 1000000000L)
        {
            lastRequest = now;
            results = new BlockPos[0];
            NetworkHandler.NETWORK.sendToServer(new MessageItemSearch(stack));
            mc.player.closeScreen();
        }
        return true;
    }

    @SubscribeEvent
    public static void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent() && search(Keyboard.getEventKey()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void mouse(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (Mouse.getEventButton() >= 0 && Mouse.getEventButtonState() && search(Mouse.getEventButton() - 100))
            event.setCanceled(true);
    }

    public static void showResults(int resultDimension, BlockPos[] positions)
    {
        Minecraft mc = Minecraft.getMinecraft();
        results = positions;
        dimension = resultDimension;
        resultWorld = mc.world;
        expires = System.nanoTime() + ModConfig.OTHER.itemSearchHighlightSeconds * 1000000000L;
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.world != resultWorld || mc.player == null
                || mc.player.dimension != dimension || System.nanoTime() >= expires || results.length == 0) return;
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) return;
        float partial = event.getPartialTicks();
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-x, -y, -z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(3);
        for (BlockPos pos : results)
        {
            if (mc.world.isBlockLoaded(pos) && !mc.world.isAirBlock(pos))
                RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(pos).grow(.003), .1F, .85F, 1F, 1F);
        }
        GlStateManager.glLineWidth(1);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}