package net.vyroxes.minersprosperity.integration.jei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.vyroxes.minersprosperity.client.gui.GuiConduit;
import net.vyroxes.minersprosperity.transport.ConduitPort;
import org.jetbrains.annotations.NotNull;

public final class ConduitGhostIngredientHandler implements IGhostIngredientHandler<GuiConduit>
{
    @Override
    public <I> @NotNull List<Target<I>> getTargets(@NotNull GuiConduit gui, @NotNull I ingredient, boolean doStart)
    {
        if (!gui.canEditFilters() || !(ingredient instanceof ItemStack) && !(ingredient instanceof FluidStack))
            return Collections.emptyList();
        List<Target<I>> targets = new ArrayList<>();
        for (int i = 0; i < ConduitPort.FILTER_COLUMNS * ConduitPort.FILTER_ROWS; i++)
        {
            int index = i;
            targets.add(new Target<I>()
            {
                @Override
                public @NotNull Rectangle getArea()
                {
                    return gui.getFilterArea(index);
                }

                @Override
                public void accept(@NotNull I value)
                {
                    ItemStack stack = value instanceof ItemStack item ? item
                            : value instanceof FluidStack fluid ? FluidUtil.getFilledBucket(fluid) : ItemStack.EMPTY;
                    if (!stack.isEmpty()) gui.setGhostFilter(index, stack);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete()
    {
    }
}