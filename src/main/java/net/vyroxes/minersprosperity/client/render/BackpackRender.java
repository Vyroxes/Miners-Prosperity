package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.integration.baubles.BaublesCompat;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.tile.TileEntityBackpack;
import org.jetbrains.annotations.NotNull;

public final class BackpackRender extends TileEntitySpecialRenderer<TileEntityBackpack>
{
    private static final ModelBase MODEL = new ModelBase() {};
    private static final ModelRenderer BODY = new ModelRenderer(MODEL, 0, 0);
    private static final int[] COLORS = {0x835735, 0x68667A, 0xD6D6D6, 0xADB38B, 0xE8BE36, 0xEAE1A0, 0x55D9D0, 0x382E4A, 0x35B86E, 0x4678BA, 0x494C53, 0x383945};
    private static final ModelRenderer STRAPS = new ModelRenderer(MODEL, 0, 0);
    private static final ModelRenderer POCKET = new ModelRenderer(MODEL, 0, 0);
    static
    {
        STRAPS.setTextureSize(16, 16);
        STRAPS.addBox(-4, 0, -5, 1, 11, 1);
        STRAPS.addBox(3, 0, -5, 1, 11, 1);
        STRAPS.addBox(-4, -1, -5, 1, 1, 6);
        STRAPS.addBox(3, -1, -5, 1, 1, 6);
        BODY.setTextureSize(16, 16);
        BODY.addBox(-5, 0, 0, 10, 12, 5);
        POCKET.setTextureSize(16, 16);
        POCKET.addBox(-4, 5, 5, 8, 6, 2);
    }

    private static void draw(ItemStack stack)
    {
        if (!(stack.getItem() instanceof ItemBackpackBase item)) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("minecraft", "textures/blocks/wool_colored_white.png"));
        int color = COLORS[item.getTier().ordinal()];
        GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        BODY.render(1.0F / 16);
        POCKET.render(1.0F / 16);
        STRAPS.render(1.0F / 16);
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void register()
    {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBackpack.class, new BackpackRender());
        if (!Loader.isModLoaded("baubles")) return;
        for (RenderPlayer renderer : Minecraft.getMinecraft().getRenderManager().getSkinMap().values())
        {
            renderer.addLayer(new LayerRenderer<AbstractClientPlayer>()
            {
                @Override
                public void doRenderLayer(@NotNull AbstractClientPlayer player, float swing, float amount, float partial,
                                                    float age, float yaw, float pitch, float scale)
                {
                    if (player.isInvisible()) return;
                    ItemStack stack = BaublesCompat.getBackpackStackFromBaubles(player);
                    if (stack.isEmpty()) return;
                    GlStateManager.pushMatrix();
                    if (player.isSneaking()) GlStateManager.translate(0, 0.2F, 0);
                    renderer.getMainModel().bipedBody.postRender(scale);
                    GlStateManager.translate(0, 0, 0.14F);
                    draw(stack);
                    GlStateManager.popMatrix();
                }

                @Override
                public boolean shouldCombineTextures()
                {
                    return false;
                }
            });
        }
    }

    @Override
    public void render(TileEntityBackpack tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.75, z + 0.5);
        net.minecraft.util.EnumFacing facing = tile.getWorld().getBlockState(tile.getPos()).getValue(net.vyroxes.minersprosperity.block.BlockBackpack.FACING);
        GlStateManager.rotate(180 - facing.getHorizontalAngle(), 0, 1, 0);
        GlStateManager.scale(1, -1, -1);
        GlStateManager.translate(0, 0, -1.0F / 16);
        draw(tile.getBackpack());
        GlStateManager.popMatrix();
    }
}