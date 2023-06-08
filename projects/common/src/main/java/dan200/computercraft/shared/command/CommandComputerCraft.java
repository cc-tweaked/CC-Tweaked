// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.inventory.ViewComputerMenu;
import dan200.computercraft.shared.computer.metrics.basic.Aggregate;
import dan200.computercraft.shared.computer.metrics.basic.AggregatedMetric;
import dan200.computercraft.shared.computer.metrics.basic.BasicComputerMetricsObserver;
import dan200.computercraft.shared.computer.metrics.basic.ComputerMetrics;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import static dan200.computercraft.shared.command.CommandUtils.isPlayer;
import static dan200.computercraft.shared.command.Exceptions.*;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.getComputerArgument;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.oneComputer;
import static dan200.computercraft.shared.command.arguments.ComputersArgumentType.*;
import static dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType.metric;
import static dan200.computercraft.shared.command.builder.CommandBuilder.args;
import static dan200.computercraft.shared.command.builder.CommandBuilder.command;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static dan200.computercraft.shared.command.text.ChatHelpers.*;
import static net.minecraft.commands.Commands.literal;

public final class CommandComputerCraft {
    public static final UUID SYSTEM_UUID = new UUID(0, 0);
    public static final String OPEN_COMPUTER = "computercraft open-computer ";

