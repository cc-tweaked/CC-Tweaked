package dan200.computercraft.shared.util;

import java.util.ArrayList;
import java.util.List;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.integration.RedstoneFluxCC;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

public class EnergyUtils {
	
	public static boolean blockHasEnergyHandler( final World world, final BlockPos pos, final EnumFacing side )
	{
		final TileEntity tile = world.getTileEntity(pos);
		
		if ( ComputerCraft.redstonefluxLoaded && RedstoneFluxCC.isEnergyHandler(tile) )
		{
			return true;
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			return true;
		}
    	
		return false;
	}
	
	public static String[] getPossibleMethods( final TileEntity tile, final EnumFacing side )
	{
		List<String> methods = new ArrayList<String>();
		
		if ( ComputerCraft.redstonefluxLoaded )
		{
			if ( RedstoneFluxCC.isEnergyHandler(tile) )
			{
				methods.add("getEnergyStored");
				methods.add("getMaxEnergyStored");
			}
			
			if ( RedstoneFluxCC.isEnergyProvider(tile) )
			{
				methods.add("getMaxEnergyExtract");
			}
			
			if ( RedstoneFluxCC.isEnergyReceiver(tile) )
			{
				methods.add("getMaxEnergyReceive");
			}
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			if ( !methods.contains("getEnergyStored") ) 
				methods.add("getEnergyStored");
			
			if ( !methods.contains("getMaxEnergyStored") ) 
				methods.add("getMaxEnergyStored");
			
			if ( !methods.contains("getMaxEnergyExtract") ) 
				methods.add("getMaxEnergyExtract");
			
			if ( !methods.contains("getMaxEnergyReceive") ) 
				methods.add("getMaxEnergyReceive");
		}
		
		return methods.toArray(new String[0]);
	}
	
	public static int getEnergyStored( final TileEntity tile, final EnumFacing side )
	{
		if ( ComputerCraft.redstonefluxLoaded )
		{
			return RedstoneFluxCC.getEnergyStored(tile, side);
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			return tile.getCapability(CapabilityEnergy.ENERGY, side).getEnergyStored();
		}
		
		return 0;
	}
	
	public static int getMaxEnergyStored( final TileEntity tile, final EnumFacing side )
	{
		if ( ComputerCraft.redstonefluxLoaded )
		{
			return RedstoneFluxCC.getMaxEnergyStored(tile, side);
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			return tile.getCapability(CapabilityEnergy.ENERGY, side).getMaxEnergyStored();
		}
		
		return 0;
	}
	
	public static int getMaxEnergyExtract( final TileEntity tile, final EnumFacing side )
	{
		if ( ComputerCraft.redstonefluxLoaded )
		{
			return RedstoneFluxCC.getMaxEnergyExtract(tile, side);
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			return tile.getCapability(CapabilityEnergy.ENERGY, side).extractEnergy(Integer.MAX_VALUE, true);
		}
		
		return 0;
	}
	
	public static int getMaxEnergyReceive( final TileEntity tile, final EnumFacing side )
	{
		if ( ComputerCraft.redstonefluxLoaded )
		{
			return RedstoneFluxCC.getMaxEnergyReceive(tile, side);
		}
		
		if ( tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side) )
		{
			return tile.getCapability(CapabilityEnergy.ENERGY, side).receiveEnergy(Integer.MAX_VALUE, true);
		}
		
		return 0;
	}
}