package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.inventory.container.ContainerTrashCan;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.tile.TileEntityTrashCan;

import java.util.List;

public class GuiTrashCan extends GuiContainer
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/trash_can.png");
    private static final ResourceLocation GUI_ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private final TileEntityTrashCan tileEntity;

    public GuiTrashCan(InventoryPlayer inventory, TileEntityTrashCan tile)
    {
        super(new ContainerTrashCan(inventory, tile));
        tileEntity = tile;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.buttonList.clear();
        for (int medium = 0; medium < 3; medium++)
        {
            int mode = tileEntity.getDisposalMode(medium);
            String name = I18n.format("gui.trash_can.medium_" + medium);
            String tooltip = name + ": " + I18n.format("gui.trash_can.mode_" + mode);
            if (medium != 0) tooltip += "\n" + TextFormatting.GRAY + I18n.format("gui.trash_can.container_help");
            int textureX = medium == 0 ? (mode == 0 ? 160 : 178)
                    : medium == 1 ? (mode == 0 ? 144 : mode == 2 ? 162 : 180)
                    : mode == 0 ? 72 : mode == 2 ? 126 : 54;
            int textureY = medium == 0 ? 176 : medium == 1 || mode == 2 ? 212 : 214;
            addButton(new GuiDefaultButton(2 + medium, guiLeft + 7, guiTop + 6 + medium * 21, 18, 18,
                    GUI_ELEMENTS, textureX, textureY, 0, 18, tooltip));
        }
        int state = this.tileEntity.getRedstoneControlButtonState();
        String key = state == 1 ? "gui.redstone_low_signal.name" : state == 2 ? "gui.redstone_high_signal.name" : "gui.redstone_ignored.name";
        this.addButton(new GuiDefaultButton(0, this.guiLeft + 151, this.guiTop + 6, 18, 18, GUI_ELEMENTS, 16 + state * 18, 0, 0, 18, I18n.format(key)));
        this.addButton(new GuiDefaultButton(1, this.guiLeft + 151, this.guiTop + 27, 18, 18, GUI_ELEMENTS, 70, 0, 0, 18, I18n.format("gui.configuration.name")));
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 0)
        {
            this.tileEntity.setRedstoneControlButtonState((this.tileEntity.getRedstoneControlButtonState() + 1) % 3);
            this.initGui();
        }
        else if (button.id >= 2 && button.id <= 4)
        {
            int medium = button.id - 2;
            tileEntity.setDisposalMode(medium, Math.floorMod(tileEntity.getDisposalMode(medium) + (medium == 0 ? 1 : -1), medium == 0 ? 2 : 3));
            initGui();
        }
        else if (button.id == 1) NetworkHandler.sendOpenGuiUpdate(GuiHandler.GuiTypes.MACHINE_CONFIGURATION.ordinal(), this.tileEntity.getPos());
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) throws java.io.IOException
    {
        if (mouseButton == 1 || mouseButton == 2)
        {
            for (GuiButton button : buttonList)
            {
                if ((button.id != 0 && (button.id < 2 || button.id > 4)) || !button.mousePressed(mc, x, y)) continue;
                if (button.id >= 2)
                {
                    int medium = button.id - 2;
                    tileEntity.setDisposalMode(medium, mouseButton == 2 ? 1
                            : (tileEntity.getDisposalMode(medium) + 1) % (medium == 0 ? 2 : 3));
                    button.playPressSound(mc.getSoundHandler());
                    initGui();
                    return;
                }
                tileEntity.setRedstoneControlButtonState(mouseButton == 2 ? 0
                        : Math.floorMod(tileEntity.getRedstoneControlButtonState() - 1, 3));
                button.playPressSound(mc.getSoundHandler());
                initGui();
                return;
            }
        }
        super.mouseClicked(x, y, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton button : buttonList)
        {
            if (!(button instanceof GuiDefaultButton)) continue;
            List<String> tooltip = ((GuiDefaultButton) button).getCurrentTooltip();
            if (tooltip != null) GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, fontRenderer);
        }
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        String title = tileEntity.getDisplayName().getUnformattedText();
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        String warning = I18n.format("gui.trash_can.warning");
        float scale = Math.min(1.0F, 120.0F / fontRenderer.getStringWidth(warning));
        GlStateManager.pushMatrix();
        GlStateManager.translate(xSize / 2.0F, 18, 0);
        GlStateManager.scale(scale, scale, 1);
        fontRenderer.drawString(warning, -fontRenderer.getStringWidth(warning) / 2, 0, 0xAA2222);
        GlStateManager.popMatrix();
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 73, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawRect(guiLeft + 80, guiTop + 29, guiLeft + 96, guiTop + 45, 0x55FF2222);
    }
}