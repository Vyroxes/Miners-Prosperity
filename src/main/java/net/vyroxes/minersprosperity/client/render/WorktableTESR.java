package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.tile.TileEntityWorktable;
import org.jetbrains.annotations.NotNull;

public class WorktableTESR extends TileEntitySpecialRenderer<TileEntityWorktable>
{
    @Override
    public void render(@NotNull TileEntityWorktable tile, double x, double y, double z, float partial, int destroy, float alpha)
    {
        if (getDistanceSq(tile) > 4096) return;
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        int light = tile.getWorld().getCombinedLight(tile.getPos().up(), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .5, y, z + .5);
        net.minecraft.util.EnumFacing facing = tile.getWorld().getBlockState(tile.getPos()).getValue(net.vyroxes.minersprosperity.block.BlockWorktable.FACING);
        GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
        try
        {
            for (int i = 0; i < 9; i++)
            {
                ItemStack stack = tile.grid.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                GlStateManager.pushMatrix();
                try
                {
                    net.minecraft.client.renderer.RenderItem renderer = Minecraft.getMinecraft().getRenderItem();
                    boolean raised = renderer.getItemModelWithOverrides(stack, tile.getWorld(), null).isGui3d();
                    GlStateManager.translate(-.1875 + (i % 3) * .1875, raised ? 1.105 : 1.012, -.1875 + (i / 3) * .1875);
                    if (!raised) GlStateManager.rotate(-90, 1, 0, 0);
                    GlStateManager.scale(.18, .18, .18);
                    renderer.renderItem(stack, ItemCameraTransforms.TransformType.NONE);
                }
                finally { GlStateManager.popMatrix(); }
            }
        }
        finally
        {
            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    private double getDistanceSq(TileEntityWorktable tile)
    {
        net.minecraft.entity.Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        return camera == null ? Double.MAX_VALUE : tile.getDistanceSq(camera.posX, camera.posY, camera.posZ);
    }
}