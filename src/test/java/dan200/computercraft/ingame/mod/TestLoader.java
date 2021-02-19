/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import com.google.common.base.CaseFormat;
import dan200.computercraft.ingame.api.GameTest;
import net.minecraft.test.TestFunctionInfo;
import net.minecraft.test.TestRegistry;
import net.minecraft.test.TestTrackerHolder;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Loads methods annotated with {@link GameTest} and adds them to the {@link TestRegistry}. This involves some horrible
 * reflection hacks, as Proguard makes many methods (and constructors) private.
 */
class TestLoader
{
    private static final Type gameTest = Type.getType( GameTest.class );
    private static final Collection<TestFunctionInfo> testFunctions = ObfuscationReflectionHelper.getPrivateValue( TestRegistry.class, null, "field_229526_a_" );
    private static final Set<String> testClassNames = ObfuscationReflectionHelper.getPrivateValue( TestRegistry.class, null, "field_229527_b_" );

    public static void setup()
    {
        ModList.get().getAllScanData().stream()
            .flatMap( x -> x.getAnnotations().stream() )
            .filter( x -> x.getAnnotationType().equals( gameTest ) )
            .forEach( TestLoader::loadTest );
    }


    private static void loadTest( ModFileScanData.AnnotationData annotation )
    {
        Class<?> klass;
        Method method;
        try
        {
            klass = TestLoader.class.getClassLoader().loadClass( annotation.getClassType().getClassName() );

            // We don't know the exact signature (could suspend or not), so find something with the correct descriptor instead.
            String methodName = annotation.getMemberName();
            method = Arrays.stream( klass.getMethods() ).filter( x -> (x.getName() + Type.getMethodDescriptor( x )).equals( methodName ) ).findFirst()
                .orElseThrow( () -> new NoSuchMethodException( "No method " + annotation.getClassType().getClassName() + "." + annotation.getMemberName() ) );
        }
        catch( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }

        String className = CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_UNDERSCORE, klass.getSimpleName() );
        String name = className + "." + method.getName().toLowerCase().replace( ' ', '_' );

        GameTest test = method.getAnnotation( GameTest.class );

        TestMod.log.info( "Adding test " + name );
        testClassNames.add( className );
        testFunctions.add( createTestFunction(
            test.batch(), name, name,
            test.required(),
            new TestRunner( name, method ),
            test.timeout(),
            test.setup()
        ) );
    }

    private static TestFunctionInfo createTestFunction(
        String batchName,
        String testName,
        String structureName,
        boolean required,
        Consumer<TestTrackerHolder> function,
        int maxTicks,
        long setupTicks
    )
    {
        try
        {
            TestFunctionInfo func = UnsafeHacks.newInstance( TestFunctionInfo.class );
            setFinalField( func, "batchName", batchName );
            setFinalField( func, "testName", testName );
            setFinalField( func, "structureName", structureName );
            setFinalField( func, "required", required );
            setFinalField( func, "function", function );
            setFinalField( func, "maxTicks", maxTicks );
            setFinalField( func, "setupTicks", setupTicks );
            return func;
        }
        catch( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void setFinalField( TestFunctionInfo func, String name, Object value ) throws ReflectiveOperationException
    {
        Field field = TestFunctionInfo.class.getDeclaredField( name );
        if( (field.getModifiers() & Modifier.FINAL) != 0 )
        {
            Field modifiers = Field.class.getDeclaredField( "modifiers" );
            modifiers.setAccessible( true );
            modifiers.set( field, field.getModifiers() & ~Modifier.FINAL );
        }

        field.setAccessible( true );
        field.set( func, value );
    }
}
