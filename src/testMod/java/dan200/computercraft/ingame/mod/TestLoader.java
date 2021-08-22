/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.ingame.api.GameTest;
import dan200.computercraft.ingame.api.GameTestHelper;
import net.minecraft.test.TestFunctionInfo;
import net.minecraft.test.TestRegistry;
import net.minecraft.test.TestTrackerHolder;
import net.minecraft.util.Rotation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
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

        String className = klass.getSimpleName().toLowerCase( Locale.ROOT );
        String name = className + "." + method.getName().toLowerCase( Locale.ROOT );

        GameTest test = method.getAnnotation( GameTest.class );

        TestMod.log.info( "Adding test " + name );
        testClassNames.add( className );
        testFunctions.add( createTestFunction(
            test.batch(), name, name,
            test.required(),
            holder -> runTest( holder, method ),
            test.timeoutTicks(),
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
        TestFunctionInfo func = UnsafeHacks.newInstance( TestFunctionInfo.class );
        func.batchName = batchName;
        func.testName = testName;
        func.structureName = structureName;
        func.required = required;
        func.function = function;
        func.maxTicks = maxTicks;
        func.setupTicks = setupTicks;
        func.rotation = Rotation.NONE;
        return func;
    }

    private static void runTest( TestTrackerHolder holder, Method method )
    {
        GameTestHelper helper = new GameTestHelper( holder );
        try
        {
            method.invoke( method.getDeclaringClass().getConstructor().newInstance(), helper );
        }
        catch( Exception e )
        {
            helper.fail( e );
        }
    }
}
