package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.client.event.ControllingHoverHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Pseudo
@Mixin(targets = "us.getfluxed.controlsearch.client.gui.GuiNewKeyBindingList$KeyEntry", remap = false)
public abstract class MixinControllingKeyEntry implements net.vyroxes.minersprosperity.client.event.ControllingKeyEntry
{
    @Shadow @Final private net.minecraft.client.settings.KeyBinding keybinding;

    @Override
    public net.minecraft.client.settings.KeyBinding minersprosperity$keyBinding()
    {
        return keybinding;
    }

    @Shadow @Final private GuiButton btnReset;
    @Shadow @Final private String keyDesc;

    @Unique
    private boolean minersprosperity$layoutFixEnabled()
    {
        return Loader.isModLoaded("controlling") && ModConfig.INTEGRATION.controllingLayoutFix;
    }

    @Redirect(method = {"drawEntry", "func_192634_a"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawSplitString(Ljava/lang/String;IIII)V", remap = true))
    private void drawDescription(FontRenderer font, String text, int x, int y, int wrapWidth, int color)
    {
        if (!minersprosperity$layoutFixEnabled())
        {
            font.drawSplitString(text, x, y, wrapWidth, color);
            return;
        }
        int screenWidth = Minecraft.getMinecraft().currentScreen.width;
        int rowWidth = Math.min(400, screenWidth - 40);
        int textWidth = rowWidth - 155;
        if (font.getStringWidth(text) > textWidth)
        {
            text = font.trimStringToWidth(text, textWidth - font.getStringWidth("...")) + "...";
        }
        font.drawString(text, (screenWidth - rowWidth) / 2, y, color);
    }

    @Redirect(method = {"drawEntry", "func_192634_a"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;listFormattedStringToWidth(Ljava/lang/String;I)Ljava/util/List;", remap = true))
    private List<String> keepSingleRow(FontRenderer font, String text, int width)
    {
        return minersprosperity$layoutFixEnabled() ? Collections.singletonList(text) : font.listFormattedStringToWidth(text, width);
    }

    @Redirect(method = {"drawEntry", "func_192634_a"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiButton;drawButton(Lnet/minecraft/client/Minecraft;IIF)V", remap = true))
    private void positionButton(GuiButton button, Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
    {
        if (minersprosperity$layoutFixEnabled())
        {
            int screenWidth = minecraft.currentScreen.width;
            int rowWidth = Math.min(400, screenWidth - 40);
            int right = (screenWidth - rowWidth) / 2 + rowWidth;
            button.x = right - (button == btnReset ? 50 : 150);
        }
        button.drawButton(minecraft, mouseX, mouseY, partialTicks);
    }

    @Redirect(method = {"drawEntry", "func_192634_a"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private int showFullDescription(FontRenderer font, String category, int x, int y, int color)
    {
        if (!minersprosperity$layoutFixEnabled()) return font.drawString(category, x, y, color);

        Minecraft minecraft = Minecraft.getMinecraft();
        int screenWidth = minecraft.currentScreen.width;
        int rowWidth = Math.min(400, screenWidth - 40);
        int textWidth = rowWidth - 155;
        int left = (screenWidth - rowWidth) / 2;
        int mouseX = x - 10;
        boolean truncatedDescription = mouseX >= left && mouseX < left + textWidth && font.getStringWidth(keyDesc) > textWidth;
        ControllingHoverHandler.show(category, truncatedDescription ? keyDesc : null);
        return 0;
    }
}