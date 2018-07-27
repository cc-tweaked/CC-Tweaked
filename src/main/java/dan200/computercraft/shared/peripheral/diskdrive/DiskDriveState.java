package dan200.computercraft.shared.peripheral.diskdrive;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum DiskDriveState implements IStringSerializable
{
    EMPTY( "empty" ),
    FULL( "full" ),
    INVALID( "invalid" );

    public static final DiskDriveState[] VALUES = values();

    private final String name;

    DiskDriveState( String name )
    {
        this.name = name;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return name;
    }

    public static DiskDriveState of( int index )
    {
        return index >= 0 && index < VALUES.length ? VALUES[index] : EMPTY;
    }
}
