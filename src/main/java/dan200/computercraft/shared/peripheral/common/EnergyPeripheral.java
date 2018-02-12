package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.util.EnergyUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class EnergyPeripheral implements IPeripheral
{
	
	private final TileEntity tile;
	private final EnumFacing side;
	
	private String[] methods;
	
	public EnergyPeripheral(TileEntity tile, EnumFacing side)
	{
		this.tile = tile;
		this.side = side;
	}

	@Override
	public String getType()
	{
		String name = "energy";
		final String blockName = tile.getBlockType().getUnlocalizedName();
		
		if (blockName.contains("."))
		{
			final String[] splitName = blockName.split("\\.");
			name += "_" + splitName[splitName.length - 1];
		}
		return name;
	}

	@Override
	public String[] getMethodNames()
	{
		this.methods = EnergyUtils.getPossibleMethods(tile, side.getOpposite());
		
		return this.methods;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException
	{
		switch( this.methods[method] )
        {
            case "getEnergyStored":
            {
            	synchronized( this )
                {
            		return new Object[] {EnergyUtils.getEnergyStored(tile, side.getOpposite())};
                }
            }
            case "getMaxEnergyStored":
            {
            	synchronized( this )
                {
            		return new Object[] {EnergyUtils.getMaxEnergyStored(tile, side.getOpposite())};
                }
            }
            case "getMaxEnergyExtract":
            {
            	synchronized( this )
                {
            		return new Object[] {EnergyUtils.getMaxEnergyExtract(tile, side.getOpposite())};
                }
            }
            case "getMaxEnergyReceive":
            {
            	synchronized( this )
                {
            		return new Object[] {EnergyUtils.getMaxEnergyReceive(tile, side.getOpposite())};
                }
            }
        }
		
		return null;
	}

	@Override
	public boolean equals(IPeripheral other) {
		return false;
	}
}