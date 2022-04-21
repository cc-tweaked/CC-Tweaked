/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The static entry point to the ComputerCraft API.
 *
 * Members in this class must be called after mod_ComputerCraft has been initialised, but may be called before it is
 * fully loaded.
 */
public final class ComputerCraftAPI
{
    public static final String MOD_ID = "computercraft";

    @Nonnull
    public static String getInstalledVersion()
    {
        return getInstance().getInstalledVersion();
    }

    /**
     * Creates a numbered directory in a subfolder of the save directory for a given world, and returns that number.
     *
     * Use in conjunction with createSaveDirMount() to create a unique place for your peripherals or media items to store files.
     *
     * @param world         The world for which the save dir should be created. This should be the server side world object.
     * @param parentSubPath The folder path within the save directory where the new directory should be created. eg: "computercraft/disk"
     * @return The numerical value of the name of the new folder, or -1 if the folder could not be created for some reason.
     *
     * eg: if createUniqueNumberedSaveDir( world, "computer/disk" ) was called returns 42, then "computer/disk/42" is now
     * available for writing.
     * @see #createSaveDirMount(Level, String, long)
     */
    public static int createUniqueNumberedSaveDir( @Nonnull Level world, @Nonnull String parentSubPath )
    {
        return getInstance().createUniqueNumberedSaveDir( world, parentSubPath );
    }

    /**
     * Creates a file system mount that maps to a subfolder of the save directory for a given world, and returns it.
     *
     * Use in conjunction with IComputerAccess.mount() or IComputerAccess.mountWritable() to mount a folder from the
     * users save directory onto a computers file system.
     *
     * @param world    The world for which the save dir can be found. This should be the server side world object.
     * @param subPath  The folder path within the save directory that the mount should map to. eg: "computer/disk/42".
     *                 Use createUniqueNumberedSaveDir() to create a new numbered folder to use.
     * @param capacity The amount of data that can be stored in the directory before it fills up, in bytes.
     * @return The mount, or null if it could be created for some reason. Use IComputerAccess.mount() or IComputerAccess.mountWritable()
     * to mount this on a Computers' file system.
     * @see #createUniqueNumberedSaveDir(Level, String)
     * @see IComputerAccess#mount(String, IMount)
     * @see IComputerAccess#mountWritable(String, IWritableMount)
     * @see IMount
     * @see IWritableMount
     */
    @Nullable
    public static IWritableMount createSaveDirMount( @Nonnull Level world, @Nonnull String subPath, long capacity )
    {
        return getInstance().createSaveDirMount( world, subPath, capacity );
    }

    /**
     * Creates a file system mount to a resource folder, and returns it.
     *
     * Use in conjunction with {@link IComputerAccess#mount} or {@link IComputerAccess#mountWritable} to mount a
     * resource folder onto a computer's file system.
     *
     * The files in this mount will be a combination of files in all mod jar, and data packs that contain
     * resources with the same domain and path. For instance, ComputerCraft's resources are stored in
     * "/data/computercraft/lua/rom". We construct a mount for that with
     * {@code createResourceMount("computercraft", "lua/rom")}.
     *
     * @param domain  The domain under which to look for resources. eg: "mymod".
     * @param subPath The subPath under which to look for resources. eg: "lua/myfiles".
     * @return The mount, or {@code null} if it could be created for some reason.
     * @see IComputerAccess#mount(String, IMount)
     * @see IComputerAccess#mountWritable(String, IWritableMount)
     * @see IMount
     */
    @Nullable
    public static IMount createResourceMount( @Nonnull String domain, @Nonnull String subPath )
    {
        return getInstance().createResourceMount( domain, subPath );
    }

    /**
     * Registers a peripheral provider to convert blocks into {@link IPeripheral} implementations.
     *
     * @param provider The peripheral provider to register.
     * @see IPeripheral
     * @see IPeripheralProvider
     */
    public static void registerPeripheralProvider( @Nonnull IPeripheralProvider provider )
    {
        getInstance().registerPeripheralProvider( provider );
    }

    /**
     * Registers a method source for generic peripherals.
     *
     * @param source The method source to register.
     * @see GenericSource
     */
    public static void registerGenericSource( @Nonnull GenericSource source )
    {
        getInstance().registerGenericSource( source );
    }

