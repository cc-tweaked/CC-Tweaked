/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest.core;

import dan200.computercraft.export.Exporter;
import dan200.computercraft.gametest.api.GameTestHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

@Mod( "cctest" )
public class TestMod
{
    public TestMod()
    {
        TestHooks.init();

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener( EventPriority.LOW, ( ServerStartedEvent e ) -> TestHooks.onServerStarted( e.getServer() ) );
        bus.addListener( ( RegisterCommandsEvent e ) -> CCTestCommand.register( e.getDispatcher() ) );
        bus.addListener( ( RegisterClientCommandsEvent e ) -> Exporter.register( e.getDispatcher() ) );

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener( ( RegisterGameTestsEvent event ) -> {
            var holder = Type.getType( GameTestHolder.class );
            ModList.get().getAllScanData().stream()
                .map( ModFileScanData::getAnnotations )
                .flatMap( Collection::stream )
                .filter( a -> holder.equals( a.annotationType() ) )
                .forEach( x -> registerClass( x.clazz().getClassName(), event::register ) );
        } );
    }


    private static Class<?> loadClass( String name )
    {
        try
        {
            return Class.forName( name, true, TestMod.class.getClassLoader() );
        }
        catch( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void registerClass( String className, Consumer<Method> fallback )
    {
        var klass = loadClass( className );
        for( var method : klass.getDeclaredMethods() )
        {
            var testInfo = method.getAnnotation( GameTest.class );
            if( testInfo == null )
            {
                fallback.accept( method );
                continue;
            }

            GameTestRegistry.getAllTestFunctions().add( turnMethodIntoTestFunction( method, testInfo ) );
            GameTestRegistry.getAllTestClassNames().add( className );
        }
    }

    /**
     * Custom implementation of {@link GameTestRegistry#turnMethodIntoTestFunction(Method)} which makes
     * {@link GameTest#template()} behave the same as Fabric, namely in that it points to a {@link ResourceLocation},
     * rather than a test-class-specific structure.
     * <p>
     * This effectively acts as a global version of {@link PrefixGameTestTemplate}, just one which doesn't require Forge
     * to be present.
     *
     * @param method   The method to register.
     * @param testInfo The test info.
     * @return The constructed test function.
     */
    private static TestFunction turnMethodIntoTestFunction( Method method, GameTest testInfo )
    {
        var className = method.getDeclaringClass().getSimpleName().toLowerCase( Locale.ROOT );
        var testName = className + "." + method.getName().toLowerCase( Locale.ROOT );
        return new TestFunction(
            testInfo.batch(),
            testName,
            testInfo.template().isEmpty() ? testName : testInfo.template(),
            StructureUtils.getRotationForRotationSteps( testInfo.rotationSteps() ), testInfo.timeoutTicks(), testInfo.setupTicks(),
            testInfo.required(), testInfo.requiredSuccesses(), testInfo.attempts(),
            turnMethodIntoConsumer( method )
        );
    }

    private static <T> Consumer<T> turnMethodIntoConsumer( Method method )
    {
        return value -> {
            try
            {
                Object instance = null;
                if( !Modifier.isStatic( method.getModifiers() ) )
                {
                    instance = method.getDeclaringClass().getConstructor().newInstance();
                }

                method.invoke( instance, value );
            }
            catch( InvocationTargetException e )
            {
                if( e.getCause() instanceof RuntimeException )
                {
                    throw (RuntimeException) e.getCause();
                }
                else
                {
                    throw new RuntimeException( e.getCause() );
                }
            }
            catch( ReflectiveOperationException e )
            {
                throw new RuntimeException( e );
            }
        };
    }
}
