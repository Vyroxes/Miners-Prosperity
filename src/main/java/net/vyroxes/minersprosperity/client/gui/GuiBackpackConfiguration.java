package net.vyroxes.minersprosperity.client.gui;

import baubles.api.BaublesApi;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.vyroxes.minersprosperity.item.backpack.BackpackSettings;
import net.vyroxes.minersprosperity.util.TextFilterList;
import net.vyroxes.minersprosperity.network.message.MessageBackpackTextFilter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.inventory.container.ContainerBackpackConfiguration;
import net.vyroxes.minersprosperity.item.backpack.ItemBackpackBase;
import net.vyroxes.minersprosperity.gui.GuiHandler;
import net.vyroxes.minersprosperity.network.NetworkHandler;
import java.io.IOException;
import java.util.List;

public class GuiBackpackConfiguration extends GuiContainer
{
    private static final ResourceLocation GUI_ELEMENTS = new ResourceLocation(Tags.MODID, "textures/gui/gui_elements.png");
    private static final ResourceLocation FILTER_EDITOR_TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/backpack_configuration.png");
    private final ItemStack backpackItemStack;
    private final ResourceLocation texture;
    private final ResourceLocation texture2;
    private final ContainerBackpackConfiguration container;
    private final int backpackRows;
    private final int backpackColumns;
    private final boolean fromBaubles;
    private GuiTextFilterEditor filterEditor;
    // JEI discovers focused text fields directly on the container screen.
    private GuiTextField keyboardField;

    private static final String[] KEYS = {"Collect", "WhitelistOrBlacklist", "Metadata", "NBT", "Damage", "OreDict", "ModID", "LockEdit", "Deposit", "Compression", "Void", "CompressionKeep"};
    private static final int[] ICON_X = {0, 72, 108, 144, 180, 216, 106};

    private String valueLabel(String key)
    {
        return I18n.format("gui.backpack.value." + key + "." + container.getValue(key));
    }

    private String buttonTitle(String key)
    {
        return I18n.format("gui.backpack.tooltip.setting", I18n.format("gui.backpack.option." + key), valueLabel(key));
    }

    public GuiBackpackConfiguration(ContainerBackpackConfiguration container, ItemStack backpackItemStack, boolean fromBaubles, ResourceLocation texture, ResourceLocation texture2)
    {
        super(container);
        this.container = container;
        this.backpackItemStack = backpackItemStack;
        this.texture = texture;
        this.texture2 = texture2;
        this.backpackRows = container.getRows();
        this.backpackColumns = container.getColumns();

        int slotSize = 18;
        this.xSize = 8 + backpackColumns * slotSize + 8;
        this.ySize = 137 + container.getContentHeight();
        this.fromBaubles = fromBaubles;
    }

