package net.vyroxes.minersprosperity.mixin;

import net.vyroxes.minersprosperity.integration.FTBSidebarButtonPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.feed_the_beast.ftblib.client.FTBLibClientEventHandler$GuiButtonSidebar", remap = false)
public abstract class MixinFTBSidebarButtonPosition implements FTBSidebarButtonPosition
{
    @Mutable @Shadow @Final public int buttonX;
    @Mutable @Shadow @Final public int buttonY;

    @Override
    public void minersprosperity$setGridPosition(int column, int row)
    {
        buttonX = column;
        buttonY = row;
    }
}