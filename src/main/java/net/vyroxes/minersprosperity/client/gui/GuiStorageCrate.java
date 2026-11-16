package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.inventory.container.ContainerStorageCrate;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageStorageCrate;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrate;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import net.minecraft.inventory.Slot;
import java.util.Locale;

public class GuiStorageCrate extends GuiContainer
{
    private final TileEntityStorageCrate tile;
    private int lastRevision = -1;
    private boolean editingLocks;
    private static final ResourceLocation ELEMENTS =
            new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");

    public GuiStorageCrate(
            @NotNull InventoryPlayer inventory, @NotNull TileEntityStorageCrate tile)
    {
        super(new ContainerStorageCrate(inventory, tile));
        this.tile = tile;
        xSize = 176;
        ySize = 166;
    }

    private void send(int action, int value)
    {
        NetworkHandler.NETWORK.sendToServer(
                new MessageStorageCrate(inventorySlots.windowId, action, value));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        buttonList.clear();
        addButton(
                new GuiDefaultButton(
                        10,
                        guiLeft + 151,
                        guiTop + 6,
                        18,
                        18,
                        ELEMENTS,
                        16,
                        0,
                        0,
                        18,
                        I18n.format(
                                "gui.crate.redstone."
                                        + ((tile.slots() == 1 || tile.isCompacting())
                                                ? "single."
                                                : "")
                                        + tile.redstoneMode())));
        addButton(
                new GuiDefaultButton(
                        11,
                        guiLeft + 151,
                        guiTop + 27,
                        18,
                        18,
                        ELEMENTS,
                        70,
                        0,
                        0,
                        18,
                        I18n.format("gui.configuration.name")));
        addButton(new GuiDefaultButton(12, guiLeft + 7, guiTop + 6, 18, 18,
                ELEMENTS, editingLocks ? 88 : 106, 140, 0, 18,
                I18n.format("gui.crate.lock_edit", I18n.format(editingLocks
                        ? "gui.backpack.value.LockEdit.1" : "gui.backpack.value.LockEdit.0"))));
        ((ContainerStorageCrate) inventorySlots).refreshSlots();
        lastRevision = tile.revision;
    }

    @Override
    protected void actionPerformed(@NotNull GuiButton button)
    {
        if (button.id == 10) send(1, 1);
        else if (button.id == 11)
            NetworkHandler.sendOpenGuiUpdate(
                    GuiHandler.GuiTypes.MACHINE_CONFIGURATION.ordinal(), tile.getPos());
        else if (button.id == 12)
        {
            editingLocks = !editingLocks;
            initGui();
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException
    {
        if (editingLocks && (button == 0 || button == 1))
        {
            Slot slot = getSlotUnderMouse();
            if (slot != null && slot.slotNumber < tile.slots())
            {
                send(0, slot.slotNumber);
                return;
            }
        }
        if (button == 1 || button == 2)
        {
            for (GuiButton control : buttonList)
            {
                if ((control.id != 10 && control.id != 12) || !control.mousePressed(mc, x, y)) continue;
                if (control.id == 10) send(1, button == 2 ? 0 : -1);
                else
                {
                    editingLocks = button!=2 && !editingLocks;
                    initGui();
                }
                control.playPressSound(mc.getSoundHandler());
                return;
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (lastRevision != tile.revision) initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY)
    {
        mc.getTextureManager()
                .bindTexture(
                        new ResourceLocation(
                                Tags.MODID,
                                "textures/gui/storage_crate"
                                        + (tile.slots() == 1 ? "" : "_" + tile.slots())
                                        + ".png"));
        GlStateManager.color(1, 1, 1, 1);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        GuiMachineTitle.draw(
                fontRenderer,
                tile.getDisplayName().getUnformattedText(),
                xSize,
                guiLeft,
                guiTop,
                buttonList);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 73, 0x404040);
        for (int i = 0; i < tile.slots(); i++)
        {
            int x = ContainerStorageCrate.slotX(tile.slots(), i);
            int y = ContainerStorageCrate.slotY(tile.slots(), i);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            if (tile.locked(i))
            {
                drawRect(x - 1, y - 1, x + 17, y, 0xFFFFAA00);
                drawRect(x - 1, y + 16, x + 17, y + 17, 0xFFFFAA00);
                drawRect(x - 1, y, x, y + 16, 0xFFFFAA00);
                drawRect(x + 16, y, x + 17, y + 16, 0xFFFFAA00);
            }
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            if (tile.template(i).isEmpty() || !tile.creative() && tile.count(i) == 1) continue;
            long count = tile.count(i);
            String amount = tile.creative() ? "∞" : count < 10000 ? Long.toString(count)
                    : String.format(Locale.ROOT, "%.1fk", count / 1000.0).replace(".0k", "k");
            int width = fontRenderer.getStringWidth(amount);
            float scale = count < 100 || tile.creative() ? 1F : Math.min(1F, 16F / width);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 17F, y + 9F + fontRenderer.FONT_HEIGHT * (1F - scale) + (tile.creative() ? 1 : 0), 300);
            GlStateManager.scale(scale, scale, 1);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableBlend();
            fontRenderer.drawStringWithShadow(amount, -width, 0, 0xFFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial)
    {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partial);
        renderHoveredToolTip(mouseX, mouseY);
        GuiMachineTitle.drawTooltip(
                fontRenderer,
                tile.getDisplayName().getUnformattedText(),
                xSize,
                guiLeft,
                guiTop,
                buttonList,
                mouseX,
                mouseY,
                width,
                height);
        for (GuiButton button : buttonList)
        {
            if (button instanceof GuiDefaultButton
                    && ((GuiDefaultButton) button).getCurrentTooltip() != null)
                drawHoveringText(((GuiDefaultButton) button).getCurrentTooltip(), mouseX, mouseY);
        }
    }
}