    private CommandComputerCraft() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(choice("computercraft")
            .then(literal("dump")
                .requires(UserLevel.OWNER_OP)
                .executes(context -> {
                    var table = new TableBuilder("DumpAll", "Computer", "On", "Position");

                    var source = context.getSource();
                    List<ServerComputer> computers = new ArrayList<>(ServerContext.get(source.getServer()).registry().getComputers());

                    Level world = source.getLevel();
                    var pos = BlockPos.containing(source.getPosition());

                    computers.sort((a, b) -> {
                        if (a.getLevel() == b.getLevel() && a.getLevel() == world) {
                            return Double.compare(a.getPosition().distSqr(pos), b.getPosition().distSqr(pos));
                        } else if (a.getLevel() == world) {
                            return -1;
                        } else if (b.getLevel() == world) {
                            return 1;
                        } else {
                            return Integer.compare(a.getInstanceID(), b.getInstanceID());
                        }
                    });

                    for (var computer : computers) {
                        table.row(
                            linkComputer(source, computer, computer.getID()),
                            bool(computer.isOn()),
                            linkPosition(source, computer)
                        );
                    }

                    table.display(context.getSource());
                    return computers.size();
                })
                .then(args()
                    .arg("computer", oneComputer())
                    .executes(context -> {
                        var computer = getComputerArgument(context, "computer");

                        var table = new TableBuilder("Dump");
                        table.row(header("Instance"), text(Integer.toString(computer.getInstanceID())));
                        table.row(header("Id"), text(Integer.toString(computer.getID())));
                        table.row(header("Label"), text(computer.getLabel()));
                        table.row(header("On"), bool(computer.isOn()));
                        table.row(header("Position"), linkPosition(context.getSource(), computer));
                        table.row(header("Family"), text(computer.getFamily().toString()));

                        for (var side : ComputerSide.values()) {
                            var peripheral = computer.getPeripheral(side);
                            if (peripheral != null) {
                                table.row(header("Peripheral " + side.getName()), text(peripheral.getType()));
                            }
                        }

                        table.display(context.getSource());
                        return 1;
                    })))

            .then(command("shutdown")
                .requires(UserLevel.OWNER_OP)
                .argManyValue("computers", manyComputers(), s -> ServerContext.get(s.getServer()).registry().getComputers())
                .executes((context, computerSelectors) -> {
                    var shutdown = 0;
                    var computers = unwrap(context.getSource(), computerSelectors);
                    for (var computer : computers) {
                        if (computer.isOn()) shutdown++;
                        computer.shutdown();
                    }

                    var didShutdown = shutdown;
                    context.getSource().sendSuccess(() -> Component.translatable("commands.computercraft.shutdown.done", didShutdown, computers.size()), false);
                    return shutdown;
                }))

            .then(command("turn-on")
                .requires(UserLevel.OWNER_OP)
                .argManyValue("computers", manyComputers(), s -> ServerContext.get(s.getServer()).registry().getComputers())
                .executes((context, computerSelectors) -> {
                    var on = 0;
                    var computers = unwrap(context.getSource(), computerSelectors);
                    for (var computer : computers) {
                        if (!computer.isOn()) on++;
                        computer.turnOn();
                    }

                    var didOn = on;
                    context.getSource().sendSuccess(() -> Component.translatable("commands.computercraft.turn_on.done", didOn, computers.size()), false);
                    return on;
                }))

            .then(command("tp")
                .requires(UserLevel.OP)
                .arg("computer", oneComputer())
                .executes(context -> {
                    var computer = getComputerArgument(context, "computer");
                    var world = computer.getLevel();
                    var pos = computer.getPosition();

                    var entity = context.getSource().getEntityOrException();
                    if (!(entity instanceof ServerPlayer player)) throw TP_NOT_PLAYER.create();

                    if (player.getCommandSenderWorld() == world) {
                        player.connection.teleport(
                            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0,
                            EnumSet.noneOf(RelativeMovement.class)
                        );
                    } else {
                        player.teleportTo(world,
                            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0
                        );
                    }

                    return 1;
                }))

            .then(command("queue")
                .requires(UserLevel.ANYONE)
                .arg("computer", manyComputers())
                .argManyValue("args", StringArgumentType.string(), Collections.emptyList())
                .executes((ctx, args) -> {
                    var computers = getComputersArgument(ctx, "computer");
                    var rest = args.toArray();

                    var queued = 0;
                    for (var computer : computers) {
                        if (computer.getFamily() == ComputerFamily.COMMAND && computer.isOn()) {
                            computer.queueEvent("computer_command", rest);
                            queued++;
                        }
                    }

                    return queued;
                }))

            .then(command("view")
                .requires(UserLevel.OP)
                .arg("computer", oneComputer())
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var computer = getComputerArgument(context, "computer");
                    new ComputerContainerData(computer, ItemStack.EMPTY).open(player, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("gui.computercraft.view_computer");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory player, Player entity) {
                            return new ViewComputerMenu(id, player, computer);
                        }
                    });
                    return 1;
                }))

            .then(choice("track")
                .then(command("start")
                    .requires(UserLevel.OWNER_OP)
                    .executes(context -> {
                        getMetricsInstance(context.getSource()).start();

                        var stopCommand = "/computercraft track stop";
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "commands.computercraft.track.start.stop",
                            link(text(stopCommand), stopCommand, Component.translatable("commands.computercraft.track.stop.action"))
                        ), false);
                        return 1;
                    }))

                .then(command("stop")
                    .requires(UserLevel.OWNER_OP)
                    .executes(context -> {
                        var timings = getMetricsInstance(context.getSource());
                        if (!timings.stop()) throw NOT_TRACKING_EXCEPTION.create();
                        displayTimings(context.getSource(), timings.getSnapshot(), new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.AVG), DEFAULT_FIELDS);
                        return 1;
                    }))

                .then(command("dump")
                    .requires(UserLevel.OWNER_OP)
                    .argManyValue("fields", metric(), DEFAULT_FIELDS)
                    .executes((context, fields) -> {
                        AggregatedMetric sort;
                        if (fields.size() == 1 && DEFAULT_FIELDS.contains(fields.get(0))) {
                            sort = fields.get(0);
                            fields = DEFAULT_FIELDS;
                        } else {
                            sort = fields.get(0);
                        }

                        return displayTimings(context.getSource(), sort, fields);
                    })))
        );
    }

    private static Component linkComputer(CommandSourceStack source, @Nullable ServerComputer serverComputer, int computerId) {
        var out = Component.literal("");

        // Append the computer instance
        if (serverComputer == null) {
            out.append(text("?"));
        } else {
            out.append(link(
                text(Integer.toString(serverComputer.getInstanceID())),
                "/computercraft dump " + serverComputer.getInstanceID(),
                Component.translatable("commands.computercraft.dump.action")
            ));
        }

        // And ID
        out.append(" (id " + computerId + ")");

        // And, if we're a player, some useful links
        if (serverComputer != null && UserLevel.OP.test(source) && isPlayer(source)) {
            out
                .append(" ")
                .append(link(
                    text("\u261b"),
                    "/computercraft tp " + serverComputer.getInstanceID(),
                    Component.translatable("commands.computercraft.tp.action")
                ))
                .append(" ")
                .append(link(
                    text("\u20e2"),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    Component.translatable("commands.computercraft.view.action")
                ));
        }

        if (UserLevel.OWNER.test(source) && isPlayer(source)) {
            var linkPath = linkStorage(source, computerId);
            if (linkPath != null) out.append(" ").append(linkPath);
        }

        return out;
    }

    private static Component linkPosition(CommandSourceStack context, ServerComputer computer) {
        if (UserLevel.OP.test(context)) {
            return link(
                position(computer.getPosition()),
                "/computercraft tp " + computer.getInstanceID(),
                Component.translatable("commands.computercraft.tp.action")
            );
        } else {
            return position(computer.getPosition());
        }
    }

    private static @Nullable Component linkStorage(CommandSourceStack source, int id) {
        var file = new File(ServerContext.get(source.getServer()).storageDir().toFile(), "computer/" + id);
        if (!file.isDirectory()) return null;

        return link(
            text("\u270E"),
            "/" + OPEN_COMPUTER + id,
            Component.translatable("commands.computercraft.dump.open_path")
        );
    }

    private static BasicComputerMetricsObserver getMetricsInstance(CommandSourceStack source) {
        var entity = source.getEntity();
        return ServerContext.get(source.getServer()).metrics().getMetricsInstance(entity instanceof Player ? entity.getUUID() : SYSTEM_UUID);
    }

    private static final List<AggregatedMetric> DEFAULT_FIELDS = Arrays.asList(
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.COUNT),
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.NONE),
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.AVG)
    );

    private static int displayTimings(CommandSourceStack source, AggregatedMetric sortField, List<AggregatedMetric> fields) throws CommandSyntaxException {
        return displayTimings(source, getMetricsInstance(source).getTimings(), sortField, fields);
    }

    private static int displayTimings(CommandSourceStack source, List<ComputerMetrics> timings, AggregatedMetric sortField, List<AggregatedMetric> fields) throws CommandSyntaxException {
        if (timings.isEmpty()) throw NO_TIMINGS_EXCEPTION.create();

        timings.sort(Comparator.<ComputerMetrics, Long>comparing(x -> x.get(sortField.metric(), sortField.aggregate())).reversed());

        var headers = new Component[1 + fields.size()];
        headers[0] = Component.translatable("commands.computercraft.track.dump.computer");
        for (var i = 0; i < fields.size(); i++) headers[i + 1] = fields.get(i).displayName();
        var table = new TableBuilder("Metrics", headers);

        for (var entry : timings) {
            var serverComputer = entry.computer();

            var computerComponent = linkComputer(source, serverComputer, entry.computerId());

            var row = new Component[1 + fields.size()];
            row[0] = computerComponent;
            for (var i = 0; i < fields.size(); i++) {
                var metric = fields.get(i);
                row[i + 1] = text(entry.getFormatted(metric.metric(), metric.aggregate()));
            }
            table.row(row);
        }

        table.display(source);
        return timings.size();
    }
}
