package net.vyroxes.minersprosperity.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.block.machine.BlockMachine;
import net.vyroxes.minersprosperity.tile.TileEntityCrystallizer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

public final class CrystallizerTESR extends TileEntitySpecialRenderer<TileEntityCrystallizer>
{
    private static final ResourceLocation CORE = new ResourceLocation(Tags.MODID, "textures/blocks/crystallizer_core.png");

    @Override
    public void render(@NotNull TileEntityCrystallizer tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof BlockMachine)
                || state.getValue(BlockMachine.STATE) != BlockMachine.MachineState.ACTIVE) return;
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        int light = tile.getWorld().getCombinedLight(tile.getPos().offset(state.getValue(BlockMachine.FACING)), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        switch (state.getValue(BlockMachine.FACING))
        {
            case EAST: GlStateManager.rotate(-90, 0, 1, 0); break;
            case SOUTH: GlStateManager.rotate(180, 0, 1, 0); break;
            case WEST: GlStateManager.rotate(90, 0, 1, 0); break;
            default: break;
        }
        GlStateManager.translate(0, 0, -0.501);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1, 1, 1, 1);
        bindTexture(CORE);
        double phase = net.vyroxes.minersprosperity.config.ModConfig.MACHINES.machineTesrAnimations
                ? ((tile.getWorld().getTotalWorldTime() % 80) + partialTicks) * Math.PI / 40 : 0;
        double scale = 1 + 0.1 * Math.sin(phase * 2);
        GlStateManager.translate(0, (Math.sin(phase) - 0.5) / 16, 0);
        GlStateManager.scale(scale, scale, 1);
        drawCoreQuad();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
    }
    private void drawCoreQuad()
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-0.0625, -0.09375, 0).tex(0.5625, 0.625).endVertex();
        buffer.pos(0.0625, -0.09375, 0).tex(0.4375, 0.625).endVertex();
        buffer.pos(0.0625, 0.09375, 0).tex(0.4375, 0.4375).endVertex();
        buffer.pos(-0.0625, 0.09375, 0).tex(0.5625, 0.4375).endVertex();
        tessellator.draw();
    }
}