package net.vyroxes.minersprosperity.integration.jei;

import mezz.jei.api.*;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.vyroxes.minersprosperity.init.ModBlocks;
import net.vyroxes.minersprosperity.inventory.container.*;
import net.vyroxes.minersprosperity.client.gui.GuiAlloyFurnace;
import net.vyroxes.minersprosperity.client.gui.GuiElectricFurnace;
import net.vyroxes.minersprosperity.client.gui.GuiMetalPress;
import net.vyroxes.minersprosperity.client.gui.GuiPulverizer;
import net.vyroxes.minersprosperity.client.gui.GuiCrystallizer;
import net.vyroxes.minersprosperity.client.gui.GuiGrinder;
import net.vyroxes.minersprosperity.client.gui.GuiSeparator;
import net.vyroxes.minersprosperity.integration.jei.alloy_furnace.AlloyFurnaceRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.alloy_furnace.AlloyFurnaceRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.metal_press.MetalPressRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.metal_press.MetalPressRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.pulverizer.PulverizerRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.crystallizer.CrystallizerRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.grinder.GrinderRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.separator.SeparatorRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.pulverizer.PulverizerRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.crystallizer.CrystallizerRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.grinder.GrinderRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.separator.SeparatorRecipeMaker;
import net.vyroxes.minersprosperity.integration.jei.material_tree.MaterialTreeRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.material_tree.MaterialTreeRecipeMaker;
import net.vyroxes.minersprosperity.client.gui.GuiLiquidFuelGenerator;
import net.vyroxes.minersprosperity.integration.jei.liquid_fuel_generator.LiquidFuelGeneratorRecipeCategory;
import net.vyroxes.minersprosperity.integration.jei.liquid_fuel_generator.LiquidFuelGeneratorRecipeTransferHandler;
import java.util.IllegalFormatException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tileentity.TileEntityFurnace;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.machine.upgrade.MachineUpgradeType;
import net.vyroxes.minersprosperity.client.gui.GuiSolidFuelGenerator;
import net.vyroxes.minersprosperity.integration.jei.solid_fuel_generator.SolidFuelGeneratorRecipe;
import net.vyroxes.minersprosperity.integration.jei.solid_fuel_generator.SolidFuelGeneratorRecipeCategory;
import org.jetbrains.annotations.NotNull;

@JEIPlugin
public class JEICompat implements IModPlugin

{
    public static final boolean IS_HEI = JEICompat.class.getResource("/mezz/jei/bookmarks/BookmarkItem.class") != null;

    private static final java.lang.reflect.Method HEI_BOOKMARK_INGREDIENT;

    static
    {
        java.lang.reflect.Method getter = null;
        if (IS_HEI)
        {
            try
            {
                getter = Class.forName("mezz.jei.bookmarks.BookmarkItem", false, JEICompat.class.getClassLoader()).getMethod("getIngredient");
            }
            catch (ReflectiveOperationException exception)
            {
                throw new IllegalStateException("Unable to access HEI bookmark ingredients", exception);
            }
        }
        HEI_BOOKMARK_INGREDIENT = getter;
    }

