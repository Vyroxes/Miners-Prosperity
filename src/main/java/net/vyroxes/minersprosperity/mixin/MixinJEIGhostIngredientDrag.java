package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.vyroxes.minersprosperity.client.gui.GuiBackpackConfiguration;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ghost.GhostIngredientDrag", remap = false)
public abstract class MixinJEIGhostIngredientDrag
{
    @Inject(method = "drawItem", at = @At("HEAD"), remap = false)
    private void prepareBackpackGhost(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci)
    {
        if (!(minecraft.currentScreen instanceof GuiBackpackConfiguration)) return;
        // Normalize every source, including JEI's own list. Mods using raw GL calls can leave the GL state
        // different from GlStateManager's cache, so update the cache and force the actual state as well.
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
    }
}