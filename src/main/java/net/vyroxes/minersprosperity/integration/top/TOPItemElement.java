package net.vyroxes.minersprosperity.integration.top;

import io.netty.buffer.ByteBuf;
import mcjty.theoneprobe.apiimpl.elements.ElementItemStack;
import mcjty.theoneprobe.apiimpl.styles.ItemStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TOPItemElement extends ElementItemStack
{
    static int elementId;
    final long count;

    public TOPItemElement(ItemStack stack, long count)
    {
        super(icon(stack), new ItemStyle());
        this.count = count;
    }

    public TOPItemElement(ByteBuf buffer)
    {
        super(buffer);
        count = buffer.readLong();
    }

    private static ItemStack icon(ItemStack stack)
    {
        ItemStack icon = stack.copy();
        icon.setCount(1);
        return icon;
    }

    @Override
    public int getID()
    {
        return elementId;
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        super.toBytes(buffer);
        buffer.writeLong(count);
    }

    static float countScale(int textWidth)
    {
        return Math.min(1F, 16F / (textWidth + 1));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render(int x, int y)
    {
        super.render(x, y);
        if (count == 1) return;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = count < 0 ? "∞" : Long.toString(count);
        int width = font.getStringWidth(text);
        float scale = countScale(width);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 17F - scale, y + 17F - scale, 300F);
        GlStateManager.scale(scale, scale, 1F);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        font.drawStringWithShadow(text, -width, -font.FONT_HEIGHT, 0xFFFFFF);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
}