package net.vyroxes.minersprosperity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.*;
import net.minecraftforge.client.MinecraftForgeClient;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.transport.ConduitType;
import org.lwjgl.opengl.GL11;

public class ConduitTESR extends TileEntitySpecialRenderer<TileEntityConduit>
{
    private static final ResourceLocation[] INTERFACES = {texture("interface_front_off"), texture("interface_front_insert"),
            texture("interface_front_extract"), texture("interface_front_insert_extract")};
    private static final ResourceLocation SIDES = texture("interface_sides");
    private static final ResourceLocation ARROWS = texture("insert_extract_arrows");
    private static final ResourceLocation[] NODES = {texture("redstone_conduit_node_off"), texture("redstone_conduit_node_active"), texture("redstone_conduit_node_internal")};
    private static final ResourceLocation[] ARMS = {texture("redstone_conduit_arm_off"), texture("redstone_conduit_arm_active")};
    private static final ResourceLocation[] SHORT_ARMS = {texture("redstone_conduit_arm_short_off"), texture("redstone_conduit_arm_short_active")};
    private static final ResourceLocation[][] UNIVERSAL_TEXTURES = {
            {texture("universal_conduit_node_off"), texture("universal_conduit_node_active"), texture("universal_conduit_node_internal")},
            {texture("universal_conduit_arm_off"), texture("universal_conduit_arm_active")},
            {texture("universal_conduit_arm_short_off"), texture("universal_conduit_arm_short_active")}};
    private static final int[] RESOURCE_COLORS = {0xC500FF, 0x00BEFF, 0xD4DC36, 0xFF0000};
    private static final ResourceLocation[][][] ENERGY_TEXTURES = tierTextures(ConduitType.ENERGY);
    private static final ResourceLocation[][][] ITEM_TEXTURES = tierTextures(ConduitType.ITEM);
    private static final ResourceLocation[][][] FLUID_TEXTURES = tierTextures(ConduitType.FLUID);

    private static ResourceLocation[][][] tierTextures(ConduitType type)
    {
        net.vyroxes.minersprosperity.transport.ConduitTier[] tiers = net.vyroxes.minersprosperity.transport.ConduitTier.values();
        ResourceLocation[][][] textures = new ResourceLocation[tiers.length][3][];
        String[] parts = {"node", "arm", "arm_short"};
        for (int tier = 0; tier < tiers.length; tier++) for (int part = 0; part < parts.length; part++)
        {
            String name = tiers[tier].name().toLowerCase(java.util.Locale.ROOT) + "_" + type.id + "_" + parts[part];
            textures[tier][part] = new ResourceLocation[part == 0 ? 3 : 2];
            if (part == 0) textures[tier][part][2] = texture(name + "_internal");
            textures[tier][part][0] = texture(name + "_off");
            textures[tier][part][1] = texture(name + "_active");
        }
        return textures;
    }

