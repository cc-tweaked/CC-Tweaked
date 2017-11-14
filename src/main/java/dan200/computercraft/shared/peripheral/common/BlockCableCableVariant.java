package dan200.computercraft.shared.peripheral.common;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum BlockCableCableVariant implements IStringSerializable
{
    NONE( "none" ),
    ANY( "any" ),
    X_AXIS( "x" ),
    Y_AXIS( "y" ),
    Z_AXIS( "z" ),;

    private final String m_name;

    BlockCableCableVariant( String name )
    {
        m_name = name;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return m_name;
    }
}
