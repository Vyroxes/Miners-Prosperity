package net.vyroxes.minersprosperity.mixin;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.inventory.slot.SlotFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Pseudo
@Mixin(targets = "net.gegy1000.wts.SlotPlaceInfo", remap = false)
public abstract class MixinWhatsThatSlotFilters
{
    @Redirect(method = "create", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"), remap = false)
    private static Stream<ItemStack> allowFilterIngredients(Stream<ItemStack> items, Predicate<ItemStack> valid, Slot slot)
    {
        return ModConfig.INTEGRATION.whatsThatSlotPatch && slot instanceof SlotFilter ? items : items.filter(valid);
    }
}