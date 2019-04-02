/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import dan200.computercraft.ComputerCraft;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.util.ResourceLocation;

public final class ArgumentSerializers
{
    @SuppressWarnings( "unchecked" )
    private static <T extends ArgumentType<?>> void registerUnsafe( ResourceLocation id, Class<T> type, IArgumentSerializer<?> serializer )
    {
        ArgumentTypes.register( id, type, (IArgumentSerializer<T>) serializer );
    }

    private static <T extends ArgumentType<?>> void register( ResourceLocation id, Class<T> type, IArgumentSerializer<T> serializer )
    {
        ArgumentTypes.register( id, type, serializer );
    }

    private static <T extends ArgumentType<?>> void register( ResourceLocation id, T instance )
    {
        registerUnsafe( id, instance.getClass(), new ArgumentSerializer<>( () -> instance ) );
    }

    public static void register()
    {
        register( new ResourceLocation( ComputerCraft.MOD_ID, "tracking_field" ), TrackingFieldArgumentType.trackingField() );
        register( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ), ComputerArgumentType.oneComputer() );
        register( new ResourceLocation( ComputerCraft.MOD_ID, "computers" ), ComputersArgumentType.class, new ComputersArgumentType.Serializer() );
        registerUnsafe( new ResourceLocation( ComputerCraft.MOD_ID, "repeat" ), RepeatArgumentType.class, new RepeatArgumentType.Serializer() );
    }
}
