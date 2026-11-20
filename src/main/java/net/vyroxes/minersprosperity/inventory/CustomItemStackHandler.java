package net.vyroxes.minersprosperity.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.vyroxes.minersprosperity.init.ModItems;
import net.vyroxes.minersprosperity.machine.recipe.AlloyFurnaceRecipe;
import net.vyroxes.minersprosperity.machine.recipe.MetalPressRecipe;
import net.vyroxes.minersprosperity.machine.recipe.PulverizerRecipe;
import net.vyroxes.minersprosperity.machine.recipe.CrystallizerRecipe;
import net.vyroxes.minersprosperity.machine.recipe.GrinderRecipe;
import net.vyroxes.minersprosperity.machine.recipe.SeparatorRecipe;
import net.vyroxes.minersprosperity.machine.upgrade.MachineUpgradeType;
import net.vyroxes.minersprosperity.tile.*;
import org.jetbrains.annotations.NotNull;

public class CustomItemStackHandler extends ItemStackHandler
{
    private final TileEntity tile;
    private final int inputSlots;
    private final int energySlots;
    private final int outputSlots;
    private final int upgradeSlots;
    public static final ItemStack[] DIES =
    {
            new ItemStack(ModItems.IRON_GEAR_DIE),
            new ItemStack(ModItems.IRON_PLATE_DIE),
            new ItemStack(ModItems.IRON_ROD_DIE),
            new ItemStack(ModItems.REFINED_OBSIDIAN_GEAR_DIE),
            new ItemStack(ModItems.REFINED_OBSIDIAN_PLATE_DIE),
            new ItemStack(ModItems.REFINED_OBSIDIAN_ROD_DIE)
    };

    public CustomItemStackHandler(TileEntity tile, int inputSlots, int energySlots, int outputSlots, int upgradeSlots)
    {
        super(inputSlots + energySlots + outputSlots + upgradeSlots);
        this.tile = tile;
        this.inputSlots = inputSlots;
        this.energySlots = energySlots;
        this.outputSlots = outputSlots;
        this.upgradeSlots = upgradeSlots;
    }

