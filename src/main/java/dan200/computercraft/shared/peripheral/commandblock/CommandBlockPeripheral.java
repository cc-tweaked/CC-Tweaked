/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.commandblock;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

/**
 * This peripheral allows you to interact with command blocks.
 *
 * Command blocks are only wrapped as peripherals if the {@code enable_command_block} option is true within the
 * config.
 *
 * This API is <em>not</em> the same as the {@link CommandAPI} API, which is exposed on command computers.
 *
 * @cc.module command
 */
@Mod.EventBusSubscriber
public class CommandBlockPeripheral implements IPeripheral, ICapabilityProvider
{
    private static final ResourceLocation CAP_ID = new ResourceLocation( ComputerCraft.MOD_ID, "command_block" );

    private final CommandBlockEntity commandBlock;
    private LazyOptional<IPeripheral> self;

    public CommandBlockPeripheral( CommandBlockEntity commandBlock )
    {
        this.commandBlock = commandBlock;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "command";
    }

    /**
     * Get the command this command block will run.
     *
     * @return The current command.
     */
    @LuaFunction( mainThread = true )
    public final String getCommand()
    {
        return commandBlock.getCommandBlock().getCommand();
    }

    /**
     * Set the command block's command.
     *
     * @param command The new command.
     */
    @LuaFunction( mainThread = true )
    public final void setCommand( String command )
    {
        commandBlock.getCommandBlock().setCommand( command );
        commandBlock.getCommandBlock().onUpdated();
    }

    /**
     * Execute the command block once.
     *
     * @return The result of executing.
     * @cc.treturn boolean If the command completed successfully.
     * @cc.treturn string|nil A failure message.
     */
    @LuaFunction( mainThread = true )
    public final Object[] runCommand()
    {
        commandBlock.getCommandBlock().performCommand( commandBlock.getLevel() );
        int result = commandBlock.getCommandBlock().getSuccessCount();
        return result > 0 ? new Object[] { true } : new Object[] { false, "Command failed" };
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other != null && other.getClass() == getClass();
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return commandBlock;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( self == null ) self = LazyOptional.of( () -> this );
            return self.cast();
        }
        return LazyOptional.empty();
    }

    private void invalidate()
    {
        self = CapabilityUtil.invalidate( self );
    }

    @SubscribeEvent
    public static void onCapability( AttachCapabilitiesEvent<BlockEntity> event )
    {
        BlockEntity tile = event.getObject();
        if( ComputerCraft.enableCommandBlock && tile instanceof CommandBlockEntity )
        {
            CommandBlockPeripheral peripheral = new CommandBlockPeripheral( (CommandBlockEntity) tile );
            event.addCapability( CAP_ID, peripheral );
            event.addListener( peripheral::invalidate );
        }
    }
}
