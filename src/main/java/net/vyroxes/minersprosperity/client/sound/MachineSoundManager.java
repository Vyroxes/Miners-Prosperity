package net.vyroxes.minersprosperity.client.sound;

import net.vyroxes.minersprosperity.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundEvent;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class MachineSoundManager {
    private static final Map<TileEntityMachine, WeakReference<MachineLoopSound>> SOUNDS = new WeakHashMap<>();

    public static void update(TileEntityMachine tileEntity)
    {
        if (!tileEntity.getWorld().isRemote) return;

        WeakReference<MachineLoopSound> reference = SOUNDS.get(tileEntity);
        MachineLoopSound currentSound = reference == null ? null : reference.get();

        if (ModConfig.MACHINES.machineSounds && tileEntity.shouldPlayActiveSound())
        {
            if (currentSound == null || currentSound.isDonePlaying())
            {
                SoundEvent sound = tileEntity.getActiveSound();

                if (sound == null || tileEntity.getActiveSoundVolume() <= 0) return;

                MachineLoopSound machineSound = new MachineLoopSound(tileEntity, sound);

                SOUNDS.put(tileEntity, new WeakReference<>(machineSound));
                Minecraft.getMinecraft().getSoundHandler().playSound(machineSound);
            }
        }
        else if (currentSound != null) SOUNDS.remove(tileEntity);
    }
}