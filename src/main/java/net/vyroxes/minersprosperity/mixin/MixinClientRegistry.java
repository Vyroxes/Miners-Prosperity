package net.vyroxes.minersprosperity.mixin;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.vyroxes.minersprosperity.client.event.KeyBindingModNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientRegistry.class, remap = false)
public abstract class MixinClientRegistry
{
    @Inject(method = "registerKeyBinding", at = @At("HEAD"), remap = false)
    private static void rememberKeyOwner(KeyBinding key, CallbackInfo ci)
    {
        KeyBindingModNames.register(key);
    }
}