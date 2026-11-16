package net.vyroxes.minersprosperity.item.backpack;

import net.minecraft.util.ResourceLocation;
import net.vyroxes.minersprosperity.Tags;

public class ItemBackpack extends ItemBackpackBase
{
    private final BackpackTier tier;

    public ItemBackpack(String name, BackpackTier tier)
    {
        super(name);
        this.addPropertyOverride(new ResourceLocation("minersprosperity", "open_" + tier.translationKey), ((stack, worldIn, entityIn) -> {
            if (entityIn instanceof net.minecraft.entity.player.EntityPlayer)
            {
                net.minecraft.inventory.Container container = entityIn.world.isRemote
                        ? visualContainer((net.minecraft.entity.player.EntityPlayer) entityIn)
                        : ((net.minecraft.entity.player.EntityPlayer) entityIn).openContainer;
                if (container instanceof net.vyroxes.minersprosperity.inventory.container.ContainerBackpack
                        && ((net.vyroxes.minersprosperity.inventory.container.ContainerBackpack) container).isOpenBackpack(stack, (net.minecraft.entity.player.EntityPlayer) entityIn)) return 1.0F;
                if (container instanceof net.vyroxes.minersprosperity.inventory.container.ContainerBackpackConfiguration
                        && ((net.vyroxes.minersprosperity.inventory.container.ContainerBackpackConfiguration) container).isOpenBackpack(stack, (net.minecraft.entity.player.EntityPlayer) entityIn)) return 1.0F;
            }
            return 0.0f;
        }));
        this.tier = tier;
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    private static net.minecraft.inventory.Container visualContainer(net.minecraft.entity.player.EntityPlayer player)
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

        if (player == mc.player && (mc.currentScreen instanceof net.vyroxes.minersprosperity.client.gui.GuiBackpack
                || mc.currentScreen instanceof net.vyroxes.minersprosperity.client.gui.GuiBackpackConfiguration))
            return ((net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen).inventorySlots;
        return player.openContainer;
    }

    @Override
    public int getInventorySize()
    {
        return tier.inventorySize;
    }

    @Override
    public int getInventoryColumns()
    {
        return tier.inventoryColumns;
    }

    @Override
    public int getConfigurationSize() {
        int rows = tier.inventorySize / tier.inventoryColumns;
        int panelColumns = rows < 6 ? 3 : 2;
        return (tier.inventoryColumns - panelColumns - 1) * rows;
    }

    @Override
    public ResourceLocation getTexture()
    {
        return new ResourceLocation(Tags.MODID, "textures/gui/backpack_" + tier.inventorySize + ".png");
    }

    @Override
    public ResourceLocation getTexture2()
    {
        return new ResourceLocation(Tags.MODID, "textures/gui/backpack_" + tier.inventorySize + "_2.png");
    }

    @Override
    public ResourceLocation getConfigurationTexture() {
        return new ResourceLocation(Tags.MODID, "textures/gui/backpack_configuration_" + tier.inventorySize + ".png");
    }

    @Override
    public ResourceLocation getConfigurationTexture2() {
        return new ResourceLocation(Tags.MODID, "textures/gui/backpack_configuration_" + tier.inventorySize + "_2.png");
    }

    @Override
    public BackpackTier getTier()
    {
        return tier;
    }
}