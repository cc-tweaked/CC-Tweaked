// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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

    @Override
    public boolean isUsable(Player player) {
        return isCommandUsable(player) && super.isUsable(player);
    }

    public static boolean isCommandUsable(Player player) {
        var server = player.getServer();
        if (server == null || !server.isCommandBlockEnabled()) {
            player.displayClientMessage(Component.translatable("advMode.notEnabled"), true);
            return false;
        } else if (!canUseCommandBlock(player)) {
            player.displayClientMessage(Component.translatable("advMode.notAllowed"), true);
            return false;
        }

        return true;
    }

    private static boolean canUseCommandBlock(Player player) {
        return Config.commandRequireCreative ? player.canUseGameMasterBlocks() : player.hasPermissions(2);
    }
}
