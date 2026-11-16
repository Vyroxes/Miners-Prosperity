package net.vyroxes.minersprosperity.mixin;

import invtweaks.api.container.ContainerSection;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "invtweaks.InvTweaksObfuscation", remap = false)
public abstract class MixinInventoryTweaksBubbles
{
    // Baubles and Bubbles use their own container instead of ContainerPlayer, which Inventory Tweaks does not recognize.
    @Inject(method = {"isValidInventory", "showButtons"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void recognizeInventory(Container container, CallbackInfoReturnable<Boolean> cir)
    {
        if (isBubblesContainer(container)) cir.setReturnValue(true);
    }

    @Inject(method = "isGuiInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void recognizeScreen(Object screen, CallbackInfoReturnable<Boolean> cir)
    {
        if (screen != null && screen.getClass().getName().equals("baubles.client.gui.GuiPlayerExpanded")) cir.setReturnValue(true);
    }

    @Inject(method = "getContainerSlotMap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void inventorySlots(Container container, CallbackInfoReturnable<Map<ContainerSection, List<Slot>>> cir)
    {
        if (!isBubblesContainer(container)) return;
        Map<ContainerSection, List<Slot>> sections = new EnumMap<>(ContainerSection.class);
        List<Slot> main = new ArrayList<>(27);
        List<Slot> hotbar = new ArrayList<>(9);
        List<Slot> armor = new ArrayList<>(4);
        List<Slot> crafting = new ArrayList<>(4);
        List<Slot> output = new ArrayList<>(1);
        // Accessory counts can vary; identify vanilla sections by their backing inventory rather than container offsets.
        for (Slot slot : container.inventorySlots)
        {
            if (slot.inventory instanceof InventoryPlayer)
            {
                int index = slot.getSlotIndex();
                if (index >= 9 && index < 36) main.add(slot);
                else if (index >= 0 && index < 9) hotbar.add(slot);
                else if (index >= 36 && index < 40) armor.add(slot);
            }
            else if (slot.inventory instanceof InventoryCrafting) crafting.add(slot);
            else if (slot.inventory instanceof InventoryCraftResult) output.add(slot);
        }
        main.sort(Comparator.comparingInt(Slot::getSlotIndex));
        hotbar.sort(Comparator.comparingInt(Slot::getSlotIndex));
        List<Slot> inventory = new ArrayList<>(main.size() + hotbar.size());
        inventory.addAll(main);
        inventory.addAll(hotbar);
        sections.put(ContainerSection.INVENTORY, inventory);
        sections.put(ContainerSection.INVENTORY_NOT_HOTBAR, main);
        sections.put(ContainerSection.INVENTORY_HOTBAR, hotbar);
        sections.put(ContainerSection.ARMOR, armor);
        sections.put(ContainerSection.CRAFTING_IN, crafting);
        sections.put(ContainerSection.CRAFTING_OUT, output);
        cir.setReturnValue(sections);
    }

    @Unique
    private static boolean isBubblesContainer(Container container)
    {
        return container != null && container.getClass().getName().equals("baubles.common.container.ContainerPlayerExpanded");
    }
}