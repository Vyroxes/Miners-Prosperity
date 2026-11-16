package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.vyroxes.minersprosperity.client.event.InventoryTweaksTooltipHandler;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "invtweaks.InvTweaksGuiTooltipButton", remap = false)
public abstract class MixinInventoryTweaksTooltipButton
{
    @Shadow private String[] tooltipLines;

    // Keep the button drawing intact, but defer its tooltip until later buttons can no longer cover it.
    @Inject(method = {"drawButton", "func_191745_a"}, at = @At(value = "INVOKE",
            target = "Linvtweaks/InvTweaksObfuscation;getFontRenderer()Lnet/minecraft/client/gui/FontRenderer;"),
            cancellable = true, remap = false)
    private void deferTooltip(Minecraft mc, int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        if (!ModConfig.INTEGRATION.inventoryTweaksTooltipFix) return;
        InventoryTweaksTooltipHandler.queue(tooltipLines);
        ci.cancel();
    }
}