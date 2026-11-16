package net.vyroxes.minersprosperity.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.init.ModSounds;
import net.vyroxes.minersprosperity.inventory.slot.SlotState;
import net.vyroxes.minersprosperity.machine.SidedIngredientHandler;
import net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe;
import net.vyroxes.minersprosperity.machine.upgrade.MachineUpgradeType;
import net.vyroxes.minersprosperity.fluid.CustomFluidTank;

import java.util.Objects;

public class TileEntityRockCrusher extends TileEntityProcessingMachine
{
    public TileEntityRockCrusher()
    {
        super(new Builder().setItemInput(1).setItemEnergy(1).setItemOutput(3).setItemUpgrade(4)
                .setEnergy(ModConfig.MACHINES.rockCrusherEnergyCapacity, ModConfig.MACHINES.rockCrusherMaxReceive, 0, 0)
                .addExperienceTank(Integer.MAX_VALUE)
                .setSidedIngredientHandler(new SidedIngredientHandler.Builder().setEnergyHandler(SlotState.SlotMode.INPUT)));
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return hasCustomName() ? new TextComponentString(customName) : new TextComponentTranslation("container.rock_crusher");
    }

    @Override
    public SoundEvent getActiveSound()
    {
        return ModSounds.ROCK_CRUSHER_ACTIVE;
    }

    @Override
    public float getActiveSoundVolume()
    {
        return (float) ModConfig.MACHINES.rockCrusherSoundVolume;
    }

    private RockCrusherRecipe.Recipe getRecipe()
    {
        return getCachedRecipe(() -> RockCrusherRecipe.getInstance().findRecipe(getInputStack(0)));
    }

    private int calculateInputsCount()
    {
        RockCrusherRecipe.Recipe recipe = getRecipe();
        if (recipe == null) return 0;
        int upgrades = getUpgradeCount(MachineUpgradeType.MACHINE);
        int sets = Math.min(getInputStack(0).getCount() / recipe.quantity(), upgrades == 8 ? 10 : upgrades + 1);

        for (int i = 0; i < recipe.outputs().size(); i++)
        {
            if (getOutputMode(EnumFacing.UP, i) != SlotState.SlotOutputMode.DEFAULT) continue;
            RockCrusherRecipe.Output result = recipe.outputs().get(i);
            ItemStack existing = getOutputStack(i);
            if (!existing.isEmpty() && (!ItemStack.areItemsEqual(existing, result.item)
                    || !ItemStack.areItemStackTagsEqual(existing, result.item))) return 0;
            int space = Math.min(result.item.getMaxStackSize(), getCustomItemStackHandler().getSlotLimit(getOutputSlotId(i))) - existing.getCount();
            sets = Math.min(sets, space / result.maxCount);
        }
        return Math.max(0, sets);
    }

    @Override
    protected int calculateTotalCookTime()
    {
        RockCrusherRecipe.Recipe recipe = getRecipe();
        return recipe == null ? 0 : Math.max(1, Math.round(recipe.totalCookTime() * (1 - 0.9F * getUpgradeCount(MachineUpgradeType.SPEED) / 8)));
    }

    @Override
    protected int calculateEnergyUsage()
    {
        RockCrusherRecipe.Recipe recipe = getRecipe();
        int time = calculateTotalCookTime();
        if (recipe == null || time == 0) return 0;
        double cost = (double) recipe.energyUsage() * recipe.totalCookTime() * calculateInputsCount()
                * (1 + 4.0 * getUpgradeCount(MachineUpgradeType.SPEED) / 8)
                * (1 - 0.5 * getUpgradeCount(MachineUpgradeType.ENERGY) / 8);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(cost / time));
    }

    private int experience(int sets)
    {
        RockCrusherRecipe.Recipe recipe = getRecipe();
        return recipe == null ? 0 : (int) Math.min(Integer.MAX_VALUE, recipe.experience() * (double) sets
                * (1 + getUpgradeCount(MachineUpgradeType.EXPERIENCE) / 10.0)
                * 1000 / Math.max(1, ModConfig.MACHINES.mbToXpRatio));
    }

    @Override
    protected boolean canProcess()
    {
        int sets = calculateInputsCount();
        if (sets == 0 || storage == null) return false;
        long requiredEnergy = (long) calculateEnergyUsage() * Math.max(0, calculateTotalCookTime() - cookTime);
        if (storage.getEnergyStored() < requiredEnergy) return false;
        SidedIngredientHandler handler = getSidedHandler(EnumFacing.UP);
        if (handler == null) return false;
        CustomFluidTank tank = getExperienceTank();
        return tank == null || handler.getSlotOutputMode(getExperienceTankStateId()) != SlotState.SlotOutputMode.DEFAULT
                || (long) tank.getFluidAmount() + experience(sets) <= tank.getCapacity();
    }

    @Override
    protected void finishProcess()
    {
        RockCrusherRecipe.Recipe recipe = getRecipe();
        int sets = calculateInputsCount();
        if (recipe == null || sets == 0 || !canProcess()) return;
        int xp = experience(sets);
        for (int i = 0; i < recipe.outputs().size(); i++)
        {
            RockCrusherRecipe.Output output = recipe.outputs().get(i);
            int count = 0;
            for (int set = 0; set < sets; set++) count += output.roll(world.rand);
            if (count == 0 || getOutputMode(EnumFacing.UP, i) == SlotState.SlotOutputMode.VOID_ALL) continue;
            ItemStack existing = getOutputStack(i);
            if (!existing.isEmpty() && (!ItemStack.areItemsEqual(existing, output.item)
                    || !ItemStack.areItemStackTagsEqual(existing, output.item))) continue;
            int capacity = Math.min(output.item.getMaxStackSize(), getCustomItemStackHandler().getSlotLimit(getOutputSlotId(i)));
            ItemStack result = output.item.copy();
            result.setCount(Math.min(capacity, existing.getCount() + count));
            setOutputStack(i, result);
        }
        ItemStack input = getInputStack(0);
        input.shrink(sets * recipe.quantity());
        if (input.isEmpty()) clearInputStack(0);
        CustomFluidTank tank = getExperienceTank();
        if (tank != null && xp > 0 && Objects.requireNonNull(getSidedHandler(EnumFacing.UP)).getSlotOutputMode(getExperienceTankStateId()) != SlotState.SlotOutputMode.VOID_ALL)
            tank.fillInternal(new FluidStack(getConfiguredXpFluid(), xp), true);
        invalidateRecipeCache();
        markDirty();
    }
}