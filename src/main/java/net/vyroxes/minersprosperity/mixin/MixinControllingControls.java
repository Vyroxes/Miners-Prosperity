package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.vyroxes.minersprosperity.client.event.ControllingHoverHandler;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.client.renderer.GlStateManager;
import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "us.getfluxed.controlsearch.client.gui.GuiNewControls", remap = false)
public abstract class MixinControllingControls extends GuiScreen
{
    @Shadow public GuiCheckBox boxSearchKey;
    @Shadow public GuiCheckBox boxSearchCategory;
    @Shadow private boolean toggleFreeKeys;
    @Shadow public net.minecraft.client.gui.GuiButton buttonSorting;
    @Shadow public abstract void refreshKeys();
    @Unique private int minersprosperity$sortMode;

    @Inject(method = {"actionPerformed", "func_146284_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cycleSorting(net.minecraft.client.gui.GuiButton button, CallbackInfo ci)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix || button != buttonSorting) return;
        minersprosperity$sortMode = (minersprosperity$sortMode + 1) % 7;
        toggleFreeKeys = false;
        minersprosperity$updateSortLabel();
        refreshKeys();
        ci.cancel();
    }

    @Unique
    private void minersprosperity$updateSortLabel()
    {
        String name = net.minecraft.client.resources.I18n.format("gui.controlling.sort." + minersprosperity$sortMode);
        buttonSorting.displayString = minersprosperity$sortMode < 3
                ? net.minecraft.client.resources.I18n.format("options.sort") + ": " + name : name;
    }

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("RETURN"), remap = false)
    private void restoreSortLabel(CallbackInfo ci)
    {
        if (ModConfig.INTEGRATION.controllingLayoutFix && buttonSorting != null) minersprosperity$updateSortLabel();
    }