    private static final int[][] FACES = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};

    private static final int[][] INTERFACE_EDGES = interfaceEdges();

    private static int[][] interfaceEdges()
    {
        java.util.List<int[]> edges = new java.util.ArrayList<>();
        for (int y = 0; y < 20; y++) for (int x = 0; x < 20; x++)
        {
            if (!interfacePixel(x, y)) continue;
            for (int edge = 0; edge < 4; edge++)
            {
                int dx = edge == 0 ? -1 : edge == 1 ? 1 : 0;
                int dy = edge == 2 ? -1 : edge == 3 ? 1 : 0;
                if (!interfacePixel(x + dx, y + dy)) edges.add(new int[]{x, y, edge});
            }
        }
        return edges.toArray(new int[edges.size()][]);
    }

    private static ResourceLocation texture(String name)
    {
        return new ResourceLocation(Tags.MODID, "textures/blocks/" + name + ".png");
    }

    @Override
    public void render(TileEntityConduit tile, double x, double y, double z, float partial, int destroyStage, float alpha)
    {
        if (tile.conduit() == null || destroyStage >= 0) return;
        boolean glassPass = MinecraftForgeClient.getRenderPass() == 1;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        int light = tile.getWorld().getCombinedLight(tile.getPos(), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
        boolean translucent = tile.conduit().type == ConduitType.ITEM || tile.conduit().type == ConduitType.FLUID
                || tile.conduit().type == ConduitType.UNIVERSAL;
        if (!glassPass)
        {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, translucent ? 0.99F : 0.1F);
            drawConduit(tile, false, partial);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            if (ModConfig.CONDUITS.animations) for (net.minecraft.nbt.NBTTagCompound event : tile.itemTransits)
            {
                double progress = (tile.getWorld().getTotalWorldTime() + partial - event.getDouble("Start")) / Math.max(0.0001, event.getDouble("Duration"));
                if (progress < 0 || progress >= 1) continue;
                net.minecraft.item.ItemStack icon = new net.minecraft.item.ItemStack(event.getCompoundTag("Item"));
                if (icon.isEmpty()) continue;
                EnumFacing from = EnumFacing.byIndex(event.getInteger("From"));
                EnumFacing to = EnumFacing.byIndex(event.getInteger("To"));
                double a = Math.max(0, 0.5 - progress);
                double b = Math.max(0, progress - 0.5);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GlStateManager.enableTexture2D();
                GlStateManager.color(1, 1, 1, 1);
                double itemX = 0.5 + from.getXOffset() * a + to.getXOffset() * b;
                double itemY = 0.5 + from.getYOffset() * a + to.getYOffset() * b;
                double itemZ = 0.5 + from.getZOffset() * a + to.getZOffset() * b;
                clipTransit(tile, itemX, itemY, itemZ);
                GlStateManager.pushMatrix();
                try
                {
                    GlStateManager.translate(itemX, itemY, itemZ);
                    RenderItem renderer = Minecraft.getMinecraft().getRenderItem();
                    if (!renderer.getItemModelWithOverrides(icon, tile.getWorld(), null).isGui3d())
                    {
                        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0, 1, 0);
                        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1, 0, 0);
                    }
                    GlStateManager.scale(0.18, 0.18, 0.18);
                    renderer.renderItem(icon, ItemCameraTransforms.TransformType.NONE);
                }
                finally
                {
                    GlStateManager.popMatrix();
                    for (int plane = 0; plane < 6; plane++) GL11.glDisable(GL11.GL_CLIP_PLANE0 + plane);
                }
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            }
            if (ModConfig.CONDUITS.animations) for (net.minecraft.nbt.NBTTagCompound event : tile.fluidTransits)
            {
                double progress = (tile.getWorld().getTotalWorldTime() + partial - event.getDouble("Start")) / Math.max(1, event.getDouble("Duration"));
                if (progress < 0 || progress >= 1) continue;
                Fluid fluid = FluidRegistry.getFluid(event.getString("Fluid"));
                if (fluid == null) continue;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, fluid.getLuminosity() > 0 ? 240 : lightX, fluid.getLuminosity() > 0 ? 240 : lightY);
                ResourceLocation texture = fluid.getFlowing() == null ? fluid.getStill() : fluid.getFlowing();
                TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString());
                EnumFacing from = EnumFacing.byIndex(event.getInteger("From"));
                EnumFacing to = EnumFacing.byIndex(event.getInteger("To"));
                double a = Math.max(0, 0.5 - progress);
                double b = Math.max(0, progress - 0.5);
                double cx = 0.5 + from.getXOffset() * a + to.getXOffset() * b;
                double cy = 0.5 + from.getYOffset() * a + to.getYOffset() * b;
                double cz = 0.5 + from.getZOffset() * a + to.getZOffset() * b;
                clipTransit(tile, cx, cy, cz);
                try
                {
                    box(new AxisAlignedBB(Math.max(0.001, cx - 0.123), Math.max(0.001, cy - 0.123), Math.max(0.001, cz - 0.123),
                            Math.min(0.999, cx + 0.123), Math.min(0.999, cy + 0.123), Math.min(0.999, cz + 0.123)), fluid.getColor(), 1, sprite);
                }
                finally
                {
                    for (int plane = 0; plane < 6; plane++) GL11.glDisable(GL11.GL_CLIP_PLANE0 + plane);
                }
            }
        }
        if (glassPass)
        {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
            GlStateManager.depthMask(false);
            GlStateManager.enableAlpha();
            GlStateManager.enableCull();
            GlStateManager.alphaFunc(GL11.GL_LESS, 0.99F);
            drawConduit(tile, true, partial);
            GlStateManager.disableCull();
            GlStateManager.depthMask(true);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        if (!glassPass && tile.facade.getItem() instanceof ItemBlock facade)
        {
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            var state = facade.getBlock().getStateFromMeta(tile.facade.getMetadata());
            state = state.getActualState(tile.getWorld(), tile.getPos());
            var dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            var model = dispatcher.getModelForState(state);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            buffer.setTranslation(-tile.getPos().getX(), -tile.getPos().getY(), -tile.getPos().getZ());
            try
            {
                dispatcher.getBlockModelRenderer().renderModel(tile.getWorld(), model, state, tile.getPos(), buffer, false);
            }
            finally
            {
                buffer.setTranslation(0, 0, 0);
            }
            if (tile.transparentFacade)
            {
                int stride = buffer.getVertexFormat().getSize();
                int offset = buffer.getVertexFormat().getColorOffset() + 3;
                for (int vertex = 0; vertex < buffer.getVertexCount(); vertex++) buffer.getByteBuffer().put(vertex * stride + offset, (byte) 72);
            }
            GlStateManager.depthMask(!tile.transparentFacade);
            Tessellator.getInstance().draw();
            GlStateManager.depthMask(true);
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawConduit(TileEntityConduit tile, boolean glass, float partial)
    {
        boolean redstone = tile.conduit().type == ConduitType.REDSTONE;
        int redstoneTexture = tile.redstoneActive ? 1 : 0;
        for (int signal : tile.signals)
        {
            if (signal <= 0) continue;
            redstoneTexture = 1;
            break;
        }
        boolean energy = tile.conduit().type == ConduitType.ENERGY;
        boolean item = tile.conduit().type == ConduitType.ITEM;
        boolean fluidPipe = tile.conduit().type == ConduitType.FLUID;
        boolean universal = tile.conduit().type == ConduitType.UNIVERSAL;
        boolean textured = redstone || energy || item || fluidPipe || universal;
        long now = tile.getWorld().getTotalWorldTime();
        int textureState = energy || fluidPipe ? (tile.resourceUntil[fluidPipe ? 2 : 0] > now ? 1 : 0) : redstoneTexture;
        if (universal) textureState = tile.isActive(now) ? 1 : 0;
        if (item) textureState = transitActive(tile.itemTransits, now + partial) ? 1 : 0;
        if (fluidPipe) textureState = transitActive(tile.fluidTransits, now + partial) ? 1 : 0;
        if (universal && (transitActive(tile.itemTransits, now + partial) || transitActive(tile.fluidTransits, now + partial))) textureState = 1;
        ResourceLocation[][] tierTextures = energy ? ENERGY_TEXTURES[tile.conduit().tier.ordinal()]
                : item ? ITEM_TEXTURES[tile.conduit().tier.ordinal()]
                : fluidPipe ? FLUID_TEXTURES[tile.conduit().tier.ordinal()] : universal ? UNIVERSAL_TEXTURES : null;
        EnumFacing straight = textured ? tile.conduit().straightDirection(tile.getWorld(), tile.getPos()) : null;
        if (textured && straight == null)
        {
            int connectedFaces = 0;
            for (EnumFacing side : EnumFacing.VALUES)
            {
                if (!tile.conduit().connects(tile.getWorld(), tile.getPos(), side)) continue;
                connectedFaces |= switch (side)
                {
                    case NORTH -> 1;
                    case SOUTH -> 2;
                    case WEST -> 4;
                    case EAST -> 8;
                    case UP -> 16;
                    case DOWN -> 32;
                };
            }
            if (connectedFaces != 63)
            {
                bindTexture(tierTextures != null ? tierTextures[0][textureState] : NODES[textureState]);
                texturedBox(5, 5, 5, 11, 11, 11, 5, 5, 6, 6, 63 & ~connectedFaces, energy);
            }
            if (connectedFaces != 0)
            {
                bindTexture(tierTextures != null ? tierTextures[0][2] : NODES[2]);
                texturedBox(5, 5, 5, 11, 11, 11, 5, 5, 6, 6, connectedFaces, energy);
            }
        }
        for (EnumFacing side : EnumFacing.VALUES)
        {
            if (!tile.conduit().connects(tile.getWorld(), tile.getPos(), side)) continue;
            if (straight != null && side != straight) continue;
            boolean interfaceSide = !(tile.getWorld().getTileEntity(tile.getPos().offset(side)) instanceof TileEntityConduit);
            int resource = tile.getInterfaceResource(side.getIndex());
            int mode = tile.ports[side.getIndex()][resource].mode;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.5, 0.5, 0.5);
            switch (side)
            {
                case NORTH -> GlStateManager.rotate(180, 0, 1, 0);
                case WEST -> GlStateManager.rotate(-90, 0, 1, 0);
                case EAST -> GlStateManager.rotate(90, 0, 1, 0);
                case UP -> GlStateManager.rotate(-90, 1, 0, 0);
                case DOWN -> GlStateManager.rotate(90, 1, 0, 0);
            }
            GlStateManager.translate(-0.5, -0.5, -0.5);
            if (textured)
            {
                bindTexture(tierTextures != null ? tierTextures[interfaceSide ? 2 : 1][textureState]
                        : interfaceSide ? SHORT_ARMS[textureState] : ARMS[textureState]);
                if (straight != null)
                {
                    for (int segment = 0; segment < 4; segment++)
                        texturedBox(6, 6, segment * 4, 10, 10, (segment + 1) * 4, 5.5, 6, 5, 4, 60);
                }
                else
                {
                    texturedBox(interfaceSide ? 5.5 : 6, interfaceSide ? 5.5 : 6, 11,
                            interfaceSide ? 10.5 : 10, interfaceSide ? 10.5 : 10, interfaceSide ? 15 : 16,
                            5.5, 6, 5, 4, 60);
                }
            }
            if (interfaceSide && !glass)
            {
                bindTexture(SIDES);
                drawInterfaceSides(mode);
                bindTexture(INTERFACES[mode]);
                texturedBox(3, 3, 15, 13, 13, 16, 3, 3, 10, 10, 3);
                drawArrows(mode);
                if (universal) drawResourceMarker(RESOURCE_COLORS[resource]);
            }
            GlStateManager.popMatrix();
        }
    }

    private static boolean transitActive(Iterable<net.minecraft.nbt.NBTTagCompound> transits, double now)
    {
        for (net.minecraft.nbt.NBTTagCompound transit : transits)
        {
            double start = transit.getDouble("Start");
            if (now >= start && now < start + transit.getDouble("Duration") + 20) return true;
        }
        return false;
    }

    private static void drawResourceMarker(int color)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        for (int face = 0; face < 4; face++)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.5, 0.5, 0);
            GlStateManager.rotate(face * 90, 0, 0, 1);
            GlStateManager.translate(-0.5, -0.5, 0);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            buffer.pos(5.5 / 16D, 10.505 / 16D, 14.75 / 16D).endVertex();
            buffer.pos(10.5 / 16D, 10.505 / 16D, 14.75 / 16D).endVertex();
            buffer.pos(10.5 / 16D, 10.505 / 16D, 15 / 16D).endVertex();
            buffer.pos(5.5 / 16D, 10.505 / 16D, 15 / 16D).endVertex();
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
    }

    private static final java.nio.DoubleBuffer CLIP_PLANE = org.lwjgl.BufferUtils.createDoubleBuffer(4);

    private static void clipTransit(TileEntityConduit tile, double x, double y, double z)
    {
        for (EnumFacing side : EnumFacing.VALUES)
        {
            int axis = side.getAxis() == EnumFacing.Axis.X ? 0 : side.getAxis() == EnumFacing.Axis.Y ? 1 : 2;
            double center = axis == 0 ? x : axis == 1 ? y : z;
            boolean positive = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE;
            boolean machine = tile.conduit().connects(tile.getWorld(), tile.getPos(), side)
                    && !(tile.getWorld().getTileEntity(tile.getPos().offset(side)) instanceof TileEntityConduit);
            double boundary = positive ? Math.min(center + 0.12, machine ? 15 / 16D : 1)
                    : Math.max(center - 0.12, machine ? 1 / 16D : 0);
            CLIP_PLANE.clear();
            CLIP_PLANE.put(-side.getXOffset()).put(-side.getYOffset()).put(-side.getZOffset())
                    .put(positive ? boundary : -boundary).flip();
            int plane = GL11.GL_CLIP_PLANE0 + side.getIndex();
            GL11.glClipPlane(plane, CLIP_PLANE);
            GL11.glEnable(plane);
        }
    }

    private static boolean interfacePixel(int x, int y)
    {
        if (x < 0 || x >= 20 || y < 0 || y >= 20) return false;
        if (y == 0 || y == 19) return x > 0 && x < 19 && x != 9 && x != 10;
        return (y != 9 && y != 10) || (x > 0 && x < 19);
    }

    private void drawInterfaceSides(int mode)
    {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        double v = mode == 0 ? 6 : (mode - 1) * 2;
        for (int[] boundary : INTERFACE_EDGES)
        {
            int x = boundary[0];
            int y = boundary[1];
            int edge = boundary[2];
            double x1 = 3 + (x + (edge == 1 ? 1 : 0)) * 0.5;
            double y1 = 13 - (y + (edge == 3 ? 1 : 0)) * 0.5;
            double x2 = x1 + (edge >= 2 ? 0.5 : 0);
            double y2 = y1 - (edge < 2 ? 0.5 : 0);
            double u = 3 + (edge < 2 ? y : x) * 0.5;

            if (edge < 2 && (y == 0 || y == 19) && (x == 8 || x == 11)) u = 3 + x * 0.5;
            if (edge >= 2 && (x == 0 || x == 19) && (y == 8 || y == 11)) u = 3 + y * 0.5;
            buffer.pos(x1/16, y1/16, 15/16D).tex(u/16, v/16).color(1F,1F,1F,1F).endVertex();
            buffer.pos(x2/16, y2/16, 15/16D).tex((u+0.5)/16, v/16).color(1F,1F,1F,1F).endVertex();
            buffer.pos(x2/16, y2/16, 1).tex((u+0.5)/16, (v+1)/16).color(1F,1F,1F,1F).endVertex();
            buffer.pos(x1/16, y1/16, 1).tex(u/16, (v+1)/16).color(1F,1F,1F,1F).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    private void drawArrows(int mode)
    {
        if (mode == 0) return;
        bindTexture(ARROWS);
        GlStateManager.enableAlpha();
        for (int face = 0; face < 4; face++)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.5, 0.5, 0);
            GlStateManager.rotate(face * 90, 0, 0, 1);
            GlStateManager.translate(-0.5, -0.5, 0);
            for (int direction = 1; direction <= 2; direction++)
            {
                if ((mode & direction) == 0) continue;
                double center = mode == 3 ? (direction == 1 ? 7 : 9) : 8;
                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                double startV = direction == 1 ? 15 : 8;
                double endV = direction == 1 ? 8 : 1;
                buffer.pos((center - 0.75) / 16, 10.51 / 16, 12 / 16D).tex(5 / 16D, startV / 16).color(1F,1F,1F,1F).endVertex();
                buffer.pos((center + 0.75) / 16, 10.51 / 16, 12 / 16D).tex(11 / 16D, startV / 16).color(1F,1F,1F,1F).endVertex();
                buffer.pos((center + 0.75) / 16, 10.51 / 16, 14 / 16D).tex(11 / 16D, endV / 16).color(1F,1F,1F,1F).endVertex();
                buffer.pos((center - 0.75) / 16, 10.51 / 16, 14 / 16D).tex(5 / 16D, endV / 16).color(1F,1F,1F,1F).endVertex();
                Tessellator.getInstance().draw();
            }
            GlStateManager.popMatrix();
        }
    }

    private void texturedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                             double u, double v, double width, double height, int mask)
    {
        texturedBox(minX, minY, minZ, maxX, maxY, maxZ, u, v, width, height, mask, false);
    }

    private void texturedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                             double u, double v, double width, double height, int mask, boolean rotateTexture)
    {
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int face = 0; face < FACES.length; face++)
        {
            if ((mask & 1 << face) == 0) continue;
            for (int vertex = 3; vertex >= 0; vertex--)
            {
                int corner = FACES[face][vertex];
                boolean highX = corner == 1 || corner == 2 || corner == 5 || corner == 6;
                boolean highY = corner == 2 || corner == 3 || corner == 6 || corner == 7;
                boolean highZ = corner >= 4;
                boolean highU = face == 2 || face == 3 ? highY : highX;
                boolean highV = face < 2 ? !highY : highZ;
                if (rotateTexture)
                {
                    boolean previousU = highU;
                    highU = highV;
                    highV = !previousU;
                }
                buffer.pos((highX ? maxX : minX) / 16, (highY ? maxY : minY) / 16, (highZ ? maxZ : minZ) / 16)
                        .tex((u + (highU ? width : 0)) / 16, (v + (highV ? height : 0)) / 16)
                        .color(1F, 1F, 1F, 1F).endVertex();
            }
        }
        Tessellator.getInstance().draw();
    }

    private void box(AxisAlignedBB box, int color, float alpha, TextureAtlasSprite sprite)
    {
        GlStateManager.disableCull();
        if (sprite == null) GlStateManager.disableTexture2D();
        else { GlStateManager.enableTexture2D(); bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); }
        double[][] p = {{box.minX,box.minY,box.minZ},{box.maxX,box.minY,box.minZ},{box.maxX,box.maxY,box.minZ},{box.minX,box.maxY,box.minZ},
                {box.minX,box.minY,box.maxZ},{box.maxX,box.minY,box.maxZ},{box.maxX,box.maxY,box.maxZ},{box.minX,box.maxY,box.maxZ}};
        int[][] faces = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int[] face : faces) for (int i = 0; i < 4; i++)
        {
            double[] vertex = p[face[i]];
            double u = sprite == null ? 0 : i == 0 || i == 3 ? sprite.getMinU() : sprite.getMaxU();
            double v = sprite == null ? 0 : i < 2 ? sprite.getMaxV() : sprite.getMinV();
            buffer.pos(vertex[0],vertex[1],vertex[2]).tex(u,v).color((color >> 16 & 255) / 255F,(color >> 8 & 255) / 255F,(color & 255) / 255F,alpha).endVertex();
        }
        Tessellator.getInstance().draw();
    }
}