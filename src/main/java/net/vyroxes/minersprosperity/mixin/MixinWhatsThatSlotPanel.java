package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.gegy1000.wts.gui.VanillaSlotGUI", remap = false)
public abstract class MixinWhatsThatSlotPanel
{
    @Unique private boolean minersprosperity$translated;
    @Unique private int minersprosperity$offset;

    @Inject(method = "onGUIRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void positionPanel(GuiScreenEvent.DrawScreenEvent.Post event, CallbackInfo ci)
    {
        minersprosperity$translated = false;
        WhatsThatSlotOverlay.beginPanelDraw();
        if (!ModConfig.INTEGRATION.whatsThatSlotPatch) return;
        if (WhatsThatSlotOverlay.isZoomDrawn())
        {
            ci.cancel();
            return;
        }
        if (!(event.getGui() instanceof GuiContainer)) return;
        minersprosperity$offset = Math.max(0, (((GuiContainer) event.getGui()).getGuiLeft() - 117) / 2);
        GlStateManager.pushMatrix();
        GlStateManager.translate(minersprosperity$offset, 0, 0);
        minersprosperity$translated = true;
    }

    @Redirect(method = "onGUIRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Post;getMouseX()I"), remap = false)
    private int panelMouseX(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        return event.getMouseX() - (minersprosperity$translated ? minersprosperity$offset : 0);
    }

    @Inject(method = "onGUIRender", at = @At("RETURN"), remap = false)
    private void finishPanel(GuiScreenEvent.DrawScreenEvent.Post event, CallbackInfo ci)
    {
        if (minersprosperity$translated)
        {
            GlStateManager.popMatrix();
            minersprosperity$translated = false;
            Minecraft mc = Minecraft.getMinecraft();
            if (!WhatsThatSlotOverlay.isVisible() || mc.player == null) return;
            ItemStack stack = mc.player.inventory.getItemStack();
            int x = event.getMouseX() - 8;
            int y = event.getMouseY() - 8;
            if (stack.isEmpty() || x + 16 <= minersprosperity$offset
                    || x >= minersprosperity$offset + 117) return;

            // WTS draws after GuiContainer's cursor stack, so restore it above the panel.
            RenderItem renderer = mc.getRenderItem();
            FontRenderer font = stack.getItem().getFontRenderer(stack);
            float previousZ = renderer.zLevel;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 32);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.color(1, 1, 1, 1);
            renderer.zLevel = 200;
            renderer.renderItemAndEffectIntoGUI(stack, x, y);
            renderer.renderItemOverlayIntoGUI(font == null ? mc.fontRenderer : font, stack, x, y, null);
            renderer.zLevel = previousZ;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }
}