package net.vyroxes.minersprosperity.transport;

public enum ConduitType
{
    ENERGY("energy_conduit", 0xE94436),
    ITEM("item_duct", 0xD8AB52),
    FLUID("fluid_pipe", 0x369EDA),
    REDSTONE("redstone_conduit", 0xC83155),
    UNIVERSAL("universal_conduit", 0xFF7E00),
    LOGIC("logic_cable", 0x00FF01);

    public static final int LOGIC_RESOURCE = 4;
    public static final int RESOURCE_COUNT = 5;

    public final String id;
    public final int color;

    ConduitType(String id, int color)
    {
        this.id = id;
        this.color = color;
    }

    public int resourceIndex()
    {
        return this == LOGIC ? LOGIC_RESOURCE : ordinal();
    }

    public boolean isTiered()
    {
        return this == ENERGY || this == ITEM || this == FLUID;
    }

    public boolean supports(int resource)
    {
        return resource >= 0 && resource < RESOURCE_COUNT && (this == UNIVERSAL || resourceIndex() == resource);
    }
}