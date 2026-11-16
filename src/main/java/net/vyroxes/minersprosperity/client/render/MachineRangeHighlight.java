package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class MachineRangeHighlight
{

    private static BlockPos selected;
    private static World selectedWorld;
    private static AxisAlignedBB terrainBounds;
    private static boolean[][] terrainOpen;
    private static long nextTerrainUpdate;

    private MachineRangeHighlight()
    {
    }

    public static void select(TileEntityMachine tile)
    {
        if (!tile.supportsRange()) return;
        selected = tile.getPos();
        selectedWorld = tile.getWorld();
        terrainBounds = null;
        terrainOpen = null;
    }

    @SubscribeEvent
    public static void openGui(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post event)
    {
        if (!(event.getGui() instanceof net.minecraft.client.gui.inventory.GuiContainer)) return;
        net.minecraft.inventory.Container container = ((net.minecraft.client.gui.inventory.GuiContainer) event.getGui()).inventorySlots;
        if (container instanceof net.vyroxes.minersprosperity.inventory.container.ContainerInventory)
            select(((net.vyroxes.minersprosperity.inventory.container.ContainerInventory) container).getTileEntity());
    }

    @SubscribeEvent
    public static void unload(net.minecraftforge.event.world.WorldEvent.Unload event)
    {
        if (event.getWorld() == selectedWorld)
        {
            selectedWorld = null;
            selected = null;
            terrainBounds = null;
            terrainOpen = null;
        }
    }

    @SubscribeEvent
    public static void tick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event)
    {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.world.getTotalWorldTime() % 20 != 0) return;
        if (mc.world == selectedWorld && selected != null && mc.world.isBlockLoaded(selected))
        {
            TileEntity current = mc.world.getTileEntity(selected);
            if (current instanceof TileEntityMachine && ((TileEntityMachine) current).showsRangeFor(mc.player.getUniqueID())
                    && current.getDistanceSq(mc.player.posX, mc.player.posY, mc.player.posZ) <= 16384) return;
        }
        TileEntityMachine nearest = null;
        double nearestDistance = 16384;
        for (TileEntity entry : mc.world.loadedTileEntityList)
        {
            if (!(entry instanceof TileEntityMachine machine) || entry.isInvalid()) continue;
            if (!machine.showsRangeFor(mc.player.getUniqueID())) continue;
            double distance = machine.getDistanceSq(mc.player.posX, mc.player.posY, mc.player.posZ);
            if (distance > nearestDistance) continue;
            nearest = machine;
            nearestDistance = distance;
        }
        if (nearest != null) select(nearest);
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.world != selectedWorld || selected == null || !mc.world.isBlockLoaded(selected)) return;
        TileEntity entry = mc.world.getTileEntity(selected);
        if (!(entry instanceof TileEntityMachine tile)) return;
        Entity camera = mc.getRenderViewEntity();
        if (mc.player == null || !tile.showsRangeFor(mc.player.getUniqueID()) || camera == null || tile.getDistanceSq(camera.posX, camera.posY, camera.posZ) > 16384) return;
        float partial = event.getPartialTicks();
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;
        GlStateManager.pushMatrix();

        Tessellator.getInstance().getBuffer().setTranslation(-x, -y, -z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2);
        try
        {
            AxisAlignedBB rangeBox = tile.rangeBounds();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.disableCull();

            GlStateManager.disableAlpha();
            if (tile.rangeDisplayMode() == 1)
            {
                GlStateManager.enableDepth();
                drawTerrain(mc.world, rangeBox, x, y + camera.getEyeHeight(), z);
            }
            else
            {
                RenderGlobal.renderFilledBox(rangeBox, .25F, .7F, 1, .16F);
                RenderGlobal.drawSelectionBoundingBox(rangeBox, .25F, .7F, 1, 1);
            }
            GlStateManager.enableAlpha();
            GlStateManager.enableCull();
            for (long target : tile.rangeHighlights())
                RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(BlockPos.fromLong(target)).grow(.004), .25F, 1, .35F, 1);
        }
        finally
        {
            GlStateManager.glLineWidth(1);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            Tessellator.getInstance().getBuffer().setTranslation(0, 0, 0);
            GlStateManager.popMatrix();
        }
    }

    private static void drawTerrain(World world, AxisAlignedBB box, double cameraX, double cameraY, double cameraZ)
    {
        int width = (int) (box.maxX - box.minX);
        int height = (int) (box.maxY - box.minY);
        int depth = (int) (box.maxZ - box.minZ);
        long tick = world.getTotalWorldTime();
        if (!box.equals(terrainBounds) || terrainOpen == null || tick >= nextTerrainUpdate)
        {
            terrainBounds = box;
            nextTerrainUpdate = tick + 10;
            terrainOpen = new boolean[12][];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int layer = 0; layer < 12; layer++)
            {
                int face = layer % 6;
                int columns = face < 2 || face >= 4 ? width : depth;
                int rows = face < 4 ? height : depth;
                boolean[] open = terrainOpen[layer] = new boolean[columns * rows];
                for (int v = 0; v < rows; v++)
                    for (int u = 0; u < columns; u++)
                    {
                        int x = (int) box.minX + (face < 2 || face >= 4 ? u : face == 2 ? 0 : width - 1);
                        int y = (int) box.minY + (face < 4 ? v : face == 4 ? 0 : height - 1);
                        int z = (int) box.minZ + (face < 2 ? face == 0 ? 0 : depth - 1 : face < 4 ? u : v);
                        if (layer >= 6)
                        {
                            if (face < 2) z += face == 0 ? -1 : 1;
                            else if (face < 4) x += face == 2 ? -1 : 1;
                            else y += face == 4 ? -1 : 1;
                        }
                        cursor.setPos(x, y, z);
                        if (y < 0 || y > 255 || !world.isBlockLoaded(cursor)) continue;

                        open[v * columns + u] = !world.getBlockState(cursor).isFullCube();
                    }
            }
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        AxisAlignedBB renderBox = new AxisAlignedBB(
                box.minX + Math.copySign(.003, cameraX - box.minX),
                box.minY + Math.copySign(.003, cameraY - box.minY),
                box.minZ + Math.copySign(.003, cameraZ - box.minZ),
                box.maxX + Math.copySign(.003, cameraX - box.maxX),
                box.maxY + Math.copySign(.003, cameraY - box.maxY),
                box.maxZ + Math.copySign(.003, cameraZ - box.maxZ));
        GlStateManager.disableCull();
        for (int pass = 0; pass < 2; pass++)
        {
                buffer.begin(pass == 0 ? 7 : 1, DefaultVertexFormats.POSITION_COLOR);
                for (int face = 0; face < 6; face++)
                {
                    int columns = face < 2 || face >= 4 ? width : depth;
                    int rows = face < 4 ? height : depth;
                    double plane = face == 0 ? box.minZ : face == 1 ? box.maxZ
                            : face == 2 ? box.minX : face == 3 ? box.maxX
                            : face == 4 ? box.minY : box.maxY;
                    double camera = face < 2 ? cameraZ : face < 4 ? cameraX : cameraY;
                    boolean outside = face % 2 == 0 ? camera < plane : camera > plane;
                    boolean[] open = terrainOpen[face + (outside ? 6 : 0)];
                    plane = face == 0 ? renderBox.minZ : face == 1 ? renderBox.maxZ
                            : face == 2 ? renderBox.minX : face == 3 ? renderBox.maxX
                            : face == 4 ? renderBox.minY : renderBox.maxY;
                    for (int v = 0; v < rows; v++)
                        for (int u = 0; u < columns; u++)
                        {
                            int index = v * columns + u;
                            if (!open[index]) continue;
                            if (pass == 0)
                            {
                                boundaryVertex(buffer, renderBox, face, plane, u, v, .16F);
                                boundaryVertex(buffer, renderBox, face, plane, u + 1, v, .16F);
                                boundaryVertex(buffer, renderBox, face, plane, u + 1, v + 1, .16F);
                                boundaryVertex(buffer, renderBox, face, plane, u, v + 1, .16F);
                            }
                            else
                            {
                                if (u == 0 || !open[index - 1]) boundaryEdge(buffer, renderBox, face, plane, u, v, u, v + 1);
                                if (u == columns - 1 || !open[index + 1]) boundaryEdge(buffer, renderBox, face, plane, u + 1, v, u + 1, v + 1);
                                if (v == 0 || !open[index - columns]) boundaryEdge(buffer, renderBox, face, plane, u, v, u + 1, v);
                                if (v == rows - 1 || !open[index + columns]) boundaryEdge(buffer, renderBox, face, plane, u, v + 1, u + 1, v + 1);
                            }
                        }
                }
                tessellator.draw();
            }

    }

    private static void boundaryEdge(BufferBuilder buffer, AxisAlignedBB box, int face, double plane,
                                     int u1, int v1, int u2, int v2)
    {
        boundaryVertex(buffer, box, face, plane, u1, v1, 1);
        boundaryVertex(buffer, box, face, plane, u2, v2, 1);
    }

    private static void boundaryVertex(BufferBuilder buffer, AxisAlignedBB box, int face, double plane,
                                       int u, int v, float alpha)
    {
        double x = face < 2 || face >= 4 ? box.minX + (box.maxX - box.minX) * u / Math.round(box.maxX - box.minX) : plane;
        double y = face < 4 ? box.minY + (box.maxY - box.minY) * v / Math.round(box.maxY - box.minY) : plane;
        double z = face < 2 ? plane : face < 4 ? box.minZ + (box.maxZ - box.minZ) * u / Math.round(box.maxZ - box.minZ) : box.minZ + (box.maxZ - box.minZ) * v / Math.round(box.maxZ - box.minZ);

        buffer.pos(x, y, z).color(.25F, .7F, 1, alpha).endVertex();
    }
}