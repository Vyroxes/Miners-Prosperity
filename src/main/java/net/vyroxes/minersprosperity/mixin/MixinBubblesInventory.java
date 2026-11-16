package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.vyroxes.minersprosperity.client.gui.BaublesRecipeBook;
import net.vyroxes.minersprosperity.client.event.InventoryTweaksButtonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "baubles.client.gui.GuiPlayerExpanded", remap = false)
public abstract class MixinBubblesInventory extends GuiContainer
{
    @Shadow protected GuiButtonImage recipeBook;

    protected MixinBubblesInventory(Container container)
    {
        super(container);
    }

    // Bubbles recenters the GUI after creating its buttons, leaving their original coordinates stale.
    @Inject(method = "resetGuiLeft", at = @At("TAIL"), remap = false)
    private void afterCentering(CallbackInfo ci)
    {
        updateButtonPositions();
    }

    // Inventory Tweaks can add its buttons after initGui, so align them again before drawing.
    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("HEAD"), remap = false)
    private void beforeDrawing(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        updateButtonPositions();
    }

    @Inject(method = {"actionPerformed", "func_146284_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void openRecipeBook(GuiButton button, CallbackInfo ci)
    {
        if (button != recipeBook) return;
        BaublesRecipeBook.open(mc);
        ci.cancel();
    }

    @Unique
    private void updateButtonPositions()
    {
        if (recipeBook != null) recipeBook.setPosition(guiLeft + 104, guiTop + 61);
        InventoryTweaksButtonHandler.positionButtons(this, buttonList);
        for (GuiButton button : buttonList)
        {
            if (button.id != 54696386) continue;
            button.x += 3;
            button.y += 1;
            break;
        }
    }
}