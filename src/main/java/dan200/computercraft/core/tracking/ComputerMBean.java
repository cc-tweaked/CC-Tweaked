/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.tracking;

import com.google.common.base.CaseFormat;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.Computer;
import net.minecraft.locale.Language;

import javax.annotation.Nonnull;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class ComputerMBean implements DynamicMBean, Tracker
{
    private static final Set<TrackingField> SKIP = new HashSet<>( Arrays.asList(
        TrackingField.TASKS, TrackingField.TOTAL_TIME, TrackingField.AVERAGE_TIME, TrackingField.MAX_TIME,
        TrackingField.SERVER_COUNT, TrackingField.SERVER_TIME
    ) );

    private static ComputerMBean instance;

    private final Map<String, LongSupplier> attributes = new HashMap<>();
    private final Map<TrackingField, Counter> values = new HashMap<>();
    private final MBeanInfo info;

    private ComputerMBean()
    {
        List<MBeanAttributeInfo> attributes = new ArrayList<>();
        for( Map.Entry<String, TrackingField> field : TrackingField.fields().entrySet() )
        {
            if( SKIP.contains( field.getValue() ) ) continue;

            String name = CaseFormat.LOWER_UNDERSCORE.to( CaseFormat.LOWER_CAMEL, field.getKey() );
            add( name, field.getValue(), attributes, null );
        }

        add( "task", TrackingField.TOTAL_TIME, attributes, TrackingField.TASKS );
        add( "serverTask", TrackingField.SERVER_TIME, attributes, TrackingField.SERVER_COUNT );

        info = new MBeanInfo(
            ComputerMBean.class.getSimpleName(),
            "metrics about all computers on the server",
            attributes.toArray( new MBeanAttributeInfo[0] ), null, null, null
        );
    }

    public static void register()
    {
        try
        {
            ManagementFactory.getPlatformMBeanServer().registerMBean( instance = new ComputerMBean(), new ObjectName( "dan200.computercraft:type=Computers" ) );
        }
        catch( InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException e )
        {
            ComputerCraft.log.warn( "Failed to register JMX bean", e );
        }
    }

    public static void registerTracker()
    {
        if( instance != null ) Tracking.add( instance );
    }

    @Override
    public Object getAttribute( String attribute ) throws AttributeNotFoundException
    {
        LongSupplier value = attributes.get( attribute );
        if( value == null ) throw new AttributeNotFoundException();
        return value.getAsLong();
    }

    @Override
    public void setAttribute( Attribute attribute ) throws InvalidAttributeValueException
    {
        throw new InvalidAttributeValueException( "Cannot set attribute" );
    }

    @Override
    public AttributeList getAttributes( String[] attributes )
    {
        return null;
    }

    @Override
    public AttributeList setAttributes( AttributeList attributes )
    {
        return new AttributeList();
    }

    @Override
    public Object invoke( String actionName, Object[] params, String[] signature )
    {
        return null;
    }

    @Override
    @Nonnull
    public MBeanInfo getMBeanInfo()
    {
        return info;
    }

    @Override
    public void addTaskTiming( Computer computer, long time )
    {
        addValue( computer, TrackingField.TOTAL_TIME, time );
    }

    @Override
    public void addServerTiming( Computer computer, long time )
    {
        addValue( computer, TrackingField.SERVER_TIME, time );
    }

    @Override
    public void addValue( Computer computer, TrackingField field, long change )
    {
        Counter counter = values.get( field );
        counter.value.addAndGet( change );
        counter.count.incrementAndGet();
    }

    private MBeanAttributeInfo addAttribute( String name, String description, LongSupplier value )
    {
        attributes.put( name, value );
        return new MBeanAttributeInfo( name, "long", description, true, false, false );
    }

    private void add( String name, TrackingField field, List<MBeanAttributeInfo> attributes, TrackingField count )
    {
        Counter counter = new Counter();
        values.put( field, counter );

        String prettyName = Language.getInstance().getOrDefault( field.translationKey() );
        attributes.add( addAttribute( name, prettyName, counter.value::longValue ) );
        if( count != null )
        {
            String countName = Language.getInstance().getOrDefault( count.translationKey() );
            attributes.add( addAttribute( name + "Count", countName, counter.count::longValue ) );
        }
    }

    private static class Counter
    {
        final AtomicLong value = new AtomicLong();
        final AtomicLong count = new AtomicLong();
    }
}
