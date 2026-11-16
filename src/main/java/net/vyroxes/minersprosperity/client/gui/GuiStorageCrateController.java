package net.vyroxes.minersprosperity.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.inventory.container.ContainerInventory;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageStorageController;
import net.vyroxes.minersprosperity.tile.TileEntityStorageCrateController;
import org.jetbrains.annotations.NotNull;

public class GuiStorageCrateController extends GuiContainer
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(Tags.MODID, "textures/gui/machine_configuration_storage_crate_controller.png");
    private static final ResourceLocation ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private final TileEntityStorageCrateController tile;

    public GuiStorageCrateController(InventoryPlayer inventory, TileEntityStorageCrateController tile)
    {
        super(new ContainerInventory(inventory, tile));
        this.tile = tile;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        buttonList.clear();
        addButton(new ControllerButton(0, guiLeft + 49, guiTop + 40));
        addButton(new ControllerButton(1, guiLeft + 109, guiTop + 40));
        addButton(new ControllerButton(2, guiLeft + 7, guiTop + 6));
        addButton(new ControllerButton(3, guiLeft + 7, guiTop + 27));
        addButton(new GuiDefaultButton(4, guiLeft + 151, guiTop + 27, 18, 18, ELEMENTS, 70, 0, 0, 18, I18n.format("gui.configuration.name")));
        addButton(new GuiDefaultButton(5, guiLeft + 151, guiTop + 48, 18, 18, ELEMENTS, 88, 0, 0, 18, I18n.format("gui.upgrades.name")));
        addButton(new GuiDefaultButton(6, guiLeft + 151, guiTop + 6, 18, 18, ELEMENTS, 16 + 18 * tile.getRedstoneControlButtonState(), 0, 0, 18, ""));
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 4 || button.id == 5)
        {
            NetworkHandler.sendOpenGuiUpdate((button.id == 4 ? GuiHandler.GuiTypes.MACHINE_CONFIGURATION : GuiHandler.GuiTypes.MACHINE_UPGRADES).ordinal(), tile.getPos());
            return;
        }
        if (button.id == 6)
        {
            tile.setRedstoneControlButtonState((tile.getRedstoneControlButtonState() + 1) % 3);
            initGui();
            return;
        }
        if (button.id < 2)
        {
            int value = tile.range() + (button.id == 0 ? -1 : 1) * (isShiftKeyDown() ? 4 : 1);
            tile.setRange(value);
        }
        else if (button.id == 2) tile.setRangeDisplayMode((tile.rangeDisplayMode() + 1) % 3);
        else NetworkHandler.NETWORK.sendToServer(new MessageStorageController(tile.getPos(), inventorySlots.windowId));
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) throws java.io.IOException
    {
        if (mouseButton == 1 || mouseButton == 2)
        {
            for (GuiButton button : buttonList)
            {
                if (button.id == 4 || button.id == 5 || !button.visible
                        || x < button.x || y < button.y || x >= button.x + button.width || y >= button.y + button.height) continue;
                boolean reset = mouseButton == 2;
                if (button.id < 2)
                    tile.setRange(reset ? net.vyroxes.minersprosperity.config.ModConfig.BLOCKS.storageControllerBaseRange
                            : tile.range() + (button.id == 0 ? 1 : -1) * (isShiftKeyDown() ? 4 : 1));
                else if (button.id == 2) tile.setRangeDisplayMode(reset ? 0 : Math.floorMod(tile.rangeDisplayMode() - 1, 3));
                else if (button.id == 3)
                    NetworkHandler.NETWORK.sendToServer(new MessageStorageController(tile.getPos(), inventorySlots.windowId, reset));
                else if (button.id == 6) tile.setRedstoneControlButtonState(reset ? 0 : Math.floorMod(tile.getRedstoneControlButtonState() - 1, 3));
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
        for (GuiButton button : buttonList)
        {
            if (button.id == 0) button.enabled = tile.range() > 1;
            if (button.id == 1) button.enabled = tile.range() < tile.maxRange();
        }
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        for (GuiButton button : buttonList)
            if (button instanceof GuiDefaultButton ? ((GuiDefaultButton) button).isHovered(mouseX, mouseY) : button.isMouseOver())
            {
                String key = button.id < 2 ? "gui.machine.range"
                        : button.id == 2 ? "gui.machine.range.mode." + tile.rangeDisplayMode()
                        : button.id == 3 ? (tile.assignsEmpty() ? "gui.controller.assign" : "gui.controller.existing")
                        : button.id == 4 ? "gui.configuration.name"
                        : button.id == 5 ? "gui.upgrades.name"
                        : tile.getRedstoneControlButtonState() == 0 ? "gui.redstone_ignored.name"
                        : tile.getRedstoneControlButtonState() == 1 ? "gui.redstone_low_signal.name" : "gui.redstone_high_signal.name";
                java.util.List<String> tooltip = new java.util.ArrayList<>();
                tooltip.add(I18n.format(key, tile.range(), tile.maxRange()));
                if (button.id < 4)
                    tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + I18n.format(button.id < 2 ? "gui.machine.range.desc"
                            : button.id == 2 ? "gui.machine.range.mode.desc"
                            : tile.assignsEmpty() ? "gui.controller.assign.desc" : "gui.controller.existing.desc"));
                net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, width / 2, fontRenderer);
            }
        GuiMachineTitle.drawTooltip(fontRenderer, tile.getDisplayName().getUnformattedText(), xSize, guiLeft, guiTop, buttonList, mouseX, mouseY, width, height);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        GuiMachineTitle.draw(fontRenderer, tile.getDisplayName().getUnformattedText(), xSize, guiLeft, guiTop, buttonList);
        String connected = I18n.format("gui.controller.connected", tile.connectedCount());
        fontRenderer.drawString(connected, (xSize - fontRenderer.getStringWidth(connected)) / 2, 25, 4210752);
        String range = tile.range() + " / " + tile.maxRange();
        fontRenderer.drawString(range, (xSize - fontRenderer.getStringWidth(range)) / 2, 45, 4210752);
        fontRenderer.drawString(I18n.format("key.categories.inventory"), 8, 73, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawRect(guiLeft + 7, guiTop + 18, guiLeft + 169, guiTop + 72, 0xFFC6C6C6);
    }

    private class ControllerButton extends GuiButton
    {
        private ControllerButton(int id, int x, int y)
        {
            super(id, x, y, 18, 18, "");
        }

        @Override
        public void drawButton(@NotNull Minecraft mc, int mouseX, int mouseY, float partialTicks)
        {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int textureX;
            int textureY;
            if (id < 2)
            {
                textureX = id == 0 ? 18 : 0;
                textureY = 182;
            }
            else if (id == 2)
            {
                textureX = tile.rangeDisplayMode() * 18;
                textureY = tile.rangeDisplayMode() == 2 ? 214 : 218;
            }
            else
            {
                textureX = tile.assignsEmpty() ? 160 : 142;
                textureY = 140;
            }
            if (enabled && hovered) textureY += 18;
            float brightness = enabled ? 1 : 0.5F;
            GlStateManager.color(brightness, brightness, brightness, 1);
            mc.getTextureManager().bindTexture(ELEMENTS);
            drawTexturedModalRect(x, y, textureX, textureY, width, height);
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}