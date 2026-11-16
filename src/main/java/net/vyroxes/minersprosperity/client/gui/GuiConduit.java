package net.vyroxes.minersprosperity.client.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.inventory.container.ContainerConduit;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import net.vyroxes.minersprosperity.network.message.MessageConduitSettings;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import net.vyroxes.minersprosperity.transport.ConduitPort;
import net.vyroxes.minersprosperity.transport.ConduitType;
import net.vyroxes.minersprosperity.util.TextFilterList;
import org.jetbrains.annotations.NotNull;

public class GuiConduit extends GuiContainer
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/cable_configuration.png");
    private static final ResourceLocation FILTER_TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/cable_configuration_3.png");
    private static final ResourceLocation EDITOR_TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/cable_configuration_2.png");
    private static final ResourceLocation GUI_ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private final TileEntityConduit tile;
    private final String conduitName;
    private final int side;
    private int resource;
    private int page;
    private int limitsPage;
    private static final int LIMITS_PER_PAGE = 3;
    private static final String[] LIMIT_SETTINGS = {"channel", "priority", "interval", "limit", "batch", "keep", "source_slots", "destination_slots"};
    private ConduitPort edited;
    private final List<GuiTextField> fields = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final ConduitPort[] ports = new ConduitPort[4];
    private final NBTTagCompound[] saved = new NBTTagCompound[4];
    private final boolean[] changed = new boolean[4];
    private GuiTextFilterEditor filterEditor;
    // JEI discovers focused text fields directly on the container screen.
    private GuiTextField keyboardField;
    private int saveDelay;
    private int revision;

    public GuiConduit(InventoryPlayer inventory, TileEntityConduit tile, int side)
    {
        super(new ContainerConduit(inventory, tile, side));
        this.tile = tile;
        this.conduitName = new ItemStack(tile.conduit()).getDisplayName();
        this.side = side;
        this.revision = tile.settingsRevision;
        this.resource = tile.getInterfaceResource(side);
        xSize = GuiTextFilterEditor.SIZE;
        ySize = GuiTextFilterEditor.SIZE;
    }

    private String text(String key)
    {
        return I18n.format("gui.conduit." + key);
    }

    @Override
    public void initGui()
    {
        if (edited != null && filterEditor == null) save();
        super.initGui();
        selectResource();
        if (filterEditor != null)
        {
            filterEditor.init(mc, width, height);
            keyboardField = filterEditor.getInput();
        }
        else layout();
    }

    private void layout()
    {
        buttonList.clear();
        fields.clear();
        labels.clear();
        keyboardField = null;
        boolean universal = tile.conduit().type == ConduitType.UNIVERSAL;
        int buttonWidth = universal ? 56 : 76;
        int column = 0;
        if (universal)
            addButton(new GuiButton(0, guiLeft + 8 + column++ * (buttonWidth + 2), guiTop + 21, buttonWidth, 20, text("resource." + resource)));
        for (int tab = 0; tab < 3; tab++)
        {
            GuiButton button = addButton(new GuiButton(tab + 1, guiLeft + 8 + column++ * (buttonWidth + 2), guiTop + 21,
                    buttonWidth, 20, fontRenderer.trimStringToWidth(text("page." + tab), buttonWidth - 8)));
            button.enabled = tab != page && (tab != 2 || resource == 1 || resource == 2);
        }
        if (page == 0)
        {
            addButton(new GuiCheckBox(10, guiLeft + 8, guiTop + 54, "", (edited.mode & 1) != 0));
            addButton(new GuiCheckBox(15, guiLeft + 128, guiTop + 54, "", (edited.mode & 2) != 0));
            behaviorButton(11, "redstone." + edited.redstone, 2);
            behaviorButton(12, "extraction." + edited.extraction, 3);
            behaviorButton(13, "distribution." + edited.distribution, 4);
            behaviorButton(14, "insertion." + edited.insertion, 5);
        }
        else if (page == 1)
        {
            addButton(new GuiDefaultButton(4, guiLeft + 8, guiTop + 44, 18, 18, GUI_ELEMENTS, 90, 212, 0, 18, "")).enabled = limitsPage > 0;
            addButton(new GuiDefaultButton(5, guiLeft + xSize - 26, guiTop + 44, 18, 18, GUI_ELEMENTS, 108, 212, 0, 18, "")).enabled = (limitsPage + 1) * LIMITS_PER_PAGE < LIMIT_SETTINGS.length;
            int first = limitsPage * LIMITS_PER_PAGE;
            for (int i = first; i < Math.min(first + LIMITS_PER_PAGE, LIMIT_SETTINGS.length); i++)
            {
                String key = LIMIT_SETTINGS[i];
                String value = switch (key)
                {
                    case "channel" -> "" + edited.channel;
                    case "priority" -> "" + edited.priority;
                    case "interval" -> "" + edited.interval;
                    case "limit" -> "" + edited.limit;
                    case "batch" -> "" + edited.batch;
                    case "keep" -> "" + edited.keep;
                    case "source_slots" -> edited.getSourceSlots();
                    case "destination_slots" -> edited.getDestinationSlots();
                    default -> "";
                };
                field(key, value, i - first);
            }
        }
        else
        {
            int[] icons = {72, 108, 144, 180, 216, 106};
            boolean[] values = {!edited.blacklist, edited.metadata, edited.nbt, edited.damage, edited.useOreDict, edited.useModID};
            for (int i = 0; i < icons.length; i++)
            {
                int iconX = icons[i] + (values[i] ? 0 : 18);
                if (i == 0) iconX = edited.blacklist ? 90 : 72;
                addButton(new GuiDefaultButton(20 + i, guiLeft + 202 + i % 2 * 20, guiTop + 50 + i / 2 * 20,
                        18, 18, GUI_ELEMENTS, iconX, i == 5 ? 68 : 104, 0, 18, ""));
            }
            addButton(new GuiDefaultButton(26, guiLeft + 202, guiTop + 110, 18, 18,
                    GUI_ELEMENTS, edited.useFluid ? 54 : 72, 214, 0, 18, ""));
        }
        for (GuiButton button : buttonList)
        {
            if (button instanceof GuiCheckBox) button.width = 16 + fontRenderer.getStringWidth(text(optionKey(button.id)));
            if (button.id >= 10) button.enabled = optionEnabled(optionKey(button.id));
        }
    }

    private void field(String label, String value, int index)
    {
        GuiTextField field = new GuiTextField(index, fontRenderer, guiLeft + 8, guiTop + 75 + index * 29, xSize - 16, 16);
        field.setMaxStringLength(label.endsWith("_slots") ? 512 : 10);
        field.setText(value);
        field.setEnabled(optionEnabled(label));
        fields.add(field);
        labels.add(label);
    }

    private void behaviorButton(int id, String label, int index)
    {
        addButton(new GuiButton(id, guiLeft + 8 + index % 2 * 120, guiTop + 50 + index / 2 * 29, 112, 20,
                fontRenderer.trimStringToWidth(text(label), 104)));
    }

    private boolean optionEnabled(String key)
    {
        boolean insert = (edited.mode & 1) != 0;
        boolean extract = (edited.mode & 2) != 0;
        return switch (key)
        {
            case "insert", "extract" -> true;
            case "priority" -> insert && resource != 3;
            case "interval" -> extract;
            case "batch", "extraction", "source_slots" -> extract && resource == 1;
            case "keep", "insertion", "destination_slots" -> insert && resource == 1;
            case "distribution" -> extract && resource != 3;
            case "limit" -> edited.mode != 0 && resource != 3;
            case "ore", "mod", "metadata", "nbt", "damage" -> edited.mode != 0 && resource == 1;
            case "blacklist" -> edited.mode != 0 && (resource == 1 || resource == 2);
            case "fluid" -> edited.mode != 0 && resource == 2;
            default -> edited.mode != 0;
        };
    }

    private String optionKey(int id)
    {
        return switch (id)
        {
            case 0 -> "resource";
            case 1, 2, 3 -> "page";
            case 4, 5 -> "limits_page";
            case 10 -> "insert";
            case 15 -> "extract";
            case 11 -> "redstone";
            case 12 -> "extraction";
            case 13 -> "distribution";
            case 14 -> "insertion";
            case 20 -> "blacklist";
            case 21 -> "metadata";
            case 22 -> "nbt";
            case 23 -> "damage";
            case 24 -> "ore";
            case 25 -> "mod";
            case 26 -> "fluid";
            default -> "";
        };
    }

    private int number(int index, int fallback)
    {
        try { return Integer.parseInt(fields.get(index).getText()); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private void collect()
    {
        if (filterEditor != null || page != 1) return;
        for (int i = 0; i < fields.size(); i++)
        {
            switch (labels.get(i))
            {
                case "channel" -> edited.channel = number(i, edited.channel);
                case "priority" -> edited.priority = number(i, edited.priority);
                case "interval" -> edited.interval = number(i, edited.interval);
                case "limit" -> edited.limit = number(i, edited.limit);
                case "batch" -> edited.batch = number(i, edited.batch);
                case "keep" -> edited.keep = number(i, edited.keep);
                case "source_slots" -> fields.get(i).setTextColor(edited.setSourceSlots(fields.get(i).getText()) ? 0xE0E0E0 : 0xFF5555);
                case "destination_slots" -> fields.get(i).setTextColor(edited.setDestinationSlots(fields.get(i).getText()) ? 0xE0E0E0 : 0xFF5555);
            }
        }
    }

    private void selectResource()
    {
        if (ports[resource] == null)
        {
            ports[resource] = new ConduitPort();
            saved[resource] = tile.ports[side][resource].write(false);
            ports[resource].read(saved[resource], false);
        }
        edited = ports[resource];
    }

    private void save()
    {
        collect();
        saveDelay = 0;
        edited.read(edited.write(false), false);
        NBTTagCompound settings = edited.write(false);
        if (settings.equals(saved[resource])) return;
        saved[resource] = settings;
        changed[resource] = true;
        NetworkHandler.NETWORK.sendToServer(new MessageConduitSettings(inventorySlots.windowId, resource, -1, settings));
    }

    private void changeButton(GuiButton button, int direction)
    {
        save();
        boolean reset = direction == 0;
        if (button.id == 0)
        {
            if (reset) resource = tile.conduit().type.ordinal() % 4;
            else do { resource = Math.floorMod(resource + direction, 4); } while (!tile.conduit().type.supports(resource));
            selectResource();
            if (page == 2 && resource != 1 && resource != 2) page = 0;
        }
        else if (button.id >= 1 && button.id <= 3) page = button.id - 1;
        else if (button.id == 4 || button.id == 5)
            limitsPage = reset ? 0 : limitsPage + (button.id == 4 ? -1 : 1);
        else
        {
            switch (button.id)
            {
                case 10 -> edited.mode = reset ? edited.mode & ~1 : edited.mode ^ 1;
                case 15 -> edited.mode = reset ? edited.mode & ~2 : edited.mode ^ 2;
                case 11 -> edited.redstone = reset ? 0 : Math.floorMod(edited.redstone + direction, 3);
                case 12 -> edited.extraction = reset ? 0 : Math.floorMod(edited.extraction + direction, 4);
                case 13 -> {
                    int[] order = {0, 1, 2, 5, 3, 4};
                    int index = 0;
                    for (int i = 0; i < order.length; i++) if (order[i] == edited.distribution) { index = i; break; }
                    edited.distribution = reset ? 0 : order[Math.floorMod(index + direction, order.length)];
                }
                case 14 -> edited.insertion = reset ? 0 : Math.floorMod(edited.insertion + direction, 5);
                case 20 -> edited.blacklist = !reset && !edited.blacklist;
                case 21 -> edited.metadata = reset || !edited.metadata;
                case 22 -> edited.nbt = !reset && !edited.nbt;
                case 23 -> edited.damage = !reset && !edited.damage;
                case 24 -> edited.useOreDict = !reset && !edited.useOreDict;
                case 25 -> edited.useModID = !reset && !edited.useModID;
                case 26 -> edited.useFluid = reset || !edited.useFluid;
            }
            save();
        }
        layout();
    }

    @Override
    protected void actionPerformed(@NotNull GuiButton button)
    {
        changeButton(button, 1);
    }

    private void openFilterEditor(TextFilterList.Type type)
    {
        save();
        filterEditor = new GuiTextFilterEditor(EDITOR_TEXTURE, type, edited.getTextFilter(type), value ->
        {
            if (edited.setTextFilter(type, value)) save();
        }, () ->
        {
            filterEditor = null;
            keyboardField = null;
            fields.clear();
            setWorldAndResolution(mc, width, height);
        });
        setWorldAndResolution(mc, width, height);
    }

    public boolean canEditFilters()
    {
        return filterEditor == null && page == 2 && (resource == 1 || resource == 2) && edited.mode != 0;
    }

    public Rectangle getFilterArea(int index)
    {
        return new Rectangle(guiLeft + 9 + index % ConduitPort.FILTER_COLUMNS * 18,
                guiTop + 51 + index / ConduitPort.FILTER_COLUMNS * 18, 16, 16);
    }

    public void setGhostFilter(int index, ItemStack stack)
    {
        if (!canEditFilters() || index < 0 || index >= edited.filters.length) return;
        if (resource == 2 && !stack.isEmpty() && net.minecraftforge.fluids.FluidUtil.getFluidContained(stack) == null) return;
        save();
        edited.setFilter(index, stack);
        changed[resource] = true;
        NBTTagCompound ghost = new NBTTagCompound();
        ghost.setTag("GhostFilter", edited.filters[index].writeToNBT(new NBTTagCompound()));
        NetworkHandler.NETWORK.sendToServer(new MessageConduitSettings(inventorySlots.windowId, resource, index, ghost));
        saved[resource] = edited.write(false);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (filterEditor != null)
        {
            filterEditor.updateScreen();
            return;
        }
        if (revision != tile.settingsRevision)
        {
            revision = tile.settingsRevision;
            NBTTagCompound settings = tile.ports[side][resource].write(false);
            if (settings.equals(saved[resource])) changed[resource] = false;
            else if (saveDelay == 0 && !changed[resource])
            {
                saved[resource] = settings;
                edited.read(settings, false);
                layout();
            }
        }
        if (saveDelay > 0 && --saveDelay == 0) save();
        for (GuiTextField field : fields) field.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException
    {
        if (filterEditor != null) { filterEditor.keyTyped(character, key); return; }
        if (key == 1)
        {
            save();
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            super.keyTyped(character, key);
            return;
        }
        if (key == 28 || key == 156) { save(); return; }
        for (GuiTextField field : fields)
        {
            if (!field.isFocused()) continue;
            String previous = field.getText();
            if (!field.textboxKeyTyped(character, key)) continue;
            if (!previous.equals(field.getText())) saveDelay = 10;
            return;
        }
        super.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException
    {
        if (filterEditor != null) { filterEditor.mouseClicked(x, y, button); return; }
        if (saveDelay > 0) save();
        if (button == 1 || button == 2)
            for (GuiButton control : buttonList)
            {
                if (!control.mousePressed(mc, x, y)) continue;
                control.playPressSound(mc.getSoundHandler());
                if (button == 1 && control.id >= 24 && control.id <= 26)
                    openFilterEditor(TextFilterList.Type.values()[control.id - 24]);
                else changeButton(control, button == 2 ? 0 : -1);
                return;
            }
        keyboardField = null;
        for (GuiTextField field : fields)
        {
            field.mouseClicked(x, y, button);
            if (field.isFocused()) keyboardField = field;
        }
        if (canEditFilters() && (button == 0 || button == 1))
            for (int i = 0; i < edited.filters.length; i++)
                if (getFilterArea(i).contains(x, y))
                {
                    setGhostFilter(i, mc.player.inventory.getItemStack());
                    return;
                }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseReleased(int x, int y, int button)
    {
        if (filterEditor != null) filterEditor.mouseReleased(x, y, button);
        else super.mouseReleased(x, y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed)
    {
        if (filterEditor == null) super.mouseClickMove(x, y, button, elapsed);
    }

    @Override
    public Slot getSlotUnderMouse()
    {
        return filterEditor == null ? super.getSlotUnderMouse() : null;
    }

    @Override
    protected boolean isPointInRegion(int left, int top, int regionWidth, int regionHeight, int x, int y)
    {
        return filterEditor == null && super.isPointInRegion(left, top, regionWidth, regionHeight, x, y);
    }

    @Override
    public void onGuiClosed()
    {
        save();
        super.onGuiClosed();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY)
    {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(page == 2 ? FILTER_TEXTURE : TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        for (int i = 0; i < fields.size(); i++)
        {
            GuiTextField field = fields.get(i);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(text(labels.get(i)), field.width), field.x, field.y - 10,
                    optionEnabled(labels.get(i)) ? 0x404040 : 0x888888);
            field.drawTextBox();
        }
        if (page == 2)
        {
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < edited.filters.length; i++)
            {
                Rectangle area = getFilterArea(i);
                itemRender.renderItemAndEffectIntoGUI(edited.filters[i], area.x, area.y);
                if (area.contains(mouseX, mouseY))
                {
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    drawRect(area.x, area.y, area.x + 16, area.y + 16, 0x80FFFFFF);
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                }
            }
            RenderHelper.disableStandardItemLighting();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        for (GuiButton button : buttonList)
        {
            if (!(button instanceof GuiCheckBox) || !button.visible) continue;
            fontRenderer.drawString(text(optionKey(button.id)), button.x - guiLeft + 16,
                    button.y - guiTop + (button.height - fontRenderer.FONT_HEIGHT) / 2 + 1, 0x404040);
        }
        String title = conduitName + " - " + text("side." + side);
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 7, 0x404040);
        if (page == 1)
        {
            String indicator = I18n.format("gui.conduit.limits_page", limitsPage + 1,
                    (LIMIT_SETTINGS.length + LIMITS_PER_PAGE - 1) / LIMITS_PER_PAGE);
            fontRenderer.drawString(indicator, (xSize - fontRenderer.getStringWidth(indicator)) / 2, 48, 0x404040);
        }
        fontRenderer.drawString(I18n.format("key.categories.inventory"), 44, 155, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial)
    {
        drawDefaultBackground();
        if (filterEditor != null) { filterEditor.drawScreen(mouseX, mouseY, partial); return; }
        super.drawScreen(mouseX, mouseY, partial);
        renderHoveredToolTip(mouseX, mouseY);
        if (page == 2)
            for (int i = 0; i < edited.filters.length; i++)
                if (getFilterArea(i).contains(mouseX, mouseY) && !edited.filters[i].isEmpty())
                {
                    renderToolTip(edited.filters[i], mouseX, mouseY);
                    return;
                }
        String key = null;
        boolean enabled = true;
        List<String> tooltip = new ArrayList<>();
        for (GuiButton button : buttonList)
        {
            if (!button.visible || mouseX < button.x || mouseY < button.y
                    || mouseX >= button.x + button.width || mouseY >= button.y + button.height) continue;
            key = optionKey(button.id);
            enabled = button.enabled || button.id >= 1 && button.id <= 3 && button.id - 1 == page;
            String value = switch (button.id)
            {
                case 1, 2, 3 -> text("page." + (button.id - 1));
                case 11 -> text("redstone." + edited.redstone);
                case 12 -> text("extraction." + edited.extraction);
                case 13 -> text("distribution." + edited.distribution);
                case 14 -> text("insertion." + edited.insertion);
                case 20 -> text(edited.blacklist ? "blacklist" : "whitelist");
                case 21 -> text("metadata") + ": " + I18n.format("gui.backpack.value.Metadata." + (edited.metadata ? 1 : 0));
                case 22 -> "NBT: " + I18n.format("gui.backpack.value.Metadata." + (edited.nbt ? 1 : 0));
                case 23 -> text("damage") + ": " + I18n.format("gui.backpack.value.Metadata." + (edited.damage ? 1 : 0));
                case 24 -> text("ore") + ": " + I18n.format("gui.backpack.value.Metadata." + (edited.useOreDict ? 1 : 0));
                case 25 -> text("mod") + ": " + I18n.format("gui.backpack.value.Metadata." + (edited.useModID ? 1 : 0));
                case 26 -> text("fluid") + ": " + I18n.format("gui.backpack.value.Metadata." + (edited.useFluid ? 1 : 0));
                default -> "";
            };
            if (!value.isEmpty()) tooltip.add(net.minecraft.util.text.TextFormatting.WHITE + value);
            break;
        }
        if (key == null)
            for (int i = 0; i < fields.size(); i++)
            {
                GuiTextField field = fields.get(i);
                if (mouseX < field.x || mouseY < field.y - 10 || mouseX >= field.x + field.width || mouseY >= field.y + field.height) continue;
                key = labels.get(i);
                enabled = optionEnabled(key);
                tooltip.add(text(key));
                if (key.endsWith("_slots"))
                {
                    int count = ((ContainerConduit) inventorySlots).connectedSlots;
                    tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + (count == 0 ? text("slot_preview_empty") : I18n.format("gui.conduit.slot_preview", count)));
                }
                break;
            }
        if (key == null) return;
        for (String line : fontRenderer.listFormattedStringToWidth(net.minecraft.util.text.TextFormatting.GRAY + text("help." + key).replace("\\n", "\n"), 230))
            tooltip.add(line);
        if (key.equals("ore") || key.equals("mod") || key.equals("fluid"))
        {
            TextFilterList.Type type = key.equals("ore") ? TextFilterList.Type.ORE : key.equals("mod") ? TextFilterList.Type.MOD : TextFilterList.Type.FLUID;
            tooltip.add(I18n.format("gui.backpack.text_filters.edit", TextFilterList.entries(edited.getTextFilter(type)).length));
        }
        if (!enabled) tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + text("help.unavailable"));
        GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, fontRenderer);
    }
}