    @Override
    public int getSlots()
    {
        return tile instanceof TileEntityStorageCrate crate ? crate.slots() : super.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot)
    {
        return tile instanceof TileEntityStorageCrate crate ? crate.inventory().getStackInSlot(slot) : super.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
    {
        if (tile instanceof TileEntityTrashCan trash)
            {
            if (slot != 0 || !trash.acceptsItem(stack)) return stack;
            if (!trash.preservesContainer(stack)) return ItemStack.EMPTY;
            return super.insertItem(slot, stack, simulate);
        }
        return tile instanceof TileEntityStorageCrate crate ? crate.insert(slot, stack, simulate) : super.insertItem(slot, stack, simulate);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack)
    {
        if (tile instanceof TileEntityTrashCan trash && slot == 0 && !stack.isEmpty() && !trash.preservesContainer(stack)) return;
        super.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return tile instanceof TileEntityStorageCrate crate ? crate.extract(slot, amount, simulate) : super.extractItem(slot, amount, simulate);
    }

    @Override
    protected void onContentsChanged(int slot)
    {
        if (this.tile instanceof TileEntityTrashCan || this.tile instanceof TileEntityFluidTank || this.tile instanceof TileEntityEnergyStorage || this.tile instanceof TileEntityRockCrusher || this.tile instanceof TileEntityMachine && ((TileEntityMachine) this.tile).supportsRange()) this.tile.markDirty();
    }

    public int getInputSlots()
    {
        return tile instanceof TileEntityStorageCrate crate ? crate.slots() : this.inputSlots;
    }

    public int getEnergySlots()
    {
        return this.energySlots;
    }

    public int getOutputSlots()
    {
        return this.outputSlots;
    }

    public int getUpgradeSlots()
    {
        return this.upgradeSlots;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        if (tile instanceof TileEntityTrashCan trash && slot == 0 && (trash.getDisposalMode(1) == 2 || trash.getDisposalMode(2) == 2)) return 1;
        if (tile instanceof TileEntityStorageCrate crate) return (int) Math.min(Integer.MAX_VALUE, crate.capacity(slot));
        if (slot >= this.inputSlots && slot < this.inputSlots + this.energySlots) return 1;
        if (slot >= this.inputSlots + this.energySlots + this.outputSlots) return 8;
        return super.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        if (tile instanceof TileEntityStorageCrate crate) return slot >= 0 && slot < crate.slots() && !stack.isEmpty() && crate.insert(slot, stack, true).getCount() < stack.getCount();
        if (tile instanceof TileEntityTrashCan trash) return slot == 0 && trash.acceptsItem(stack);
        if (slot < this.inputSlots)
        {
            if (tile instanceof TileEntityAlloyFurnace) return AlloyFurnaceRecipe.getInstance().getLookupTable().isItemValid(this, 0, this.inputSlots, slot, stack);
            else if (tile instanceof TileEntityRockCrusher) return net.vyroxes.minersprosperity.machine.recipe.RockCrusherRecipe.getInstance().findRecipe(stack) != null;
            else if (tile instanceof TileEntityPulverizer) return PulverizerRecipe.getInstance().getLookupTable().isInputValid(stack);
            else if (tile instanceof TileEntityCrystallizer) return CrystallizerRecipe.getInstance().getLookupTable().isInputValid(stack);
            else if (tile instanceof TileEntitySeparator) return slot == 0 ? SeparatorRecipe.isInputValid(stack) : TileEntitySeparator.isFluidContainer(stack);
            else if (tile instanceof TileEntityGrinder) return GrinderRecipe.getInstance().getLookupTable().isInputValid(stack);
            else if (tile instanceof TileEntityMetalPress) return MetalPressRecipe.getInstance().getLookupTable().isItemValid(this, slot, stack);
            else if (tile instanceof TileEntityElectricFurnace) return !FurnaceRecipes.instance().getSmeltingResult(stack).isEmpty();
            else if (tile instanceof TileEntitySolidFuelGenerator) return TileEntityFurnace.getItemBurnTime(stack) > 0;
            else if (tile instanceof TileEntityFluidTank) return ((TileEntityFluidTank) tile).isContainerValid(slot, stack);
            else if (tile instanceof TileEntityLiquidFuelGenerator) return TileEntityLiquidFuelGenerator.isFluidContainer(stack);
            else return false;
        }
        else if (slot < this.inputSlots + this.energySlots)
        {
            if (this.tile instanceof TileEntityEnergyStorage) return ((TileEntityEnergyStorage) this.tile).isEnergyItemValid(slot - this.inputSlots, stack);
            return isEnergyItemValid(stack);
        }
        else if (slot >= this.inputSlots + this.energySlots + this.outputSlots)
        {
            return isUpgradeItemValid(slot, stack);
        }
        else
        {
            return false;
        }
    }

    public boolean isDieItemValid(ItemStack stack)
    {
        for (ItemStack die : DIES)
        {
            if (ItemStack.areItemsEqualIgnoreDurability(stack, die))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isEnergyItemValid(ItemStack stack)
    {
        if (!stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null))
        {
            IEnergyStorage itemEnergy = stack.getCapability(CapabilityEnergy.ENERGY, null);

            return itemEnergy != null;
        }

        return false;
    }

    public boolean isUpgradeItemValid(int slot, ItemStack stack)
    {
        if (!(this.tile instanceof TileEntityMachine machine) || stack.isEmpty()) return false;

        int index = slot - (this.inputSlots + this.energySlots + this.outputSlots);
        MachineUpgradeType type = machine.getUpgradeType(index);

        return type != null && stack.getItem() == type.getItem();
    }

    public int isUpgradeItemValid(ItemStack stack)
    {
        if (!(this.tile instanceof TileEntityMachine machine) || stack.isEmpty()) return -1;

        for (int i = 0; i < this.upgradeSlots; i++)
        {
            MachineUpgradeType type = machine.getUpgradeType(i);

            if (type != null && stack.getItem() == type.getItem()) return i;
        }

        return -1;
    }
}