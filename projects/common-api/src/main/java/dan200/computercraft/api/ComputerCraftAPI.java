// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api;

import dan200.computercraft.api.component.ComputerComponent;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaProvider;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.redstone.BundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.impl.ComputerCraftAPIService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The static entry point to the ComputerCraft API.
 * <p>
 * Members in this class must be called after ComputerCraft has been initialised, but may be called before it is
 * fully loaded.
 */
public final class ComputerCraftAPI {
    public static final String MOD_ID = "computercraft";

    public static String getInstalledVersion() {
        return getInstance().getInstalledVersion();
    }

    /**
     * Creates a numbered directory in a subfolder of the save directory for a given world, and returns that number.
     * <p>
     * Use in conjunction with createSaveDirMount() to create a unique place for your peripherals or media items to store files.
     *
     * @param server        The server for which the save dir should be created.
     * @param parentSubPath The folder path within the save directory where the new directory should be created. eg: "computercraft/disk"
     * @return The numerical value of the name of the new folder, or -1 if the folder could not be created for some reason.
     * <p>
     * eg: if createUniqueNumberedSaveDir( world, "computer/disk" ) was called returns 42, then "computer/disk/42" is now
     * available for writing.
     * @see #createSaveDirMount(MinecraftServer, String, long)
     */
    public static int createUniqueNumberedSaveDir(MinecraftServer server, String parentSubPath) {
        return getInstance().createUniqueNumberedSaveDir(server, parentSubPath);
    }

    /**
     * Creates a file system mount that maps to a subfolder of the save directory for a given world, and returns it.
     * <p>
     * Use in conjunction with Use {@link IComputerAccess#mount(String, Mount)} or {@link IComputerAccess#mountWritable(String, WritableMount)}
     * to mount this on a computer's file system.
     * <p>
     * If the same folder may be mounted on multiple computers at once (for instance, if you provide a network file share),
     * the same mount instance should be used for all computers. You should NOT have multiple mount instances for the
     * same folder.
     *
     * @param server   The server which the save dir can be found.
     * @param subPath  The folder path within the save directory that the mount should map to. eg: "disk/42".
     *                 Use {@link #createUniqueNumberedSaveDir(MinecraftServer, String)} to create a new numbered folder
     *                 to use.
     * @param capacity The amount of data that can be stored in the directory before it fills up, in bytes.
     * @return The newly created mount.
     * @see #createUniqueNumberedSaveDir(MinecraftServer, String)
     * @see IComputerAccess#mount(String, Mount)
     * @see IComputerAccess#mountWritable(String, WritableMount)
     * @see Mount
     * @see WritableMount
     */
    public static WritableMount createSaveDirMount(MinecraftServer server, String subPath, long capacity) {
        return getInstance().createSaveDirMount(server, subPath, capacity);
    }

    /**
     * Creates a file system mount to a resource folder, and returns it.
     * <p>
     * Use in conjunction with {@link IComputerAccess#mount} or {@link IComputerAccess#mountWritable} to mount a
     * resource folder onto a computer's file system.
     * <p>
     * The files in this mount will be a combination of files in all mod jar, and data packs that contain
     * resources with the same domain and path. For instance, ComputerCraft's resources are stored in
     * "/data/computercraft/lua/rom". We construct a mount for that with
     * {@code createResourceMount("computercraft", "lua/rom")}.
     *
     * @param server  The current Minecraft server, from which to read resources from.
     * @param domain  The domain under which to look for resources. eg: "mymod".
     * @param subPath The subPath under which to look for resources. eg: "lua/myfiles".
     * @return The mount, or {@code null} if it could be created for some reason.
     * @see IComputerAccess#mount(String, Mount)
     * @see IComputerAccess#mountWritable(String, WritableMount)
     * @see Mount
     */
    @Nullable
    public static Mount createResourceMount(MinecraftServer server, String domain, String subPath) {
        return getInstance().createResourceMount(server, domain, subPath);
    }

    /**
     * Registers a method source for generic peripherals.
     *
     * @param source The method source to register.
     * @see GenericSource
     */
    public static void registerGenericSource(GenericSource source) {
        getInstance().registerGenericSource(source);
    }

    /**
     * Registers a bundled redstone provider to provide bundled redstone output for blocks.
     *
     * @param provider The bundled redstone provider to register.
     * @see BundledRedstoneProvider
     */
    public static void registerBundledRedstoneProvider(BundledRedstoneProvider provider) {
        getInstance().registerBundledRedstoneProvider(provider);
    }

    /**
     * If there is a Computer or Turtle at a certain position in the world, get it's bundled redstone output.
     *
     * @param world The world this block is in.
     * @param pos   The position this block is at.
     * @param side  The side to extract the bundled redstone output from.
     * @return If there is a block capable of emitting bundled redstone at the location, it's signal (0-65535) will be returned.
     * If there is no block capable of emitting bundled redstone at the location, -1 will be returned.
     * @see BundledRedstoneProvider
     */
    public static int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        return getInstance().getBundledRedstoneOutput(world, pos, side);
    }

    /**
     * Registers a media provider to provide {@link IMedia} implementations for Items.
     *
     * @param provider The media provider to register.
     * @see MediaProvider
     */
    public static void registerMediaProvider(MediaProvider provider) {
        getInstance().registerMediaProvider(provider);
    }

    /**
     * Attempt to get the game-wide wireless network.
     *
     * @param server The current Minecraft server.
     * @return The global wireless network, or {@code null} if it could not be fetched.
     */
    public static PacketNetwork getWirelessNetwork(MinecraftServer server) {
        return getInstance().getWirelessNetwork(server);
    }

    /**
     * Register a custom {@link ILuaAPI}, which may be added onto all computers without requiring a peripheral.
     * <p>
     * Before implementing this interface, consider alternative methods of providing methods. It is generally preferred
     * to use peripherals to provide functionality to users. If an API is <em>required</em>, you may want to consider
     * using {@link ILuaAPI#getModuleName()} to expose this library as a module instead of as a global.
     * <p>
     * This may be used with {@link IComputerSystem#getComponent(ComputerComponent)} to only attach APIs to specific
     * computers. For example, one can add an additional API just to turtles with the following code:
     *
     * <pre>{@code
     * ComputerCraftAPI.registerAPIFactory(computer -> {
     *   // Read the turtle component.
     *   var turtle = computer.getComponent(ComputerComponents.TURTLE);
     *   // If present then add our API.
     *   return turtle == null ? null : new MyCustomTurtleApi(turtle);
     * });
     * }</pre>
     *
     * @param factory The factory for your API subclass.
     * @see ILuaAPIFactory
     */
    public static void registerAPIFactory(ILuaAPIFactory factory) {
        getInstance().registerAPIFactory(factory);
    }

    /**
     * Construct a new wired node for a given wired element.
     *
     * @param element The element to construct it for
     * @return The element's node
     * @see WiredElement#getNode()
     */
    public static WiredNode createWiredNodeForElement(WiredElement element) {
        return getInstance().createWiredNodeForElement(element);
    }

    /**
     * Register a refuel handler for turtles. This may be used to provide alternative fuel sources, such as consuming RF
     * batteries.
     *
     * @param handler The turtle refuel handler.
     * @see TurtleRefuelHandler#refuel(ITurtleAccess, ItemStack, int, int)
     */
    public static void registerRefuelHandler(TurtleRefuelHandler handler) {
        getInstance().registerRefuelHandler(handler);
    }

    private static ComputerCraftAPIService getInstance() {
        return ComputerCraftAPIService.get();
    }
}
