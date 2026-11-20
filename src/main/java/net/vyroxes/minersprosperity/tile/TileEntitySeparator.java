package net.vyroxes.minersprosperity.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.vyroxes.minersprosperity.machine.recipe.SeparatorRecipe;
import net.vyroxes.minersprosperity.machine.upgrade.MachineUpgradeType;
import net.vyroxes.minersprosperity.fluid.CustomFluidTank;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.machine.SidedIngredientHandler;
import net.vyroxes.minersprosperity.inventory.slot.SlotState;
import java.util.Objects;

public class TileEntitySeparator extends TileEntityProcessingMachine
{
    private Fluid recipeFluid;

    // Containers can drain the recipe tank without enabling extraction through automation.
    private final IFluidHandler containerFluidHandler = new IFluidHandler()
    {
        @Override
        public IFluidTankProperties[] getTankProperties()
        {
            CustomFluidTank tank = Objects.requireNonNull(getFluidTank(0));
            return new IFluidTankProperties[]{new FluidTankProperties(tank.getFluid(), tank.getCapacity(), true, true)};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill)
        {
            return Objects.requireNonNull(getFluidTank(0)).fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain)
        {
            CustomFluidTank tank = Objects.requireNonNull(getFluidTank(0));
            if (resource == null || resource.amount <= 0 || !resource.isFluidEqual(tank.getFluid())) return null;
            return tank.drainInternal(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain)
        {
            return Objects.requireNonNull(getFluidTank(0)).drainInternal(maxDrain, doDrain);
        }
    };

    public TileEntitySeparator()
    {
        super(new Builder()
                .setItemInput(2)
                .setItemEnergy(1)
                .setItemOutput(2)
                .setItemUpgrade(4)
                .setEnergy(ModConfig.MACHINES.separatorEnergyCapacity, ModConfig.MACHINES.separatorMaxReceive, 0, 0)
                .addFluidTank(null, ModConfig.MACHINES.separatorTankCapacity, 0, false, true, SlotState.SlotType.INPUT, SlotState.SlotMode.INPUT)
                .addExperienceTank(Integer.MAX_VALUE)
                .setSidedIngredientHandler(new SidedIngredientHandler.Builder().setEnergyHandler(SlotState.SlotMode.INPUT)));

        this.fluidTanks[0] = new CustomFluidTank(null, 0, ModConfig.MACHINES.separatorTankCapacity, false, true)
        {
            @Override
            public boolean canFillFluidType(FluidStack fluid)
            {
                return super.canFillFluidType(fluid) && SeparatorRecipe.isFluidValid(fluid);
            }

            @Override
            protected void onContentsChanged()
            {
                FluidStack contents = this.getFluid();
                Fluid fluid = contents == null ? null : contents.getFluid();
                if (recipeFluid != null && fluid != null && fluid != recipeFluid) cookTime = 0;
                if (fluid != null) recipeFluid = fluid;
                invalidateRecipeCache();
                TileEntitySeparator.this.markDirty();
            }
        };
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.hasCustomName() ? new TextComponentString(this.customName) : new TextComponentTranslation("container.separator");
    }

    public static boolean isFluidContainer(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        ItemStack single = stack.copy();
        single.setCount(1);
        if (FluidUtil.getFluidHandler(single) == null) return false;
        FluidStack fluid = FluidUtil.getFluidContained(single);
        return fluid == null || SeparatorRecipe.isFluidValid(fluid);
    }

    public IFluidHandler getContainerFluidHandler()
    {
        return this.containerFluidHandler;
    }

    @Override
    public void update()
    {
        if (this.world != null && !this.world.isRemote && this.canMachineOperate()) this.processFluidContainer();
        super.update();
    }

    private void processFluidContainer()
    {
        ItemStack input = this.getInputStack(1);
        if (input.isEmpty()) return;

        FluidActionResult preview = FluidUtil.tryFillContainer(input, this.containerFluidHandler, Integer.MAX_VALUE, null, false);
        boolean filling = preview.isSuccess();
        if (!filling) preview = FluidUtil.tryEmptyContainer(input, this.containerFluidHandler, Integer.MAX_VALUE, null, false);
        if (!preview.isSuccess()) return;
        ItemStack result = preview.getResult();
        ItemStack output = this.getOutputStack(1);
        SlotState.SlotOutputMode mode = this.getOutputMode(EnumFacing.UP, 1);
        boolean fits = output.isEmpty() || (ItemStack.areItemsEqual(output, result)
                && ItemStack.areItemStackTagsEqual(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
        if (!result.isEmpty() && !fits && mode == SlotState.SlotOutputMode.DEFAULT) return;

        FluidActionResult transfer = filling
                ? FluidUtil.tryFillContainer(input, this.containerFluidHandler, Integer.MAX_VALUE, null, true)
                : FluidUtil.tryEmptyContainer(input, this.containerFluidHandler, Integer.MAX_VALUE, null, true);
        if (!transfer.isSuccess()) return;
        if (fits && mode != SlotState.SlotOutputMode.VOID_ALL)
        {
            if (output.isEmpty()) this.setOutputStack(1, transfer.getResult());
            else output.grow(transfer.getResult().getCount());
        }
        input.shrink(1);
        if (input.isEmpty()) this.clearInputStack(1);
        this.markDirty();
    }

    private SeparatorRecipe.Recipe getRecipe()
    {
        return this.getCachedRecipe(() -> SeparatorRecipe.findRecipe(this.getInputStack(0),
                Objects.requireNonNull(this.getFluidTank(0)).getFluid()));
    }

    @Override
    protected int calculateTotalCookTime()
    {
        SeparatorRecipe.Recipe recipe = this.getRecipe();
        if (recipe == null) return 0;

        int speedUpgradesCount = this.getUpgradeCount(MachineUpgradeType.SPEED);
        int baseTotalCookTime = recipe.totalCookTime();

        return Math.max(1, Math.round(baseTotalCookTime * (1.0F - 0.9F * speedUpgradesCount / 8)));
    }

    private int calculateInputsCount()
    {
        ItemStack input = this.getInputStack(0);
        ItemStack output = this.getOutputStack(0);

        if (input.isEmpty()) return 0;

        SeparatorRecipe.Recipe recipe = this.getRecipe();
        if (recipe == null) return 0;

        int requiredQuantity = recipe.quantity();
        if (requiredQuantity <= 0) return 0;

        int maxSets = Math.min(input.getCount() / requiredQuantity,
                Objects.requireNonNull(this.getFluidTank(0)).getFluidAmount() / recipe.fluid().amount);

        int upgrades = this.getUpgradeCount(MachineUpgradeType.MACHINE);
        int machineUpgradesCount = upgrades == 8 ? 10 : upgrades + 1;

        maxSets = Math.min(maxSets, machineUpgradesCount);

        ItemStack result = recipe.result();
        if (result.isEmpty()) return 0;

        if (this.getOutputMode(EnumFacing.UP, 0) == SlotState.SlotOutputMode.DEFAULT)
        {
            if (output.isEmpty())
            {
                int maxByOutput = result.getMaxStackSize() / result.getCount();
                maxSets = Math.min(maxSets, maxByOutput);
            }
            else
            {
                if (!ItemStack.areItemsEqual(output, result) || !ItemStack.areItemStackTagsEqual(output, result)) return 0;

                int maxByOutput = (output.getMaxStackSize() - output.getCount()) / result.getCount();
                maxSets = Math.min(maxSets, maxByOutput);
            }
        }

        return maxSets;
    }

    @Override
    protected int calculateEnergyUsage()
    {
        SeparatorRecipe.Recipe recipe = this.getRecipe();
        if (recipe == null) return 0;

        int quantity = this.calculateInputsCount();
        if (quantity <= 0) return 0;

        int speedUpgradesCount = this.getUpgradeCount(MachineUpgradeType.SPEED);
        int energyUpgradesCount = this.getUpgradeCount(MachineUpgradeType.ENERGY);

        int totalCookTime = this.calculateTotalCookTime();
        if (totalCookTime <= 0) return 0;

        double speedModifier = 1.0 + 4.0 * speedUpgradesCount / 8;
        double energyModifier = 1.0 - 0.5 * energyUpgradesCount / 8;
        double processEnergy = (double) recipe.energyUsage() * recipe.totalCookTime() * speedModifier * energyModifier * quantity;

        return (int) Math.ceil(processEnergy / totalCookTime);
    }

    @Override
    protected boolean canProcess()
    {
        if (this.storage == null) return false;

        ItemStack input = this.getInputStack(0);
        if (input.isEmpty()) return false;

        SeparatorRecipe.Recipe recipe = this.getRecipe();
        if (recipe == null) return false;

        ItemStack result = recipe.result();
        if (result.isEmpty()) return false;

        int quantity = this.calculateInputsCount();
        if (quantity <= 0) return false;

        int totalCookTime = this.calculateTotalCookTime();
        int energyUsage = this.calculateEnergyUsage();
        long requiredEnergy = (long) energyUsage * Math.max(0, totalCookTime - this.cookTime);

        if (this.storage.getEnergyStored() < requiredEnergy) return false;

        SidedIngredientHandler handler = this.getSidedHandler(EnumFacing.UP);
        if (handler == null) return false;

        int experienceUpgrades = this.getUpgradeCount(MachineUpgradeType.EXPERIENCE);
        float experienceModifier = 1.0F + (float) experienceUpgrades / 10.0F;
        int experience = (int) (experienceModifier * recipe.experience() * quantity * ((float) 1000 / ModConfig.MACHINES.mbToXpRatio));

        CustomFluidTank experienceTank = this.getExperienceTank();
        if (experienceTank != null && experienceTank.getFluidAmount() + experience > experienceTank.getCapacity() && handler.getSlotOutputMode(this.getExperienceTankStateId()) == SlotState.SlotOutputMode.DEFAULT) return false;

        if (this.getOutputMode(EnumFacing.UP, 0) != SlotState.SlotOutputMode.DEFAULT) return true;

        ItemStack output = this.getOutputStack(0);

        if (output.isEmpty()) return result.getCount() * quantity <= result.getMaxStackSize();
        if (!ItemStack.areItemsEqual(output, result) || !ItemStack.areItemStackTagsEqual(output, result)) return false;

        return output.getCount() + result.getCount() * quantity <= output.getMaxStackSize();
    }

    @Override
    protected void finishProcess()
    {
        ItemStack input = this.getInputStack(0);
        ItemStack output = this.getOutputStack(0);

        SeparatorRecipe.Recipe recipe = this.getRecipe();
        if (recipe == null) return;

        int quantity = this.calculateInputsCount();
        if (quantity <= 0) return;

        int requiredQuantity = recipe.quantity();
        if (requiredQuantity <= 0) return;

        ItemStack result = recipe.result();
        if (result.isEmpty()) return;

        int experienceUpgrades = this.getUpgradeCount(MachineUpgradeType.EXPERIENCE);
        float experienceModifier = 1.0F + (float) experienceUpgrades / 10.0F;
        int experience = (int) (experienceModifier * recipe.experience() * quantity * ((float) 1000 / ModConfig.MACHINES.mbToXpRatio));

        SidedIngredientHandler handler = this.getSidedHandler(EnumFacing.UP);
        if (handler == null) return;

        if (this.getOutputMode(EnumFacing.UP, 0) != SlotState.SlotOutputMode.VOID_ALL)
        {
            if (output.isEmpty())
            {
                ItemStack resultCopy = result.copy();
                resultCopy.setCount(Math.min(result.getCount() * quantity, result.getMaxStackSize()));
                this.setOutputStack(0, resultCopy);
            }
            else if (ItemStack.areItemsEqual(output, result) && ItemStack.areItemStackTagsEqual(output, result))
            {
                output.grow(Math.min(result.getCount() * quantity, Math.max(0, output.getMaxStackSize() - output.getCount())));
            }
        }

        Objects.requireNonNull(this.getFluidTank(0)).drainInternal(recipe.fluid().amount * quantity, true);
        input.shrink(quantity * requiredQuantity);

        if (input.isEmpty()) this.clearInputStack(0);

        CustomFluidTank experienceTank = this.getExperienceTank();
        if (experienceTank != null && handler.getSlotOutputMode(this.getExperienceTankStateId()) != SlotState.SlotOutputMode.VOID_ALL) experienceTank.fillInternal(new FluidStack(getConfiguredXpFluid(), experience), true);

        this.invalidateRecipeCache();
        this.markDirty();
    }
}