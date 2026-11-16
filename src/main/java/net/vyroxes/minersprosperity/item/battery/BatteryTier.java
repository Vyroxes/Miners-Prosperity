package net.vyroxes.minersprosperity.item.battery;

public enum BatteryTier
{
    BASIC("basic_battery", 20000, 200,200),
    IMPROVED("improved_solar_panel", 80000, 800, 800),
    ADVANCED("advanced_solar_panel", 320000, 3200, 3200),
    ELITE("elite_solar_panel", 1280000, 12800, 12800),
    ULTIMATE("ultimate_solar_panel", 5120000, 51200, 51200),
    SUPREME("supreme_solar_panel", 20480000, 204800, 204800),
    CREATIVE("creative_solar_panel", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public final String translationKey;
    public final int capacity;
    public final int maxExtract;
    public final int maxReceive;

    BatteryTier(String translationKey, int capacity, int maxExtract, int maxReceive)
    {
        this.translationKey = translationKey;
        this.capacity = capacity;
        this.maxExtract = maxExtract;
        this.maxReceive = maxReceive;
    }
    public int getCapacity()
    {
        if (this == CREATIVE) return capacity;
        return net.vyroxes.minersprosperity.config.ModConfig.tierValue(net.vyroxes.minersprosperity.config.ModConfig.ITEMS.batteryCapacity, ordinal(), capacity, 1);
    }

    public int getMaxReceive()
    {
        if (this == CREATIVE) return maxReceive;
        return net.vyroxes.minersprosperity.config.ModConfig.tierValue(net.vyroxes.minersprosperity.config.ModConfig.ITEMS.batteryMaxReceive, ordinal(), maxReceive, 0);
    }

    public int getMaxExtract()
    {
        if (this == CREATIVE) return maxExtract;
        return net.vyroxes.minersprosperity.config.ModConfig.tierValue(net.vyroxes.minersprosperity.config.ModConfig.ITEMS.batteryMaxExtract, ordinal(), maxExtract, 0);
    }
}