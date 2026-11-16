package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.client.event.TooltipHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = net.minecraftforge.fml.client.config.GuiUtils.class, remap = false)
public abstract class MixinGuiUtilsTooltip
{
    @ModifyVariable(method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static List<String> finishTooltip(List<String> lines, ItemStack stack, List<String> original,
                                            int mouseX, int mouseY, int width, int height, int maxWidth, FontRenderer font)
    {
        return TooltipHandler.finishTooltip(stack, lines, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
    }
}