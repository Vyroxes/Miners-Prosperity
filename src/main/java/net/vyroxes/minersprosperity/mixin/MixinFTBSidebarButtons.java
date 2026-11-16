package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.integration.FTBSidebarButtonPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "com.feed_the_beast.ftblib.client.FTBLibClientEventHandler$GuiButtonSidebarGroup", remap = false)
public abstract class MixinFTBSidebarButtons
{
    @Shadow @Final public List<?> buttons;

    @ModifyVariable(method = {"drawButton", "func_191745_a"}, at = @At("STORE"), index = 8, remap = false, argsOnly = true)
    private boolean useTopPlacement(boolean top)
    {
        return ModConfig.INTEGRATION.ftbUtilitiesCustomLayout || top;
    }

    @Inject(method = {"drawButton", "func_191745_a"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/InventoryEffectRenderer;getGuiLeft()I", ordinal = 0), remap = false)
    private void arrangeButtons(Minecraft minecraft, int mouseX, int mouseY, float partial, CallbackInfo ci)
    {
        if (!ModConfig.INTEGRATION.ftbUtilitiesCustomLayout || buttons.isEmpty()) return;
        int limit = ModConfig.INTEGRATION.ftbUtilitiesButtonsPerRow;
        int columns = limit > 0 ? limit : buttons.size();
        for (int i = 0; i < buttons.size(); i++)
            ((FTBSidebarButtonPosition) buttons.get(i)).minersprosperity$setGridPosition(i % columns, i / columns);
    }
}