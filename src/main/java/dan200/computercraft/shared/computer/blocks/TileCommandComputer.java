/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import java.util.HashMap;
import java.util.Map;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.NamedBlockEntityType;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

public class TileCommandComputer extends TileComputer {
    public static final NamedBlockEntityType<TileCommandComputer> FACTORY = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID,
                                                                                                                       "command_computer"),
                                                                                                        f -> new TileCommandComputer(ComputerFamily.Command,
                                                                                                                                     f));
    private final CommandReceiver receiver;

    public TileCommandComputer(ComputerFamily family, BlockEntityType<? extends TileCommandComputer> type) {
        super(family, type);
        this.receiver = new CommandReceiver();
    }

    public CommandReceiver getReceiver() {
        return this.receiver;
    }

    public ServerCommandSource getSource() {
        ServerComputer computer = this.getServerComputer();
        String name = "@";
        if (computer != null) {
            String label = computer.getLabel();
            if (label != null) {
                name = label;
            }
        }

        return new ServerCommandSource(this.receiver,
                                       new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5),
                                       Vec2f.ZERO,
                                       (ServerWorld) this.getWorld(),
                                       2,
                                       name,
                                       new LiteralText(name),
                                       this.getWorld().getServer(),
                                       null);
    }

    @Override
    protected ServerComputer createComputer(int instanceID, int id) {
        ServerComputer computer = super.createComputer(instanceID, id);
        computer.addAPI(new CommandAPI(this));
        return computer;
    }

    @Override
    public boolean isUsable(PlayerEntity player, boolean ignoreRange) {
        MinecraftServer server = player.getServer();
        if (server == null || !server.areCommandBlocksEnabled()) {
            player.sendMessage(new TranslatableText("advMode.notEnabled"), true);
            return false;
        } else if (!player.isCreativeLevelTwoOp()) {
            player.sendMessage(new TranslatableText("advMode.notAllowed"), true);
            return false;
        } else {
            return super.isUsable(player, ignoreRange);
        }
    }

    public class CommandReceiver implements CommandOutput {
        private final Map<Integer, String> output = new HashMap<>();

        public void clearOutput() {
            this.output.clear();
        }

        public Map<Integer, String> getOutput() {
            return this.output;
        }

        public Map<Integer, String> copyOutput() {
            return new HashMap<>(this.output);
        }

        @Override
        public void sendSystemMessage(Text textComponent) {
            this.output.put(this.output.size() + 1, textComponent.getString());
        }

        @Override
        public boolean shouldReceiveFeedback() {
            return TileCommandComputer.this.getWorld().getGameRules()
                                           .getBoolean(GameRules.SEND_COMMAND_FEEDBACK);
        }

        @Override
        public boolean shouldTrackOutput() {
            return true;
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return TileCommandComputer.this.getWorld().getGameRules()
                                           .getBoolean(GameRules.COMMAND_BLOCK_OUTPUT);
        }
    }
}
