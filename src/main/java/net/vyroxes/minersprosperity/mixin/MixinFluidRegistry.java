package net.vyroxes.minersprosperity.mixin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.vyroxes.minersprosperity.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = FluidRegistry.class, remap = false)
public abstract class MixinFluidRegistry
{
    @Shadow
    static BiMap<String, String> defaultFluidName;

    @Shadow
    private static void loadFluidDefaults(BiMap<Fluid, Integer> localFluidIDs, Set<String> defaultNames)
    {
        throw new AssertionError();
    }

    @Inject(method = "registerFluid", at = @At("RETURN"), require = 1)
    private static void preferRegisteredModFluid(Fluid fluid, CallbackInfoReturnable<Boolean> callback)
    {
        if (callback.getReturnValueZ()) return;

        String preferred = FluidRegistry.getDefaultFluidName(fluid);
        if (!preferred.startsWith(Tags.MODID + ":")) return;

        // False means Forge kept our fluid as an alternative. Rebind through Forge to retain the existing numeric ID.
        defaultFluidName.put(fluid.getName(), preferred);
        loadFluidDefaults(HashBiMap.create(FluidRegistry.getRegisteredFluidIDs()), Collections.singleton(preferred));
    }

    @ModifyArg(method = "loadFluidDefaults(Lnet/minecraft/nbt/NBTTagCompound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidRegistry;loadFluidDefaults(Lcom/google/common/collect/BiMap;Ljava/util/Set;)V"),
            index = 1, require = 1)
    private static Set<String> preferSavedModFluids(Set<String> defaults)
    {
        if (defaults.isEmpty()) return defaults;

        // Patch the world-loading overload only. Clients still receive the server's choice.
        Set<String> preferredDefaults = new HashSet<>(defaults);
        preferredDefaults.removeIf(name -> {
            String preferred = defaultFluidName.get(name.substring(name.indexOf(':') + 1));
            return preferred != null && preferred.startsWith(Tags.MODID + ":") && !preferred.equals(name);
        });
        for (String preferred : defaultFluidName.values())
        {
            if (preferred.startsWith(Tags.MODID + ":")) preferredDefaults.add(preferred);
        }
        return preferredDefaults;
    }
}