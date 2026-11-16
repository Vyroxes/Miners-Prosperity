package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay;
import net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.gegy1000.wts.WhatsThatSlot", remap = false)
public abstract class MixinWhatsThatSlot
{
    @Shadow @Final public static KeyBinding KEY_CHECK_SLOT;
    @Shadow private void hide() { throw new AssertionError(); }

    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void togglePanel(GuiScreenEvent.KeyboardInputEvent event, CallbackInfo ci)
    {
        if (!ModConfig.INTEGRATION.whatsThatSlotPatch) return;

        if (!(event instanceof GuiScreenEvent.KeyboardInputEvent.Pre) || !Keyboard.getEventKeyState()
                || Keyboard.isRepeatEvent() || Keyboard.getEventKey() != KEY_CHECK_SLOT.getKeyCode())
        {
            ci.cancel();
            return;
        }
        if (WhatsThatSlotOverlay.isVisible())
        {
            if (!net.minecraftforge.fml.common.Loader.isModLoaded("jei") || !BackpackFilterDragHandler.isDraggingFromWhatsThatSlot()) hide();
            ci.cancel();
        }
    }

    @Inject(method = "onKeyPress", at = @At(value = "INVOKE",
            target = "Lnet/gegy1000/wts/gui/SlotGUI;show(Lnet/gegy1000/wts/SlotPlaceInfo;)V", shift = At.Shift.AFTER), remap = false)
    private void panelShown(GuiScreenEvent.KeyboardInputEvent event, CallbackInfo ci)
    {
        WhatsThatSlotOverlay.setVisible(true);
    }

    @Inject(method = "hide", at = @At("RETURN"), remap = false)
    private void panelHidden(CallbackInfo ci)
    {
        WhatsThatSlotOverlay.setVisible(false);
    }

    @Inject(method = "onGUIChange", at = @At("HEAD"), remap = false)
    private void closeOnScreenChange(GuiOpenEvent event, CallbackInfo ci)
    {
        if (WhatsThatSlotOverlay.isVisible() && event.getGui() != Minecraft.getMinecraft().currentScreen) hide();
    }
}