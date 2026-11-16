package net.vyroxes.minersprosperity.storage;

import net.vyroxes.minersprosperity.config.ModConfig;

public enum StorageCrateTier
{
    BASIC,
    IMPROVED,
    ADVANCED,
    ELITE,
    ULTIMATE,
    SUPREME,
    CREATIVE;

    public int stacks()
    {
        return this == CREATIVE
                ? Integer.MAX_VALUE
                : ModConfig.tierValue(
                        ModConfig.BLOCKS.storageCrateStacks, ordinal(), 8 << (2 * ordinal()), 1);
    }
}