    /**
     * Registers a capability that can be used by generic peripherals.
     *
     * @param capability The capability to register.
     * @see GenericSource
     */
    public static void registerGenericCapability( @Nonnull Capability<?> capability )
    {
        getInstance().registerGenericCapability( capability );
    }

    /**
     * Registers a bundled redstone provider to provide bundled redstone output for blocks.
     *
     * @param provider The bundled redstone provider to register.
     * @see IBundledRedstoneProvider
     */
    public static void registerBundledRedstoneProvider( @Nonnull IBundledRedstoneProvider provider )
    {
        getInstance().registerBundledRedstoneProvider( provider );
    }

    /**
     * If there is a Computer or Turtle at a certain position in the world, get it's bundled redstone output.
     *
     * @param world The world this block is in.
     * @param pos   The position this block is at.
     * @param side  The side to extract the bundled redstone output from.
     * @return If there is a block capable of emitting bundled redstone at the location, it's signal (0-65535) will be returned.
     * If there is no block capable of emitting bundled redstone at the location, -1 will be returned.
     * @see IBundledRedstoneProvider
     */
    public static int getBundledRedstoneOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return getInstance().getBundledRedstoneOutput( world, pos, side );
    }

    /**
     * Registers a media provider to provide {@link IMedia} implementations for Items.
     *
     * @param provider The media provider to register.
     * @see IMediaProvider
     */
    public static void registerMediaProvider( @Nonnull IMediaProvider provider )
    {
        getInstance().registerMediaProvider( provider );
    }

    /**
     * Attempt to get the game-wide wireless network.
     *
     * @return The global wireless network, or {@code null} if it could not be fetched.
     */
    public static IPacketNetwork getWirelessNetwork()
    {
        return getInstance().getWirelessNetwork();
    }

    public static void registerAPIFactory( @Nonnull ILuaAPIFactory factory )
    {
        getInstance().registerAPIFactory( factory );
    }

    /**
     * Construct a new wired node for a given wired element.
     *
     * @param element The element to construct it for
     * @return The element's node
     * @see IWiredElement#getNode()
     */
    @Nonnull
    public static IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element )
    {
        return getInstance().createWiredNodeForElement( element );
    }

    /**
     * Get the wired network element for a block in world.
     *
     * @param world The world the block exists in
     * @param pos   The position the block exists in
     * @param side  The side to extract the network element from
     * @return The element's node
     * @see IWiredElement#getNode()
     */
    @Nonnull
    public static LazyOptional<IWiredElement> getWiredElementAt( @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return getInstance().getWiredElementAt( world, pos, side );
    }

    private static IComputerCraftAPI instance;

    @Nonnull
    private static IComputerCraftAPI getInstance()
    {
        if( instance != null ) return instance;

        try
        {
            return instance = (IComputerCraftAPI) Class.forName( "dan200.computercraft.ComputerCraftAPIImpl" )
                .getField( "INSTANCE" ).get( null );
        }
        catch( ReflectiveOperationException e )
        {
            throw new IllegalStateException( "Cannot find ComputerCraft API", e );
        }
    }

    public interface IComputerCraftAPI
    {
        @Nonnull
        String getInstalledVersion();

        int createUniqueNumberedSaveDir( @Nonnull Level world, @Nonnull String parentSubPath );

        @Nullable
        IWritableMount createSaveDirMount( @Nonnull Level world, @Nonnull String subPath, long capacity );

        @Nullable
        IMount createResourceMount( @Nonnull String domain, @Nonnull String subPath );

        void registerPeripheralProvider( @Nonnull IPeripheralProvider provider );

        void registerGenericSource( @Nonnull GenericSource source );

        void registerGenericCapability( @Nonnull Capability<?> capability );

        void registerBundledRedstoneProvider( @Nonnull IBundledRedstoneProvider provider );

        int getBundledRedstoneOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side );

        void registerMediaProvider( @Nonnull IMediaProvider provider );

        @Nonnull
        IPacketNetwork getWirelessNetwork();

        void registerAPIFactory( @Nonnull ILuaAPIFactory factory );

        @Nonnull
        IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element );

        @Nonnull
        LazyOptional<IWiredElement> getWiredElementAt( @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction side );
    }
}