    @Inject(method = "sort", at = @At("HEAD"), cancellable = true, remap = false)
    private void sortGroups(java.util.LinkedList<net.minecraft.client.gui.GuiListExtended.IGuiListEntry> entries,
                            @org.spongepowered.asm.mixin.injection.Coerce Object originalMode,
                            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.LinkedList<net.minecraft.client.gui.GuiListExtended.IGuiListEntry>> cir)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix || minersprosperity$sortMode == 0) return;
        java.util.LinkedList<net.minecraft.client.gui.GuiListExtended.IGuiListEntry> keys = new java.util.LinkedList<>();
        java.util.Map<net.minecraft.client.gui.GuiListExtended.IGuiListEntry, String[]> labels = new java.util.IdentityHashMap<>();
        for (net.minecraft.client.gui.GuiListExtended.IGuiListEntry entry : entries)
        {
            if (!(entry instanceof net.vyroxes.minersprosperity.client.event.ControllingKeyEntry)) continue;
            net.minecraft.client.settings.KeyBinding key = ((net.vyroxes.minersprosperity.client.event.ControllingKeyEntry) entry).minersprosperity$keyBinding();
            String category = net.minecraft.client.resources.I18n.format(key.getKeyCategory());
            String group = minersprosperity$sortMode >= 5
                    ? net.vyroxes.minersprosperity.client.event.KeyBindingModNames.name(key) : category;
            labels.put(entry, new String[] {group, category, net.minecraft.client.resources.I18n.format(key.getKeyDescription()), key.getKeyDescription()});
            keys.add(entry);
        }
        keys.sort((a, b) -> {
            String[] first = labels.get(a), second = labels.get(b);
            int result = minersprosperity$sortMode >= 3 ? first[0].compareToIgnoreCase(second[0]) : 0;
            if (minersprosperity$sortMode == 4 || minersprosperity$sortMode == 6) result = -result;
            if (result == 0 && minersprosperity$sortMode >= 5) result = first[1].compareToIgnoreCase(second[1]);
            if (result == 0)
            {
                result = first[2].compareToIgnoreCase(second[2]);
                if (minersprosperity$sortMode == 2) result = -result;
            }
            return result != 0 ? result : first[3].compareTo(second[3]);
        });
        cir.setReturnValue(keys);
    }
    @Unique private final List<String> minersprosperity$freeKeys = new ArrayList<>();
    @Unique private final List<Integer> minersprosperity$keyColors = new ArrayList<>();
    @Unique private int minersprosperity$freeTop;

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("HEAD"), remap = false)
    private void beginLayout(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        minersprosperity$freeKeys.clear();
        minersprosperity$keyColors.clear();
    }

    @Group(name = "minersprosperity$searchLabel", min = 1, max = 2)
    @ModifyArgs(method = {"drawScreen", "func_73863_a"}, at = @At(value = "INVOKE",
            target = "Lus/getfluxed/controlsearch/client/gui/GuiNewControls;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 1, remap = false), remap = false, require = 0)
    private void alignSearchLabelDevelopment(Args args)
    {
        minersprosperity$alignSearchLabel(args);
    }

    @Group(name = "minersprosperity$searchLabel", min = 1, max = 2)
    @ModifyArgs(method = {"drawScreen", "func_73863_a"}, at = @At(value = "INVOKE",
            target = "Lus/getfluxed/controlsearch/client/gui/GuiNewControls;func_73732_a(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 1, remap = false), remap = false, require = 0)
    private void alignSearchLabelProduction(Args args)
    {
        minersprosperity$alignSearchLabel(args);
    }

    @Unique
    private void minersprosperity$alignSearchLabel(Args args)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix) return;
        FontRenderer font = args.get(0);
        String text = args.get(1);
        int left = Math.min(boxSearchKey.x, boxSearchCategory.x);
        int top = Math.min(boxSearchKey.y, boxSearchCategory.y);
        int bottom = Math.max(boxSearchKey.y + boxSearchKey.height, boxSearchCategory.y + boxSearchCategory.height);
        args.set(2, left - 6 - (font.getStringWidth(text) + 1) / 2);
        args.set(3, top + (bottom - top - font.FONT_HEIGHT) / 2);
    }

    @Redirect(method = {"drawScreen", "func_73863_a"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true), remap = false)
    private int centerAvailableTitle(FontRenderer font, String text, int x, int y, int color)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix) return font.drawString(text, x, y, color);
        minersprosperity$freeTop = y + font.FONT_HEIGHT + 4;
        return font.drawString(text, (width - font.getStringWidth(text)) / 2, y, color);
    }

    @Redirect(method = "lambda$drawScreen$3", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true), remap = false)
    private int collectAvailableKey(FontRenderer font, String text, int x, int y, int color)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix) return font.drawString(text, x, y, color);
        minersprosperity$freeKeys.add(text);
        minersprosperity$keyColors.add(color);
        return 0;
    }

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("RETURN"), remap = false)
    private void drawAvailableKeys(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        if (!ModConfig.INTEGRATION.controllingLayoutFix || !toggleFreeKeys) return;
        ControllingHoverHandler.show(null, null);
        int count = minersprosperity$freeKeys.size();
        if (count == 0) return;
        int bottom = Math.min(boxSearchKey.y, boxSearchCategory.y) - 6;
        int availableHeight = Math.max(1, bottom - minersprosperity$freeTop);
        int cellWidth = 1;
        for (String key : minersprosperity$freeKeys) cellWidth = Math.max(cellWidth, fontRenderer.getStringWidth(key) + 10);
        int rows = Math.max(1, availableHeight / (fontRenderer.FONT_HEIGHT + 2));
        int columns = (count + rows - 1) / rows;
        float scale = Math.min(1F, (width - 24F) / (columns * cellWidth));
        GlStateManager.pushMatrix();
        try
        {
            GlStateManager.translate((width - columns * cellWidth * scale) / 2F, minersprosperity$freeTop, 0);
            GlStateManager.scale(scale, scale, 1);
            for (int i = 0; i < count; i++)
                fontRenderer.drawString(minersprosperity$freeKeys.get(i), (i / rows) * cellWidth,
                        (i % rows) * (fontRenderer.FONT_HEIGHT + 2), minersprosperity$keyColors.get(i));
        }
        finally { GlStateManager.popMatrix(); }
    }
}