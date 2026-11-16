package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ForgeHooksClient;
import net.vyroxes.minersprosperity.block.BlockStorageCrate;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;

import org.jetbrains.annotations.NotNull;

public class StorageCrateTESR extends TileEntitySpecialRenderer<TileEntityStorageCrate>
{
    private static boolean visible(int mode, boolean looking, boolean shift)
    {
        return mode == 1 || mode == 2 && looking || mode == 3 && shift || mode == 4 && looking && shift;
    }

    @Override
    public void render(
            @NotNull TileEntityStorageCrate tile,
            double x,
            double y,
            double z,
            float partial,
            int destroy,
            float alpha)
    {
        if (!(tile.getBlockType() instanceof BlockStorageCrate)) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean looking =
                mc.objectMouseOver != null
                        && tile.getPos().equals(mc.objectMouseOver.getBlockPos());
        boolean shift = GuiScreen.isShiftKeyDown();
        boolean counts = visible(ModConfig.BLOCKS.storageCrateItemCounts, looking, shift);
        boolean names = visible(ModConfig.BLOCKS.storageCrateItemNames, looking, shift);
        boolean bars = visible(ModConfig.BLOCKS.storageCrateFillBars, looking, shift);
        EnumFacing face =
                tile.getWorld().getBlockState(tile.getPos()).getValue(BlockStorageCrate.FACING);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .5, y + .5, z + .5);
        GlStateManager.rotate(
                face == EnumFacing.SOUTH
                        ? 0
                        : face == EnumFacing.NORTH ? 180 : face == EnumFacing.EAST ? 90 : 270,
                0,
                1,
                0);
        try
        {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            for (int i = 0; i < tile.slots(); i++)
            {
                ItemStack stack = tile.template(i);
                if (stack.isEmpty()) continue;
                int slots = tile.slots();
                boolean splitColumns = slots == 4 || slots == 3 && i > 0;
                boolean right = slots == 4 ? i % 2 == 1 : i == 2;
                boolean bottom = slots == 2 ? i == 1 : slots == 3 ? i > 0 : slots == 4 && i >= 2;
                double left = splitColumns && right ? 8.5 : 1;
                double rightEdge = splitColumns && !right ? 7.5 : 15;
                double top = bottom ? 8.5 : 1;
                double bottomEdge = slots == 1 || bottom ? 15 : 7.5;
                double screenWidth = rightEdge - left;
                double screenHeight = bottomEdge - top;
                double u = (left + rightEdge) / 2;
                double v = top + screenHeight * .45;
                double size = screenHeight * .48;

                GlStateManager.pushMatrix();
                GlStateManager.translate(u / 16 - .5, .5 - v / 16, .503);
                GlStateManager.scale(size / 16, size / 16, .0001);
                GlStateManager.color(1, 1, 1, 1);
                mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                IBakedModel model =
                        mc.getRenderItem()
                                .getItemModelWithOverrides(stack, tile.getWorld(), mc.player);
                model =
                        ForgeHooksClient.handleCameraTransforms(
                                model, ItemCameraTransforms.TransformType.GUI, false);
                mc.getRenderItem().renderItem(stack, model);
                GlStateManager.popMatrix();
                GlStateManager.disableLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GlStateManager.depthMask(false);
                if (names)
                    label(
                            mc,
                            stack.getDisplayName(),
                            u,
                            top + screenHeight * .05,
                            (screenWidth - .6) / 16,
                            (float) (screenHeight * .12 / (16 * (mc.fontRenderer.FONT_HEIGHT + 1))));
                if (counts)
                    label(
                            mc,
                            tile.creative() ? "∞" : Long.toString(tile.count(i)),
                            u,
                            top + screenHeight * .73,
                            (screenWidth - .6) / 16,
                            (float) (screenHeight * .13 / (16 * (mc.fontRenderer.FONT_HEIGHT + 1))));
                if (bars)
                {
                    int barWidth = (int) Math.round(screenWidth * .8 * 16);
                    int barHeight = Math.max(3, (int) Math.round(screenHeight * .045 * 16));
                    int barLeft = -barWidth / 2;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(u / 16 - .5, .5 - (top + screenHeight * .91) / 16, .507);
                    GlStateManager.scale(1.0 / 256, -1.0 / 256, 1);
                    GlStateManager.disableLighting();
                    Gui.drawRect(barLeft, 0, barLeft + barWidth, barHeight, 0xFF11151B);
                    GlStateManager.translate(0, 0, .0005);
                    Gui.drawRect(barLeft + 1, 1, barLeft + barWidth - 1, barHeight - 1, 0xFF405060);
                    int fill = tile.fill(i) <= 0 ? 0
                            : Math.max(1, (int) Math.round((barWidth - 2) * tile.fill(i)));
                    GlStateManager.translate(0, 0, .0005);
                    if (fill > 0)
                        Gui.drawRect(barLeft + 1, 1, barLeft + 1 + fill, barHeight - 1, 0xFF35B9FF);
                    GlStateManager.popMatrix();
                }
                GlStateManager.depthMask(true);
            }
        } finally
        {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }

    private void label(
            @NotNull Minecraft mc,
            @NotNull String value,
            double u,
            double v,
            double width,
            float maximumScale)
    {
        String text = value.equals("∞") ? value : TextFormatting.BOLD + value;
        int pixels = mc.fontRenderer.getStringWidth(text);
        float scale = (float) Math.min(maximumScale, width / Math.max(1, pixels + 1));
        GlStateManager.pushMatrix();
        GlStateManager.translate(u / 16 - .5, .5 - v / 16, .508);
        GlStateManager.scale(scale, -scale, 1);
        GlStateManager.disableLighting();
        GlStateManager.color(1, 1, 1, 1);
        mc.fontRenderer.drawStringWithShadow(text, -pixels / 2F, 0, 0xFFFFFF);
        GlStateManager.popMatrix();
    }
}