    @Override
    public void initGui() {
        this.xSize = isEditingFilter() ? GuiTextFilterEditor.SIZE : 16 + backpackColumns * 18;
        this.ySize = isEditingFilter() ? GuiTextFilterEditor.SIZE : 137 + container.getContentHeight();
        super.initGui();

        int guiLeft = (this.width - this.xSize) / 2;
        int guiRight = guiLeft + this.xSize;
        int guiTop = (this.height - this.ySize) / 2;

        this.buttonList.clear();
        keyboardField = null;
        if (isEditingFilter())
        {
            filterEditor.init(mc, width, height);
            keyboardField = filterEditor.getInput();
            return;
        }

        int columns = container.getPanelColumns();
        int panelWidth = columns * 20 - 2;
        int panelHeight = (12 / columns) * 20 - 2;
        int panelTop = guiTop + 38 + (container.getContentHeight() - panelHeight) / 2;
        for (int i = 0; i < KEYS.length; i++)
        {
            String key = KEYS[i];
            String tooltip = I18n.format("gui.backpack.tooltip.title", buttonTitle(key));
            String secondary = secondarySetting(key);
            if (secondary != null)
                tooltip += "\n" + I18n.format("gui.backpack.tooltip.title", buttonTitle(secondary));
            String help = I18n.format("gui.backpack.help." + key);
            if (!help.isEmpty()) tooltip += "\n" + I18n.format("gui.backpack.tooltip.help", help);
            if (key.equals("OreDict") || key.equals("ModID"))
            {
                int count = BackpackSettings.data(backpackItemStack).getTagList(key.equals("OreDict") ? "OreFilters" : "ModFilters", 8).tagCount();
                tooltip += "\n" + I18n.format("gui.backpack.text_filters.edit", count);
            }
            int state = container.getValue(key);
            int iconX = i < ICON_X.length ? ICON_X[i] + (state == 0 ? 18 : 0) : 0;
            int iconY = i < 6 ? 104 : 68;
            if (i == 0) iconX = state == 0 ? 18 : state == 1 ? 36 : 54;
            iconY = switch (key) {
                case "LockEdit" -> {
                    iconX = state==0 ? 106:88;
                    yield 140;
                }
                case "Deposit" -> {
                    iconX = state==0 ? 124:state==1 ? 142:160;
                    yield 140;
                }
                case "Compression" -> {
                    iconX = state==0 ? 178:state==1 ? 52:state==2 ? 70:88;
                    yield state==0 ? 140:state==1 || state==2 ? 178:176;
                }
                case "Void" -> {
                    iconX = state==0 ? 106:state==1 ? 124:142;
                    yield 176;
                }
                case "CompressionKeep" -> {
                    iconX = state==0 ? 160:state==1 ? 178:196;
                    yield 176;
                }
                default -> iconY;
            };
            this.addButton(new GuiDefaultButton(i, guiRight - 9 - panelWidth + i % columns * 20,
                    panelTop + i / columns * 20, 18, 18, GUI_ELEMENTS,
                    iconX, iconY, 0, 18, tooltip));
        }

        this.addButton(new GuiDefaultButton(99, guiLeft + 3, guiTop + 1, 26, 21, new ResourceLocation(Tags.MODID, GUI_ELEMENTS.getPath()), 0, 140, 0,21, I18n.format("gui.backpack.name")));
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        if (isEditingFilter())
        {
            filterEditor.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiButton button : this.buttonList)
        {
            if (button instanceof GuiDefaultButton)
            {
                List<String> guiButtonTooltip = ((GuiDefaultButton) button).getCurrentTooltip();
                if (guiButtonTooltip != null) GuiUtils.drawHoveringText(guiButtonTooltip, mouseX, mouseY, this.width, this.height, this.width/2, this.fontRenderer);
            }
        }

        this.renderHoveredToolTip(mouseX, mouseY);
    }

