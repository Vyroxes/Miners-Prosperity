package net.vyroxes.minersprosperity.transport;

public enum ConduitType
{
    ENERGY("energy_conduit", 0xE94436),
    ITEM("item_duct", 0xD8AB52),
    FLUID("fluid_pipe", 0x369EDA),
    REDSTONE("redstone_conduit", 0xC83155),
    UNIVERSAL("universal_conduit", 0xBB65EB);

    public final String id;
    public final int color;

    ConduitType(String id, int color)
    {
        this.id = id;
        this.color = color;
    }

    public boolean supports(int resource)
    {
        return resource >= 0 && resource < 4 && (this == UNIVERSAL || ordinal() == resource);
    }
}