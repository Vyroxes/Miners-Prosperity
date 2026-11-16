package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLContainerHolder;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileNotFoundException;
import java.io.InputStream;

@Mixin(AbstractResourcePack.class)
public abstract class MixinVanillaResourceOverrides
{
    @Unique
    private boolean minersprosperity$blocked(ResourceLocation resource)
    {
        if (ModConfig.OTHER.vanillaResourceOverrides || !"minecraft".equals(resource.getNamespace())) return false;
        if (!((Object) this instanceof FMLContainerHolder holder) || !Tags.MODID.equals(holder.getFMLContainer().getModId())) return false;
        String path = resource.getPath();
        return path.startsWith("lang/") || path.startsWith("models/") || path.startsWith("textures/");
    }

    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    private void hideVanillaOverrides(ResourceLocation resource, CallbackInfoReturnable<Boolean> cir)
    {
        if (minersprosperity$blocked(resource)) cir.setReturnValue(false);
    }

    @Inject(method = "getInputStream", at = @At("HEAD"), cancellable = true)
    private void preventVanillaOverrideRead(ResourceLocation resource, CallbackInfoReturnable<InputStream> cir) throws FileNotFoundException
    {
        if (minersprosperity$blocked(resource)) throw new FileNotFoundException(resource.toString());
    }
}