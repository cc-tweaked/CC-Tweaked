package dan200.computercraft.shared.peripheral.redstoneIntegrator;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.squiddev.plethora.gameplay.Plethora;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * We extends {@link BlockGeneric} as the bundled redstone provider requires it:
 * {@link ComputerCraftAPI#getBundledRedstoneOutput(World, BlockPos, EnumFacing)}.
 */

@Mod.EventBusSubscriber(modid = Plethora.ID)
public class BlockRedstoneIntegrator extends BlockGeneric {
    private static final Set<TileRedstoneIntegrator> toTick = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final String NAME = "redstone_integrator";

    public BlockRedstoneIntegrator() {
        super(Material.ROCK);

        setRegistryName(new ResourceLocation(Plethora.RESOURCE_DOMAIN, NAME));

        setHardness(2);
        setTranslationKey(Plethora.RESOURCE_DOMAIN + "." + NAME);
        setCreativeTab(Plethora.getCreativeTab());
    }

    protected IBlockState getDefaultBlockState(int meta, EnumFacing direction) {
        return blockState.getBaseState();
    }

    @Override
    protected TileGeneric createTile(IBlockState blockState) {
        return new TileRedstoneIntegrator();
    }

    @Override
    protected TileGeneric createTile(int meta) {
        return new TileRedstoneIntegrator();
    }

    @Nonnull
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    public static void enqueueTick(TileRedstoneIntegrator tile) {
        toTick.add(tile);
    }

    @SubscribeEvent
    public static void handleTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            Iterator<TileRedstoneIntegrator> iterator = toTick.iterator();
            while (iterator.hasNext()) {
                TileRedstoneIntegrator tile = iterator.next();
                tile.updateOnce();
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void handleUnload(WorldEvent.Unload e) {
        World eventWorld = e.getWorld();
        if (!eventWorld.isRemote) {
            Iterator<TileRedstoneIntegrator> iterator = toTick.iterator();
            while (iterator.hasNext()) {
                World world = iterator.next().getWorld();
                if (world == null || world == eventWorld) iterator.remove();
            }
        }
    }
}
