/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dan200.computercraft.ComputerCraft;

import java.util.concurrent.ThreadFactory;

/**
 * Provides some utilities to create thread groups.
 */
public final class ThreadUtils
{
    private static final ThreadGroup baseGroup = new ThreadGroup( "ComputerCraft" );

    private ThreadUtils()
    {
    }

    /**
     * Get the base thread group, that all off-thread ComputerCraft activities are run on.
     *
     * @return The ComputerCraft group.
     */
    public static ThreadGroup group()
    {
        return baseGroup;
    }

    /**
     * Construct a group under ComputerCraft's shared group.
     *
     * @param name The group's name. This will be prefixed with "ComputerCraft-".
     * @return The constructed thread group.
     */
    public static ThreadGroup group( String name )
    {
        return new ThreadGroup( baseGroup, baseGroup.getName() + "-" + name );
    }

    /**
     * Create a new {@link ThreadFactoryBuilder}, which constructs threads under a group of the given {@code name}.
     *
     * Each thread will be of the format {@code ComputerCraft-<name>-<number>}, and belong to a group
     * called {@code ComputerCraft-<name>} (which in turn will be a child group of the main {@code ComputerCraft} group.
     *
     * @param name The name for the thread group and child threads.
     * @return The constructed thread factory builder, which may be extended with other properties.
     * @see #factory(String)
     */
    public static ThreadFactoryBuilder builder( String name )
    {
        ThreadGroup group = group( name );
        return new ThreadFactoryBuilder()
            .setDaemon( true )
            .setNameFormat( group.getName().replace( "%", "%%" ) + "-%d" )
            .setUncaughtExceptionHandler( ( t, e ) -> ComputerCraft.log.error( "Exception in thread " + t.getName(), e ) )
            .setThreadFactory( x -> new Thread( group, x ) );
    }

    /**
     * Create a new {@link ThreadFactory}, which constructs threads under a group of the given {@code name}.
     *
     * Each thread will be of the format {@code ComputerCraft-<name>-<number>}, and belong to a group
     * called {@code ComputerCraft-<name>} (which in turn will be a child group of the main {@code ComputerCraft} group.
     *
     * @param name The name for the thread group and child threads.
     * @return The constructed thread factory.
     * @see #builder(String)
     */
    public static ThreadFactory factory( String name )
    {
        return builder( name ).build();
    }
}
