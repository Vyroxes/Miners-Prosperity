package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.itemzoom.client.EventHandler", remap = false)
public abstract class MixinWhatsThatSlotItemZoom
{
    @Inject(method = "renderZoomedStack", at = @At("RETURN"), remap = false)
    private static void markZoom(ItemStack stack, GuiContainer gui, Minecraft minecraft, CallbackInfo ci)
    {
        WhatsThatSlotOverlay.zoomDrawn();
    }
}