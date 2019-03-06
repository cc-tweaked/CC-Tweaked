/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.TranslatableTextComponent;

public class Exceptions
{
    public static final DynamicCommandExceptionType COMPUTER_SELECTOR_NONE = translated1( "command.computercraft.computer_selector_none" );
    public static final DynamicCommandExceptionType COMPUTER_SELECTOR_MANY = translated1( "command.computercraft.computer_selector_many" );

    public static final SimpleCommandExceptionType NOT_TRACKING_EXCEPTION = translated( "command.computercraft.not_tracking" );
    public static final SimpleCommandExceptionType NO_TIMINGS_EXCEPTION = translated( "command.computercraft.no_timings" );
    public static final DynamicCommandExceptionType UNKNOWN_TRACKING_FIELD = translated1( "command.computercraft.unknown_tracking_field" );

    public static final SimpleCommandExceptionType UNLOCATED_COMPUTER_EXCEPTION = translated( "command.computercraft.unlocated_computer" );

    public static final SimpleCommandExceptionType ARGUMENT_EXPECTED = translated( "command.computercraft.argument_expected" );

    private static SimpleCommandExceptionType translated( String key )
    {
        return new SimpleCommandExceptionType( new TranslatableTextComponent( key ) );
    }

    private static DynamicCommandExceptionType translated1( String key )
    {
        return new DynamicCommandExceptionType( x -> new TranslatableTextComponent( key, x ) );
    }
}
