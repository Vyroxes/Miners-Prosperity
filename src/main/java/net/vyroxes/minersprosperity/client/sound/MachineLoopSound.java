package net.vyroxes.minersprosperity.client.sound;

import net.vyroxes.minersprosperity.config.ModConfig;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.vyroxes.minersprosperity.tile.TileEntityMachine;

public class MachineLoopSound extends MovingSound {
    private final TileEntityMachine tileEntity;

    public MachineLoopSound(TileEntityMachine tileEntity, SoundEvent sound)
    {
        super(sound, SoundCategory.BLOCKS);

        this.tileEntity = tileEntity;

        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = tileEntity.getActiveSoundVolume();
        this.pitch = tileEntity.getActiveSoundPitch();

        this.xPosF = tileEntity.getPos().getX() + 0.5F;
        this.yPosF = tileEntity.getPos().getY() + 0.5F;
        this.zPosF = tileEntity.getPos().getZ() + 0.5F;
    }

    @Override
    public void update()
    {
        if (!ModConfig.MACHINES.machineSounds || tileEntity.getActiveSoundVolume() <= 0 || tileEntity.isInvalid() || !tileEntity.shouldPlayActiveSound())
        {
            donePlaying = true;
            return;
        }
        volume = tileEntity.getActiveSoundVolume();
        pitch = tileEntity.getActiveSoundPitch();
    }
}