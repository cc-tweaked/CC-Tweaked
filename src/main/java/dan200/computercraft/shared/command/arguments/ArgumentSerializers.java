/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.ComputerCraft;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;

public final class ArgumentSerializers
{
    @SuppressWarnings( "unchecked" )
    private static <T extends ArgumentType<?>> void registerUnsafe( Identifier id, Class<T> type, ArgumentSerializer<?> serializer )
    {
        ArgumentTypes.register( id.toString(), type, (ArgumentSerializer<T>) serializer );
    }

    private static <T extends ArgumentType<?>> void register( Identifier id, Class<T> type, ArgumentSerializer<T> serializer )
    {
        ArgumentTypes.register( id.toString(), type, serializer );
    }

    private static <T extends ArgumentType<?>> void register( Identifier id, T instance )
    {
        registerUnsafe( id, instance.getClass(), new ConstantArgumentSerializer<>( () -> instance ) );
    }

    public static void register()
    {
        register( new Identifier( ComputerCraft.MOD_ID, "tracking_field" ), TrackingFieldArgumentType.trackingField() );
        register( new Identifier( ComputerCraft.MOD_ID, "computer" ), ComputerArgumentType.oneComputer() );
        register( new Identifier( ComputerCraft.MOD_ID, "computers" ), ComputersArgumentType.class, new ComputersArgumentType.Serializer() );
        registerUnsafe( new Identifier( ComputerCraft.MOD_ID, "repeat" ), RepeatArgumentType.class, new RepeatArgumentType.Serializer() );
    }
}
