package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.annotation.NonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class GuiDefaultButton extends GuiButton
{
    private final ResourceLocation texture;
    private final int textureX;
    private final int textureY;
    private final int offsetX;
    private final int offsetY;
    private final String tooltip;
    private List<String> currentTooltip = null;

    public GuiDefaultButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture, int textureX, int textureY, int offsetX, int offsetY, String tooltip)
    {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.textureX = textureX;
        this.textureY = textureY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.tooltip = tooltip;
    }

    public List<String> getCurrentTooltip()
    {
        return currentTooltip;
    }

    public boolean isHovered(int mouseX, int mouseY)
    {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    @NonnullByDefault
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            boolean isHovered = this.isHovered(mouseX, mouseY);

            int xOffset = isHovered ? this.offsetX : 0;
            int yOffset = isHovered ? this.offsetY : 0;

            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.texture);
            this.drawTexturedModalRect(this.x, this.y, this.textureX + xOffset, this.textureY + yOffset, this.width, this.height);

            if (isHovered)
            {
                currentTooltip = new ArrayList<>();
                for (String line : this.tooltip.replace("\\n", "\n").split("\n"))
                    currentTooltip.addAll(mc.fontRenderer.listFormattedStringToWidth(line, 230));
            }
            else currentTooltip = null;
        }
    }
}