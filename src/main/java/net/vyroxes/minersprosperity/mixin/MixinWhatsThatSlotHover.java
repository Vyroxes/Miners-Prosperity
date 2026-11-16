package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.gegy1000.wts.gui.SlotGUI", remap = false)
public abstract class MixinWhatsThatSlotHover
{
    @Inject(method = "renderSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private void captureIngredient(ScaledResolution resolution, int mouseX, int mouseY, ItemStack stack,
                                   int x, int y, CallbackInfo ci)
    {
        WhatsThatSlotOverlay.hovered(stack, x, y);
        Minecraft mc = Minecraft.getMinecraft();
        if (WhatsThatSlotOverlay.isVisible() && mc.player != null
                && !mc.player.inventory.getItemStack().isEmpty()
                && net.minecraftforge.fml.common.Loader.isModLoaded("jei")
                && mezz.jei.config.Config.isCheatItemsEnabled()) ci.cancel();
    }

    // This point follows the slot highlight but precedes the tooltip, allowing only the tooltip to be suppressed.
    @Inject(method = "renderSelected", at = @At(value = "FIELD",
            target = "Lnet/gegy1000/wts/gui/SlotGUI;MC:Lnet/minecraft/client/Minecraft;", ordinal = 0),
            cancellable = true, remap = false)
    private void hideTooltip(ScaledResolution resolution, int mouseX, int mouseY, ItemStack stack,
                             int x, int y, CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (WhatsThatSlotOverlay.isVisible() && mc.player != null
                && !mc.player.inventory.getItemStack().isEmpty()) ci.cancel();
        if (WhatsThatSlotOverlay.isVisible() && net.minecraftforge.fml.common.Loader.isModLoaded("jei")
                && net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler.isDragging()) ci.cancel();
    }
}