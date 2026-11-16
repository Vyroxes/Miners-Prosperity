package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.vyroxes.minersprosperity.client.gui.BaublesRecipeBook;
import net.minecraft.inventory.Container;
import net.vyroxes.minersprosperity.client.event.InventoryTweaksButtonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "baubles.client.gui.GuiPlayerExpanded", remap = false)
public abstract class MixinBaublesInventory extends GuiContainer
{
    @Unique private GuiButtonImage minersprosperity$recipeButton;

    protected MixinBaublesInventory(Container container)
    {
        super(container);
    }

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("TAIL"), remap = false)
    private void addRecipeButton(CallbackInfo ci)
    {
        minersprosperity$recipeButton = new GuiButtonImage(10, guiLeft + 115, guiTop + 61,
                20, 18, 178, 0, 19, INVENTORY_BACKGROUND);
        buttonList.add(minersprosperity$recipeButton);
    }

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("HEAD"), remap = false)
    private void alignButtons(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        if (minersprosperity$recipeButton != null)
            minersprosperity$recipeButton.setPosition(guiLeft + 115, guiTop + 61);
        InventoryTweaksButtonHandler.positionButtons(this, buttonList);
        for (GuiButton button : buttonList)
        {
            if (button.id != 54696386) continue;
            button.x += 3;
            button.y += 1;
            break;
        }
    }

    @Inject(method = {"actionPerformed", "func_146284_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void openRecipeBook(GuiButton button, CallbackInfo ci)
    {
        if (button != minersprosperity$recipeButton) return;
        BaublesRecipeBook.open(mc);
        ci.cancel();
    }
}