/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import dan200.computercraft.test.shared.WithMinecraft;
import dan200.computercraft.test.shared.platform.ContainerTransferContract;
import net.minecraft.world.Container;
import net.minecraftforge.items.wrapper.InvWrapper;

@WithMinecraft
public class ForgeContainerTransferTest implements ContainerTransferContract {
    @Override
    public ContainerTransfer.Slotted wrap(Container container) {
        return new ForgeContainerTransfer(new InvWrapper(container));
    }
}
