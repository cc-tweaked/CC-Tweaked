/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Loads methods annotated with {@link GameTest} and adds them to the {@link GameTestRegistry}.
 */
class TestLoader
{
    private static final Type gameTest = Type.getType( GameTest.class );

    public static void setup()
    {
        ModList.get().getAllScanData().stream()
            .flatMap( x -> x.getAnnotations().stream() )
            .filter( x -> x.annotationType().equals( gameTest ) )
            .forEach( TestLoader::loadTest );
    }

    private static void loadTest( ModFileScanData.AnnotationData annotation )
    {
        Class<?> klass;
        Method method;
        try
        {
            klass = TestLoader.class.getClassLoader().loadClass( annotation.clazz().getClassName() );

            // We don't know the exact signature (could suspend or not), so find something with the correct descriptor instead.
            String methodName = annotation.memberName();
            method = Arrays.stream( klass.getMethods() ).filter( x -> (x.getName() + Type.getMethodDescriptor( x )).equals( methodName ) ).findFirst()
                .orElseThrow( () -> new NoSuchMethodException( "No method " + annotation.clazz().getClassName() + "." + annotation.memberName() ) );
        }
        catch( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }

        GameTestRegistry.register( method );
    }
}
