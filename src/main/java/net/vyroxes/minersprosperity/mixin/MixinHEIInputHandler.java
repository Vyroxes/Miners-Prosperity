package net.vyroxes.minersprosperity.mixin;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.input.InputHandler", remap = false)
public abstract class MixinHEIInputHandler
{
    @Inject(method = "onGuiMouseEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void handleBackpackInput(GuiScreenEvent.MouseInputEvent.Pre event, CallbackInfo ci)
    {
        if (event.isCanceled())
        {
            ci.cancel();
            return;
        }
        BackpackFilterDragHandler.handleHEIMouse(event);
        if (event.isCanceled()) ci.cancel();
    }
}