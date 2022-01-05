/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

public class ClientComputerRegistry extends ComputerRegistry<ClientComputer>
{
    @Override
    public void add( int instanceID, ClientComputer computer )
    {
        super.add( instanceID, computer );
        computer.requestState();
    }
}
