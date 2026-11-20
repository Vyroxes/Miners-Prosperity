package net.vyroxes.minersprosperity.mixin;

import mezz.jei.config.Config;
import mezz.jei.input.ClickedIngredient;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler;
import net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mezz.jei.input.InputHandler", remap = false)
public abstract class MixinJEIWhatsThatSlotInput
{
    @Inject(method = {"getFocusUnderMouseForClick", "getIngredientUnderMouseForKey"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void focusWhatsThatSlot(int mouseX, int mouseY, CallbackInfoReturnable<IClickedIngredient<?>> cir)
    {
        ItemStack stack = WhatsThatSlotOverlay.getHoveredItem();
        if (!stack.isEmpty()) cir.setReturnValue(ClickedIngredient.create(stack, WhatsThatSlotOverlay.getHoveredArea()));
    }

    @Redirect(method = "handleKeyDown(CI)Z",
            at = @At(value = "INVOKE", target = "Lmezz/jei/config/Config;toggleOverlayEnabled()V"),
            remap = false, require = 0)
    private void keepOverlayDuringBackpackDrag()
    {
        if (!BackpackFilterDragHandler.isDragging() || !Config.isOverlayEnabled()) Config.toggleOverlayEnabled();
    }
}