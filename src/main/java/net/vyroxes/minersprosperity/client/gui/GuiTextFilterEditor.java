package net.vyroxes.minersprosperity.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.vyroxes.minersprosperity.util.TextFilterList;
import net.vyroxes.minersprosperity.Tags;

public final class GuiTextFilterEditor extends GuiScreen
{
    public static final int SIZE = 248;
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private static final int ROWS = 6;
    private final ResourceLocation texture;
    private final TextFilterList.Type type;
    private final Consumer<String> save;
    private final Runnable close;
    private final List<String> entries = new ArrayList<>();
    private GuiTextField input;
    private int page;
    private boolean invalid;

    public GuiTextFilterEditor(ResourceLocation texture, TextFilterList.Type type, String initial, Consumer<String> save, Runnable close)
    {
        this.texture = texture;
        this.type = type;
        this.save = save;
        this.close = close;
        entries.addAll(Arrays.asList(TextFilterList.entries(initial)));
    }

    public void init(Minecraft minecraft, int width, int height)
    {
        this.mc = minecraft;
        this.fontRenderer = minecraft.fontRenderer;
        this.width = width;
        this.height = height;
        initGui();
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        int left = (width - SIZE) / 2;
        int top = (height - SIZE) / 2;
        String previous = input == null ? "" : input.getText();
        if (input == null) input = new GuiTextField(100, fontRenderer, left + 10, top + 173, 174, 18);
        input.x = left + 10;
        input.y = top + 173;
        input.setMaxStringLength(64);
        input.setText(previous);
        input.setFocused(true);
        page = Math.max(0, Math.min(page, Math.max(0, (entries.size() - 1) / ROWS)));
        for (int row = 0; row < ROWS && page * ROWS + row < entries.size(); row++)
            addButton(new GuiDefaultButton(110 + row, left + 220, top + 35 + row * 20, 18, 18, BUTTON_TEXTURE, 18, 182, 0, 18, ""));
        addButton(new GuiButton(100, left + 190, top + 172, 50, 20, text("add")));
        addButton(new GuiButton(101, left + 10, top + 218, 110, 20, I18n.format("gui.done")));
        addButton(new GuiButton(102, left + 130, top + 218, 110, 20, I18n.format("gui.cancel")));
        addButton(new GuiDefaultButton(103, left + 10, top + 10, 18, 18, BUTTON_TEXTURE, 90, 212, 0, 18, "")).enabled = page > 0;
        addButton(new GuiDefaultButton(104, left + 220, top + 10, 18, 18, BUTTON_TEXTURE, 108, 212, 0, 18, "")).enabled = (page + 1) * ROWS < entries.size();
    }

    public GuiTextField getInput()
    {
        return input;
    }

    private String text(String key)
    {
        return I18n.format("gui.backpack.text_filters." + key);
    }

    private boolean addEntry()
    {
        String entry = input.getText().trim();
        if (entry.isEmpty()) return true;
        String normalized = TextFilterList.normalize(String.join(", ", entries) + (entries.isEmpty() ? "" : ", ") + entry, type);
        invalid = normalized == null;
        if (invalid) return false;
        entries.clear();
        entries.addAll(Arrays.asList(TextFilterList.entries(normalized)));
        input.setText("");
        page = Math.max(0, (entries.size() - 1) / ROWS);
        initGui();
        return true;
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        switch (button.id)
        {
            case 100 -> addEntry();
            case 101 -> {
                if (!addEntry()) return;
                save.accept(String.join(", ", entries));
                close.run();
            }
            case 102 -> close.run();
            case 103 -> { page--; initGui(); }
            case 104 -> { page++; initGui(); }
            default -> {
                int index = page * ROWS + button.id - 110;
                if (button.id < 110 || button.id >= 110 + ROWS || index >= entries.size()) return;
                entries.remove(index);
                initGui();
            }
        }
    }

    @Override
    public void keyTyped(char character, int key) throws IOException
    {
        if (key == 1)
        {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            close.run();
        }
        else if (key == 28 || key == 156) addEntry();
        else input.textboxKeyTyped(character, key);
    }

    @Override
    public void mouseClicked(int x, int y, int button) throws IOException
    {
        input.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    public void mouseReleased(int x, int y, int button)
    {
        super.mouseReleased(x, y, button);
    }

    @Override
    public void updateScreen()
    {
        input.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial)
    {
        int left = (width - SIZE) / 2;
        int top = (height - SIZE) / 2;
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(texture);
        drawTexturedModalRect(left, top, 0, 0, SIZE, SIZE);
        String title = text(switch (type) { case ORE -> "OreDict"; case MOD -> "ModID"; case FLUID -> "Fluid"; });
        fontRenderer.drawString(title, (width - fontRenderer.getStringWidth(title)) / 2, top + 15, 0x404040);
        String hovered = null;
        for (int row = 0; row < ROWS && page * ROWS + row < entries.size(); row++)
        {
            String entry = entries.get(page * ROWS + row);
            int rowTop = top + 35 + row * 20;
            boolean rowHovered = mouseX >= left + 8 && mouseX < left + 216 && mouseY >= rowTop && mouseY < rowTop + 18;
            drawRect(left + 8, rowTop, left + 216, rowTop + 18, rowHovered ? 0xFFBABABA : 0xFFB2B2B2);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(entry, 200), left + 12, rowTop + 5, 0x404040);
            if (rowHovered) hovered = entry;
        }
        String hint = invalid ? "invalid" : switch (type) { case ORE -> "ore_hint"; case MOD -> "mod_hint"; case FLUID -> "fluid_hint"; };
        fontRenderer.drawSplitString(text(hint), left + 10, top + 195, 228, invalid ? 0xAA0000 : 0x404040);
        GlStateManager.color(1, 1, 1, 1);
        input.drawTextBox();
        super.drawScreen(mouseX, mouseY, partial);
        if (hovered != null) GuiUtils.drawHoveringText(Collections.singletonList(hovered), mouseX, mouseY, width, height, -1, fontRenderer);
        GlStateManager.enableDepth();
    }
}