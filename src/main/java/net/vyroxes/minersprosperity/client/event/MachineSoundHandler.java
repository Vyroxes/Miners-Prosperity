package net.vyroxes.minersprosperity.client.event;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class MachineSoundHandler
{
    private MachineSoundHandler()
    {
    }

    @SubscribeEvent
    public static void play(PlaySoundEvent event)
    {
        ISound sound = event.getResultSound();
        if (sound == null) return;
        ResourceLocation id = sound.getSoundLocation();
        if (!Tags.MODID.equals(id.getNamespace()) || !id.getPath().startsWith("machine.")) return;
        if (!ModConfig.MACHINES.machineSounds)
        {
            event.setResultSound(null);
            return;
        }
        if (!"machine.metal_press_clank".equals(id.getPath())) return;
        float multiplier = (float) (ModConfig.MACHINES.metalPressSoundVolume / 0.1);
        event.setResultSound(multiplier <= 0 ? null : new VolumeScaledSound(sound, multiplier));
    }

    // Resolve the delegate before reading volume and pitch; PlaySoundEvent fires before resolution.
    private static final class VolumeScaledSound implements ISound
    {
        private final ISound delegate;
        private final float multiplier;

        private VolumeScaledSound(ISound delegate, float multiplier)
        {
            this.delegate = delegate;
            this.multiplier = multiplier;
        }

        @Override
        public @NotNull ResourceLocation getSoundLocation()
        {
            return delegate.getSoundLocation();
        }

        @Override
        public SoundEventAccessor createAccessor(@NotNull SoundHandler handler)
        {
            return delegate.createAccessor(handler);
        }

        @Override
        public @NotNull Sound getSound()
        {
            return delegate.getSound();
        }

        @Override
        public @NotNull SoundCategory getCategory()
        {
            return delegate.getCategory();
        }

        @Override
        public boolean canRepeat()
        {
            return delegate.canRepeat();
        }

        @Override
        public int getRepeatDelay()
        {
            return delegate.getRepeatDelay();
        }

        @Override
        public float getVolume()
        {
            return delegate.getVolume() * multiplier;
        }

        @Override
        public float getPitch()
        {
            return delegate.getPitch();
        }

        @Override
        public float getXPosF()
        {
            return delegate.getXPosF();
        }

        @Override
        public float getYPosF()
        {
            return delegate.getYPosF();
        }

        @Override
        public float getZPosF()
        {
            return delegate.getZPosF();
        }

        @Override
        public @NotNull AttenuationType getAttenuationType()
        {
            return delegate.getAttenuationType();
        }
    }
}