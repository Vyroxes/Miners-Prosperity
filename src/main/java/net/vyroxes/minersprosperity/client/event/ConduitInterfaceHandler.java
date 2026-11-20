package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageConduitInterface;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import net.vyroxes.minersprosperity.transport.ConduitType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class ConduitInterfaceHandler
{
    private ConduitInterfaceHandler()
    {
    }

    private static @Nullable TileEntityConduit hoveredInterface(Minecraft mc)
    {
        if (mc.currentScreen != null || mc.player == null || mc.player.isSpectator() || mc.world == null) return null;
        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.subHit < 0 || hit.subHit >= 6) return null;
        if (!(mc.world.getTileEntity(hit.getBlockPos()) instanceof TileEntityConduit tile)
                || tile.conduit() == null || !tile.facade.isEmpty()) return null;
        return tile;
    }

    @SubscribeEvent
    public static void scroll(MouseEvent event)
    {
        if (event.getDwheel() == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        TileEntityConduit tile = hoveredInterface(mc);
        if (tile == null || !mc.player.isSneaking() || tile.conduit().type != ConduitType.UNIVERSAL) return;
        event.setCanceled(true);
        NetworkHandler.NETWORK.sendToServer(new MessageConduitInterface(tile.getPos(), mc.objectMouseOver.subHit,
                event.getDwheel() < 0 ? 1 : -1));
    }

    @SubscribeEvent
    public static void tooltip(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) return;
        TileEntityConduit tile = hoveredInterface(mc);
        if (tile == null || !mc.player.isSneaking()) return;
        int side = mc.objectMouseOver.subHit;
        int selected = tile.getInterfaceResource(side);
        ConduitType type = tile.conduit().type;
        boolean universal = type == ConduitType.UNIVERSAL;
        List<String> lines = new ArrayList<>(7);
        if (universal)
            lines.add(TextFormatting.WHITE + I18n.format("gui.conduit.interface.selected", I18n.format("gui.conduit.resource." + selected)));
        for (int resource = 0; resource < ConduitType.RESOURCE_COUNT; resource++)
        {
            if (!type.supports(resource)) continue;
            lines.add((resource == selected ? TextFormatting.WHITE : TextFormatting.GRAY)
                    + I18n.format("gui.conduit.resource." + resource)
                    + (resource == ConduitType.LOGIC_RESOURCE ? "" : ": " + I18n.format("gui.conduit.mode." + tile.ports[side][resource].mode)));
        }
        if (universal) lines.add(TextFormatting.GRAY + I18n.format("gui.conduit.interface.scroll"));
        if (selected != ConduitType.LOGIC_RESOURCE) lines.addAll(mc.fontRenderer.listFormattedStringToWidth(TextFormatting.GRAY + I18n.format("gui.conduit.interface.mode").replace("\\n", "\n"), 230));
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        GuiUtils.drawHoveringText(lines, width / 2 + 12, height / 2 + 12, width, height, 260, mc.fontRenderer);
    }
}