    private String secondarySetting(String key)
    {
        return switch (key) {
            case "Deposit" -> "DepositKeep";
            case "Compression" -> "Reversible";
            case "Void" -> "VoidKeep";
            default -> null;
        };
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (isEditingFilter())
        {
            filterEditor.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (mouseButton >= 0 && mouseButton <= 2)
        {
            for (GuiButton button : buttonList)
            {
                if (button.id < 0 || button.id >= KEYS.length || !button.mousePressed(mc, mouseX, mouseY)) continue;
                String key = KEYS[button.id];
                if (mouseButton == 1 && (key.equals("OreDict") || key.equals("ModID")))
                {
                    boolean ore = key.equals("OreDict");
                    filterEditor = new GuiTextFilterEditor(FILTER_EDITOR_TEXTURE, ore ? TextFilterList.Type.ORE : TextFilterList.Type.MOD,
                            container.getFilterText(ore), text ->
                            {
                                if (container.setFilterText(ore, text))
                                    NetworkHandler.NETWORK.sendToServer(new MessageBackpackTextFilter(container.windowId, ore, text));
                            }, this::closeFilterEditor);
                    button.playPressSound(mc.getSoundHandler());
                    setWorldAndResolution(mc, width, height);
                    return;
                }
                String secondary = secondarySetting(key);
                if (isShiftKeyDown() && secondary != null) key = secondary;
                container.changeSetting(key, mouseButton == 2 ? 0 : mouseButton == 1 ? -1 : 1);
                button.playPressSound(mc.getSoundHandler());
                initGui();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton guiButton) throws IOException {
        if (guiButton.id == 99)
        {
            if (fromBaubles)
            {
                BlockPos playerPos = Minecraft.getMinecraft().player.getPosition();
                NetworkHandler.sendOpenGuiUpdate(GuiHandler.GuiTypes.BACKPACK.ordinal(), new BlockPos(playerPos.getX(), 1, playerPos.getZ()));
            }
            else NetworkHandler.sendOpenGuiUpdate(GuiHandler.GuiTypes.BACKPACK.ordinal(), new BlockPos(Minecraft.getMinecraft().player.getPosition().getX(), 0, Minecraft.getMinecraft().player.getPosition().getZ()));
        }
        this.initGui();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        String title = this.mc.player.getHeldItemMainhand().getDisplayName();
        if (fromBaubles && Loader.isModLoaded("baubles"))
        {
            if (BaublesApi.getBaublesHandler(mc.player) != null)
            {
                ItemStack bauble = BaublesApi.getBaublesHandler(mc.player).getStackInSlot(5);
                if (!bauble.isEmpty() && bauble.getItem() instanceof ItemBackpackBase) title = bauble.getDisplayName();
            }
        }

        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(title, xSize - 20 - container.getPanelColumns() * 18), 8, 28, 4210752);
        int inventoryLabelY = 42 + container.getContentHeight();
        int slotSize = 18;
        int textOffsetX = Math.max(0, (backpackColumns - 9) / 2) * slotSize;
        this.fontRenderer.drawString(I18n.format("key.categories.inventory"), 8 + textOffsetX, inventoryLabelY, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        if (this.backpackColumns > 13)
        {
            int width = (this.backpackColumns - 13) * 18 + 7;
            this.drawTexturedModalRect(this.guiLeft, this.guiTop - 1, 0, 0, 248, this.ySize);
            this.mc.getTextureManager().bindTexture(texture2);
            this.drawTexturedModalRect(this.guiLeft + 241, this.guiTop + 22, 0, 0, width, 221);
        }
        else this.drawTexturedModalRect(this.guiLeft, this.guiTop - 1, 0, 0, this.xSize, this.ySize);
        drawRect(guiLeft + 7, guiTop + 39, guiLeft + xSize - 7, guiTop + 39 + container.getContentHeight(), 0xFFC6C6C6);
        this.mc.getTextureManager().bindTexture(((ItemBackpackBase) backpackItemStack.getItem()).getTexture());
        for (net.minecraft.inventory.Slot slot : container.inventorySlots)
        {
            if (!(slot instanceof net.vyroxes.minersprosperity.inventory.slot.SlotFilter) || slot.xPos < 0) continue;
            drawTexturedModalRect(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1, 7, 40, 18, 18);
        }

    }

    public boolean isEditingFilter()
    {
        return filterEditor != null;
    }

    private void closeFilterEditor()
    {
        filterEditor = null;
        setWorldAndResolution(mc, width, height);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (isEditingFilter()) filterEditor.updateScreen();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed)
    {
        if (!isEditingFilter()) super.mouseClickMove(mouseX, mouseY, button, elapsed);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (isEditingFilter()) filterEditor.mouseReleased(mouseX, mouseY, state);
        else super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public Slot getSlotUnderMouse()
    {
        return isEditingFilter() ? null : super.getSlotUnderMouse();
    }

    @Override
    protected boolean isPointInRegion(int left, int top, int regionWidth, int regionHeight, int mouseX, int mouseY)
    {
        return !isEditingFilter() && super.isPointInRegion(left, top, regionWidth, regionHeight, mouseX, mouseY);
    }

    @Override
    protected boolean checkHotbarKeys(int keyCode)
    {
        return true;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isEditingFilter())
        {
            filterEditor.keyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);

        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode() || keyCode == 1)
        {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
        }
    }
}