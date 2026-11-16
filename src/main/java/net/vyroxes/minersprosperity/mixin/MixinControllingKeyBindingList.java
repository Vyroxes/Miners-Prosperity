package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraftforge.fml.common.Loader;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.client.event.ControllingHoverHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "us.getfluxed.controlsearch.client.gui.GuiNewKeyBindingList", remap = false)
public abstract class MixinControllingKeyBindingList extends GuiKeyBindingList
{
    private MixinControllingKeyBindingList(GuiControls controls, Minecraft minecraft)
    {
        super(controls, minecraft);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void centerList(CallbackInfo ci)
    {
        if (!Loader.isModLoaded("controlling") || !ModConfig.INTEGRATION.controllingLayoutFix) return;
        this.width -= 45;
        this.right = this.width;
    }

    @Inject(method = {"getListWidth", "func_148139_c"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void limitListWidth(CallbackInfoReturnable<Integer> cir)
    {
        if (!Loader.isModLoaded("controlling") || !ModConfig.INTEGRATION.controllingLayoutFix) return;
        cir.setReturnValue(Math.min(400, Minecraft.getMinecraft().currentScreen.width - 40));
    }

    @Inject(method = {"drawSelectionBox", "func_192638_a"}, at = @At("HEAD"), remap = false)
    private void captureTooltipBounds(CallbackInfo ci)
    {
        ControllingHoverHandler.setBounds(this.left, this.top, this.right, this.bottom);
    }
}