    private static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime)
    {
        runtime = jeiRuntime;
    }

    public static boolean showCraftingRecipes()
    {
        if (runtime == null) return false;
        runtime.getRecipesGui().showCategories(java.util.Collections.singletonList(mezz.jei.api.recipe.VanillaRecipeCategoryUid.CRAFTING));
        return true;
    }

    public static ItemStack getHoveredItem()
    {
        if (runtime == null) return ItemStack.EMPTY;
        Object ingredient = runtime.getIngredientListOverlay().getIngredientUnderMouse();
        if (!(ingredient instanceof ItemStack)) ingredient = getHoveredBookmark();
        if (!(ingredient instanceof ItemStack)) ingredient = runtime.getRecipesGui().getIngredientUnderMouse();
        return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
    }

    public static ItemStack getHoveredBookmark()
    {
        Object ingredient = runtime == null ? null : runtime.getBookmarkOverlay().getIngredientUnderMouse();
        if (HEI_BOOKMARK_INGREDIENT != null && HEI_BOOKMARK_INGREDIENT.getDeclaringClass().isInstance(ingredient))
        {
            try
            {
                ingredient = HEI_BOOKMARK_INGREDIENT.invoke(ingredient);
            }
            catch (ReflectiveOperationException exception)
            {
                throw new IllegalStateException("Unable to read HEI bookmark ingredient", exception);
            }
        }
        return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
    }

    public static ItemStack getHoveredListItem()
    {
        Object ingredient = runtime == null ? null : runtime.getIngredientListOverlay().getIngredientUnderMouse();
        return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
    }

    public static java.awt.Rectangle getHoveredListArea(int mouseX, int mouseY)
    {
        if (runtime == null || !(runtime.getIngredientListOverlay() instanceof mezz.jei.input.IShowsRecipeFocuses)) return null;
        mezz.jei.input.IClickedIngredient<?> ingredient = ((mezz.jei.input.IShowsRecipeFocuses) runtime.getIngredientListOverlay())
                .getIngredientUnderMouse(mouseX, mouseY);
        return ingredient == null ? null : ingredient.getArea();
    }

    public static java.awt.Rectangle getHoveredBookmarkArea(int mouseX, int mouseY)
    {
        if (runtime == null || !(runtime.getBookmarkOverlay() instanceof mezz.jei.input.IShowsRecipeFocuses)) return null;
        mezz.jei.input.IClickedIngredient<?> ingredient = ((mezz.jei.input.IShowsRecipeFocuses) runtime.getBookmarkOverlay())
                .getIngredientUnderMouse(mouseX, mouseY);
        return ingredient == null ? null : ingredient.getArea();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void registerItemSubtypes(mezz.jei.api.@NotNull ISubtypeRegistry registry)
    {
        for (net.minecraft.item.Item item : net.vyroxes.minersprosperity.init.ModItems.ITEMS)
        {
            if (!(item instanceof net.vyroxes.minersprosperity.item.battery.ItemBattery)
                    && !(item instanceof net.vyroxes.minersprosperity.block.machine.ItemBlockEnergyStorage)) continue;
            if (!net.vyroxes.minersprosperity.init.ModItems.isEnabled(item)) continue;
            registry.registerSubtypeInterpreter(item, stack ->
            {
                net.minecraftforge.energy.IEnergyStorage energy = stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
                return energy != null && energy.getEnergyStored() > 0 ? "charged" : "empty";
            });
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry)
    {
        final IJeiHelpers helpers = registry.getJeiHelpers();
        final IGuiHelper gui = helpers.getGuiHelper();

        registry.addRecipeCategories(new AlloyFurnaceRecipeCategory(gui));
        registry.addRecipeCategories(new PulverizerRecipeCategory(gui));
        if (ModConfig.MACHINES.enableCrystallizer) registry.addRecipeCategories(new CrystallizerRecipeCategory(gui));
        if (ModConfig.MACHINES.enableGrinder) registry.addRecipeCategories(new GrinderRecipeCategory(gui));
        if (ModConfig.MACHINES.enableSeparator) registry.addRecipeCategories(new SeparatorRecipeCategory(gui));
        if (ModConfig.MACHINES.enableRockCrusher) registry.addRecipeCategories(new net.vyroxes.minersprosperity.integration.jei.rock_crusher.RockCrusherRecipeCategory(gui));
        registry.addRecipeCategories(new MetalPressRecipeCategory(gui));
        registry.addRecipeCategories(new MaterialTreeRecipeCategory(gui));
        if (ModConfig.MACHINES.enableLiquidFuelGenerator) registry.addRecipeCategories(new LiquidFuelGeneratorRecipeCategory(gui));
        if (ModConfig.MACHINES.enableSolidFuelGenerator) registry.addRecipeCategories(new SolidFuelGeneratorRecipeCategory(gui));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(IModRegistry registry)
    {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                new net.vyroxes.minersprosperity.client.event.BackpackFilterDragHandler(registry.getIngredientRegistry()));
        registry.addGlobalGuiHandlers(new mezz.jei.api.gui.IGlobalGuiHandler()
        {
            @Override
            public java.util.@NotNull Collection<java.awt.Rectangle> getGuiExtraAreas()
            {
                if (net.vyroxes.minersprosperity.client.event.WhatsThatSlotOverlay.isVisible())
                {
                    net.minecraft.client.gui.GuiScreen screen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (screen instanceof net.minecraft.client.gui.inventory.GuiContainer)
                        return java.util.Collections.singleton(new java.awt.Rectangle(0, 0,
                                ((net.minecraft.client.gui.inventory.GuiContainer) screen).getGuiLeft(), screen.height));
                }
                return java.util.Collections.emptySet();
            }
        });
        registry.addGhostIngredientHandler(net.vyroxes.minersprosperity.client.gui.GuiBackpackConfiguration.class,
                new BackpackGhostIngredientHandler());
        registry.addGhostIngredientHandler(net.vyroxes.minersprosperity.client.gui.GuiConduit.class,
                new ConduitGhostIngredientHandler());
        registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(new ItemStack(ModBlocks.RESIN_BLOCK));
        registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(new ItemStack(ModBlocks.RESIN_BLOCK_EMPTY));

        for (MachineUpgradeType type : MachineUpgradeType.values())
        {
            ItemStack upgrade = new ItemStack(type.getItem());
            String key = upgrade.getTranslationKey() + ".tooltip";
            List<String> descriptions = new ArrayList<>();
            descriptions.add(key);

            for (int line = 1; net.minecraft.client.resources.I18n.hasKey(key + "." + line); line++)
            {
                descriptions.add(key + "." + line);
            }

            descriptions.add("tooltip.upgrade.limit");
            registry.addIngredientInfo(upgrade, ItemStack.class, descriptions.toArray(new String[0]));
        }

        final IIngredientRegistry ingredientRegistry = registry.getIngredientRegistry();
        final IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        IRecipeTransferRegistry recipeTransfer = registry.getRecipeTransferRegistry();

        if (ModConfig.MACHINES.enableSolidFuelGenerator)
        {
            List<SolidFuelGeneratorRecipe> fuels = new ArrayList<>();
            for (ItemStack fuel : ingredientRegistry.getAllIngredients(ItemStack.class))
            {
                int burnTime = TileEntityFurnace.getItemBurnTime(fuel);
                if (burnTime > 0) fuels.add(new SolidFuelGeneratorRecipe(fuel, burnTime));
            }
            registry.addRecipes(fuels, RecipeCategories.SOLID_FUEL_GENERATOR);
            registry.addRecipeClickArea(GuiSolidFuelGenerator.class, 80, 27, 16, 15, RecipeCategories.SOLID_FUEL_GENERATOR);
            recipeTransfer.addRecipeTransferHandler(ContainerSolidFuelGenerator.class, RecipeCategories.SOLID_FUEL_GENERATOR, 0, 1, 2, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.SOLID_FUEL_GENERATOR), RecipeCategories.SOLID_FUEL_GENERATOR);
        }

        if (ModConfig.MACHINES.enableLiquidFuelGenerator)
        {
            List<net.vyroxes.minersprosperity.integration.jei.liquid_fuel_generator.LiquidFuelGeneratorRecipe> fuels = new ArrayList<>();
            for (net.vyroxes.minersprosperity.machine.recipe.LiquidFuelGeneratorRecipe.Recipe recipe : net.vyroxes.minersprosperity.machine.recipe.LiquidFuelGeneratorRecipe.getInstance().getRecipes())
            {
                fuels.add(new net.vyroxes.minersprosperity.integration.jei.liquid_fuel_generator.LiquidFuelGeneratorRecipe(recipe));
            }
            registry.addRecipes(fuels, RecipeCategories.LIQUID_FUEL_GENERATOR);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.LIQUID_FUEL_GENERATOR), RecipeCategories.LIQUID_FUEL_GENERATOR);
            registry.addRecipeClickArea(GuiLiquidFuelGenerator.class, 80, 36, 16, 15, RecipeCategories.LIQUID_FUEL_GENERATOR);
            recipeTransfer.addRecipeTransferHandler(new LiquidFuelGeneratorRecipeTransferHandler(jeiHelpers.recipeTransferHandlerHelper()), RecipeCategories.LIQUID_FUEL_GENERATOR);
        }

        registry.addRecipes(MaterialTreeRecipeMaker.getRecipes(), RecipeCategories.MATERIAL_TREE);

        registry.addRecipes(AnvilCustomRecipeMaker.getRepairRecipes(jeiHelpers.getVanillaRecipeFactory()), RecipeCategories.ANVIL);

        if (ModConfig.MACHINES.enableAlloyFurnace)
        {
            registry.addRecipes(AlloyFurnaceRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.ALLOY_FURNACE);
            registry.addRecipeClickArea(GuiAlloyFurnace.class, 79, 36, 21, 15, RecipeCategories.ALLOY_FURNACE);
            recipeTransfer.addRecipeTransferHandler(ContainerAlloyFurnace.class, RecipeCategories.ALLOY_FURNACE, 0, 2, 4, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.ALLOY_FURNACE), RecipeCategories.ALLOY_FURNACE);
        }

        if (ModConfig.MACHINES.enableRockCrusher)
        {
            registry.addRecipes(net.vyroxes.minersprosperity.integration.jei.rock_crusher.RockCrusherRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.ROCK_CRUSHER);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.ROCK_CRUSHER), RecipeCategories.ROCK_CRUSHER);
            registry.addRecipeClickArea(net.vyroxes.minersprosperity.client.gui.GuiRockCrusher.class, 53, 36, 22, 15, RecipeCategories.ROCK_CRUSHER);
            recipeTransfer.addRecipeTransferHandler(ContainerRockCrusher.class, RecipeCategories.ROCK_CRUSHER, 0, 1, 5, 36);
        }

        if (ModConfig.MACHINES.enablePulverizer)
        {
            registry.addRecipes(PulverizerRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.PULVERIZER);
            registry.addRecipeClickArea(GuiPulverizer.class, 69, 36, 21, 15, RecipeCategories.PULVERIZER);
            recipeTransfer.addRecipeTransferHandler(ContainerPulverizer.class, RecipeCategories.PULVERIZER, 0, 1, 3, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.PULVERIZER), RecipeCategories.PULVERIZER);
        }
        if (ModConfig.MACHINES.enableCrystallizer)
        {
            registry.addRecipes(CrystallizerRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.CRYSTALLIZER);
            registry.addRecipeClickArea(GuiCrystallizer.class, 69, 36, 21, 15, RecipeCategories.CRYSTALLIZER);
            recipeTransfer.addRecipeTransferHandler(ContainerCrystallizer.class, RecipeCategories.CRYSTALLIZER, 0, 1, 3, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.CRYSTALLIZER), RecipeCategories.CRYSTALLIZER);
        }
        if (ModConfig.MACHINES.enableGrinder)
        {
            registry.addRecipes(GrinderRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.GRINDER);
            registry.addRecipeClickArea(GuiGrinder.class, 69, 36, 21, 15, RecipeCategories.GRINDER);
            recipeTransfer.addRecipeTransferHandler(ContainerGrinder.class, RecipeCategories.GRINDER, 0, 1, 3, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.GRINDER), RecipeCategories.GRINDER);
        }

        if (ModConfig.MACHINES.enableSeparator)
        {
            registry.addRecipes(SeparatorRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.SEPARATOR);
            registry.addRecipeClickArea(GuiSeparator.class, 89, 36, 22, 15, RecipeCategories.SEPARATOR);
            recipeTransfer.addRecipeTransferHandler(ContainerSeparator.class, RecipeCategories.SEPARATOR, 0, 1, 5, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.SEPARATOR), RecipeCategories.SEPARATOR);
        }

        if (ModConfig.MACHINES.enableMetalPress)
        {
            registry.addRecipes(MetalPressRecipeMaker.getRecipes(jeiHelpers), RecipeCategories.METAL_PRESS);
            registry.addRecipeClickArea(GuiMetalPress.class, 79, 36, 21, 15, RecipeCategories.METAL_PRESS);
            recipeTransfer.addRecipeTransferHandler(ContainerMetalPress.class, RecipeCategories.METAL_PRESS, 0, 2, 4, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.METAL_PRESS), RecipeCategories.METAL_PRESS);
        }

        if (ModConfig.MACHINES.enableElectricFurnace)
        {
            registry.addRecipeClickArea(GuiElectricFurnace.class, 69, 36, 21, 15, RecipeCategories.SMELTING);
            recipeTransfer.addRecipeTransferHandler(ContainerElectricFurnace.class, RecipeCategories.SMELTING, 0, 1, 3, 36);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE), RecipeCategories.SMELTING);
        }
    }

    @SuppressWarnings("deprecation")
    public static String translateToLocal(String key)
    {
        if(I18n.canTranslate(key)) return I18n.translateToLocal(key);
        else return I18n.translateToFallback(key);
    }

    public static String translateToLocalFormatted(String key, Object... format)
    {
        String s = translateToLocal(key);
        try
        {
            return String.format(s, format);
        }
        catch (IllegalFormatException e)
        {
            return "Format error " + s;
        }
    }
}