package dan200.computercraft.shared.integration;

import cofh.redstoneflux.api.IEnergyHandler;
import cofh.redstoneflux.api.IEnergyProvider;
import cofh.redstoneflux.api.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class RedstoneFluxCC {

	public static boolean isEnergyHandler( final TileEntity tile )
	{
		return tile instanceof IEnergyHandler;
	}
	
	public static boolean isEnergyProvider( final TileEntity tile )
	{
		return tile instanceof IEnergyProvider;
	}
	
	public static boolean isEnergyReceiver( final TileEntity tile )
	{
		return tile instanceof IEnergyReceiver;
	}
	
	public static int getEnergyStored( final TileEntity tile, final EnumFacing side )
	{
		if ( isEnergyHandler(tile) )
		{
			return ((IEnergyHandler)tile).getEnergyStored(side);
		}
		
		return 0;
	}
	
	public static int getMaxEnergyStored( final TileEntity tile, final EnumFacing side )
	{
		if ( isEnergyHandler(tile) )
		{
			return ((IEnergyHandler)tile).getMaxEnergyStored(side);
		}
		
		return 0;
	}
	
	public static int getMaxEnergyExtract( final TileEntity tile, final EnumFacing side )
	{
		if ( isEnergyProvider(tile) )
		{
			return ((IEnergyProvider)tile).extractEnergy(side, Integer.MAX_VALUE, true);
		}
		
		return 0;
	}
	
	public static int getMaxEnergyReceive( final TileEntity tile, final EnumFacing side )
	{
		if ( isEnergyReceiver(tile) )
		{
			return ((IEnergyReceiver)tile).receiveEnergy(side, Integer.MAX_VALUE, true);
		}
		
		return 0;
	}

}