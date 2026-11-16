package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.item.ItemConduitProbe;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class ConduitHighlight
{
    private ConduitHighlight()
    {
    }

    private static void outline(AxisAlignedBB box, int color)
    {
        RenderGlobal.drawSelectionBoundingBox(box.grow(0.002), (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        ItemStack held = mc.player.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemConduitProbe)) held = mc.player.getHeldItemOffhand();
        if (!(held.getItem() instanceof ItemConduitProbe)) return;
        NBTTagCompound tag = held.getTagCompound();
        int type = tag == null ? 0 : tag.getInteger("Type");
        int tier = tag == null ? 0 : tag.getInteger("Tier");
        net.minecraft.entity.Entity camera = mc.getRenderViewEntity();
        if (camera == null) return;
        float partial = event.getPartialTicks();
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-x, -y, -z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2);
        int count = 0;
        for (TileEntity entry : mc.world.loadedTileEntityList)
        {
            if (!(entry instanceof TileEntityConduit tile) || tile.conduit() == null || tile.getDistanceSq(x,y,z) > 4096) continue;
            if (type != 0 && tile.conduit().type.ordinal() != type - 1 || tier != 0 && tile.conduit().tier.ordinal() != tier - 1) continue;
            if (++count > ModConfig.CONDUITS.highlightLimit) break;
            int color = mc.player.isSneaking() ? tile.conduit().tier.color : tile.conduit().type.color;
            AxisAlignedBB core = tile.conduit().coreShape(mc.world, tile.getPos());
            java.util.List<net.minecraft.util.EnumFacing> sides = new java.util.ArrayList<>();
            for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.VALUES)
                if (tile.conduit().connects(mc.world, tile.getPos(), side)) sides.add(side);
            boolean straight = tile.conduit().straightDirection(mc.world, tile.getPos()) != null;
            if (straight)
            {
                for (net.minecraft.util.EnumFacing side : sides) core = core.union(net.vyroxes.minersprosperity.block.BlockConduit.arm(side, !(mc.world.getTileEntity(tile.getPos().offset(side)) instanceof TileEntityConduit)));
            }
            outline(core.offset(tile.getPos()), color);
            for (net.minecraft.util.EnumFacing side : sides)
            {
                if (!straight) outline(net.vyroxes.minersprosperity.block.BlockConduit.arm(side, !(mc.world.getTileEntity(tile.getPos().offset(side)) instanceof TileEntityConduit)).offset(tile.getPos()), color);
                if (!(mc.world.getTileEntity(tile.getPos().offset(side)) instanceof TileEntityConduit))
                    outline(net.vyroxes.minersprosperity.block.BlockConduit.adapter(side).offset(tile.getPos()), color);
            }
        }
        GlStateManager.glLineWidth(1);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1,1,1,1);
        GlStateManager.popMatrix();
    }
}