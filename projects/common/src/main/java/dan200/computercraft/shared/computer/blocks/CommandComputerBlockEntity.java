// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CommandComputerBlockEntity extends ComputerBlockEntity {
    public CommandComputerBlockEntity(BlockEntityType<? extends ComputerBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ComputerFamily.COMMAND);
    }

    @Override
    protected ServerComputer createComputer(int id) {
        var computer = super.createComputer(id);
        computer.addAPI(new CommandAPI(computer));
        return computer;
    }
}
