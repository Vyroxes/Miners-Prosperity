package net.vyroxes.minersprosperity.transport;

public enum ConduitTier
{
    BASIC(0xBA8E68),
    IMPROVED(0xAEAEAE),
    ADVANCED(0x38CF49),
    ELITE(0xD0D54B),
    ULTIMATE(0xD94A4A),
    SUPREME(0x16C6EF);

    public final int color;

    ConduitTier(int color)
    {
        this.color = color;
    }

    public int travelTicks()
    {
        int[] ticks = net.vyroxes.minersprosperity.config.ModConfig.CONDUITS.transportTicksPerBlock;
        return ticks.length == 0 ? 1 : Math.max(1, ticks[Math.min(ordinal(), ticks.length - 1)]);
    }

    public int rate(int resource)
    {
        var config = net.vyroxes.minersprosperity.config.ModConfig.CONDUITS;
        int[] rates = resource == 0 ? config.energyRates : resource == 1 ? config.itemRates : config.fluidRates;
        return rates.length == 0 ? 1 : Math.max(1, rates[Math.min(ordinal(), rates.length - 1)]);
    }
}