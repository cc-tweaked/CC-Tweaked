package dan200.computercraft.core.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;

public class TrackingField
{
    private static final Map<String, TrackingField> fields = new HashMap<>();

    public static final TrackingField TASKS = TrackingField.of( "tasks", "Tasks", x -> String.format( "%4d", x ) );
    public static final TrackingField TOTAL_TIME = TrackingField.of( "total", "Total time", x -> String.format( "%7.1fms", x / 1e6 ) );
    public static final TrackingField AVERAGE_TIME = TrackingField.of( "average", "Average time", x -> String.format( "%4.1fms", x / 1e6 ) );
    public static final TrackingField MAX_TIME = TrackingField.of( "max", "Max time", x -> String.format( "%5.1fms", x / 1e6 ) );

    public static final TrackingField PERIPHERAL_OPS = TrackingField.of( "peripheral", "Peripheral calls", x -> String.format( "%6d", x ) );
    public static final TrackingField FS_OPS = TrackingField.of( "fs", "Filesystem operations", x -> String.format( "%6d", x ) );
    public static final TrackingField TURTLE_OPS = TrackingField.of( "turtle", "Turtle operations", x -> String.format( "%6d", x ) );

    private final String id;
    private final String displayName;
    private final LongFunction<String> format;

    public String id()
    {
        return id;
    }

    public String displayName()
    {
        return displayName;
    }

    private TrackingField( String id, String displayName, LongFunction<String> format )
    {
        this.id = id;
        this.displayName = displayName;
        this.format = format;
    }

    public String format( long value )
    {
        return format.apply( value );
    }

    public static TrackingField of( String id, String displayName, LongFunction<String> format )
    {
        TrackingField field = new TrackingField( id, displayName, format );
        fields.put( id, field );
        return field;
    }

    public static Map<String, TrackingField> fields()
    {
        return Collections.unmodifiableMap( fields );
    }
}
