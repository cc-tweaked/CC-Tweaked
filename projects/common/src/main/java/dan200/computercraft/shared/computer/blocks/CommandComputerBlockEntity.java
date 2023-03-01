// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class CommandComputerBlockEntity extends ComputerBlockEntity {
    public class CommandReceiver implements CommandSource {
        private final Map<Integer, String> output = new HashMap<>();

        public void clearOutput() {
            output.clear();
        }

        public Map<Integer, String> getOutput() {
            return output;
        }

        public Map<Integer, String> copyOutput() {
            return new HashMap<>(output);
        }

        @Override
        public void sendSystemMessage(Component textComponent) {
            output.put(output.size() + 1, textComponent.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
        }
    }

    private final CommandReceiver receiver;

    public CommandComputerBlockEntity(BlockEntityType<? extends ComputerBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ComputerFamily.COMMAND);
        receiver = new CommandReceiver();
    }

    public CommandReceiver getReceiver() {
        return receiver;
    }

    public CommandSourceStack getSource() {
        var computer = getServerComputer();
        var name = "@";
        if (computer != null) {
            var label = computer.getLabel();
            if (label != null) name = label;
        }

        return new CommandSourceStack(receiver,
            Vec3.atCenterOf(worldPosition), Vec2.ZERO,
            (ServerLevel) getLevel(), 2,
            name, Component.literal(name),
            getLevel().getServer(), null
        );
    }

    @Override
    protected ServerComputer createComputer(int id) {
        var computer = super.createComputer(id);
        computer.addAPI(new CommandAPI(this));
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
        } else if (Config.commandRequireCreative ? !player.canUseGameMasterBlocks() : !server.getPlayerList().isOp(player.getGameProfile())) {
            player.displayClientMessage(Component.translatable("advMode.notAllowed"), true);
            return false;
        }

        return true;
    }
}
