package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.IngredientGrid", remap = false)
public abstract class MixinJEIIngredientGrid
{
    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void deferDeleteTooltip(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci)
    {
        // Draw once after the ghost and its line, for both the main list and bookmarks.
        if (BackpackFilterDragHandler.showDeleteTooltip()) ci.cancel();
    }
}