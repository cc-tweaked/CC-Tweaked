// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.command.arguments.ComputersArgumentType;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import static dan200.computercraft.shared.command.CommandUtils.isPlayer;
import static dan200.computercraft.shared.command.Exceptions.NOT_TRACKING_EXCEPTION;
import static dan200.computercraft.shared.command.Exceptions.NO_TIMINGS_EXCEPTION;
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
                .requires(ModRegistry.Permissions.PERMISSION_DUMP)
                .executes(c -> dump(c.getSource()))
                .then(args()
                    .arg("computer", oneComputer())
                    .executes(c -> dumpComputer(c.getSource(), getComputerArgument(c, "computer")))))

            .then(command("shutdown")
                .requires(ModRegistry.Permissions.PERMISSION_SHUTDOWN)
                .argManyValue("computers", manyComputers(), s -> ServerContext.get(s.getServer()).registry().getComputers())
                .executes((c, a) -> shutdown(c.getSource(), unwrap(c.getSource(), a))))

            .then(command("turn-on")
                .requires(ModRegistry.Permissions.PERMISSION_TURN_ON)
                .argManyValue("computers", manyComputers(), s -> ServerContext.get(s.getServer()).registry().getComputers())
                .executes((c, a) -> turnOn(c.getSource(), unwrap(c.getSource(), a))))

            .then(command("tp")
                .requires(ModRegistry.Permissions.PERMISSION_TP)
                .arg("computer", oneComputer())
                .executes(c -> teleport(c.getSource(), getComputerArgument(c, "computer"))))

            .then(command("queue")
                .requires(ModRegistry.Permissions.PERMISSION_QUEUE)
                .arg(
                    RequiredArgumentBuilder.<CommandSourceStack, ComputersArgumentType.ComputersSupplier>argument("computer", manyComputers())
                        .suggests((context, builder) -> Suggestions.empty())
                )
                .argManyValue("args", StringArgumentType.string(), List.of())
                .executes((c, a) -> queue(getComputersArgument(c, "computer"), a)))

            .then(command("view")
                .requires(ModRegistry.Permissions.PERMISSION_VIEW)
                .arg("computer", oneComputer())
                .executes(c -> view(c.getSource(), getComputerArgument(c, "computer"))))

            .then(choice("track")
                .requires(ModRegistry.Permissions.PERMISSION_TRACK)
                .then(command("start").executes(c -> trackStart(c.getSource())))
                .then(command("stop").executes(c -> trackStop(c.getSource())))
                .then(command("dump")
                    .argManyValue("fields", metric(), DEFAULT_FIELDS)
                    .executes((c, f) -> trackDump(c.getSource(), f))))
        );
    }

    /**
     * Display loaded computers to a table.
     *
     * @param source The thing that executed this command.
     * @return The number of loaded computers.
     */
    private static int dump(CommandSourceStack source) {
        var table = new TableBuilder("DumpAll", "Computer", "On", "Position");

        List<ServerComputer> computers = new ArrayList<>(ServerContext.get(source.getServer()).registry().getComputers());

        Level world = source.getLevel();
        var pos = BlockPos.containing(source.getPosition());

        // Sort by nearby computers.
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

        table.display(source);
        return computers.size();
    }

    /**
     * Display additional information about a single computer.
     *
     * @param source   The thing that executed this command.
     * @param computer The computer we're dumping.
     * @return The constant {@code 1}.
     */
    private static int dumpComputer(CommandSourceStack source, ServerComputer computer) {
        var table = new TableBuilder("Dump");
        table.row(header("Instance"), text(Integer.toString(computer.getInstanceID())));
        table.row(header("Id"), text(Integer.toString(computer.getID())));
        table.row(header("Label"), text(computer.getLabel()));
        table.row(header("On"), bool(computer.isOn()));
        table.row(header("Position"), linkPosition(source, computer));
        table.row(header("Family"), text(computer.getFamily().toString()));

        for (var side : ComputerSide.values()) {
            var peripheral = computer.getPeripheral(side);
            if (peripheral != null) {
                table.row(header("Peripheral " + side.getName()), text(peripheral.getType()));
            }
        }

        table.display(source);
        return 1;
    }

    /**
     * Shutdown a list of computers.
     *
     * @param source    The thing that executed this command.
     * @param computers The computers to shutdown.
     * @return The constant {@code 1}.
     */
    private static int shutdown(CommandSourceStack source, Collection<ServerComputer> computers) {
        var shutdown = 0;
        for (var computer : computers) {
            if (computer.isOn()) shutdown++;
            computer.shutdown();
        }

        var didShutdown = shutdown;
        source.sendSuccess(() -> Component.translatable("commands.computercraft.shutdown.done", didShutdown, computers.size()), false);
        return shutdown;
    }

    /**
     * Turn on a list of computers.
     *
     * @param source    The thing that executed this command.
     * @param computers The computers to turn on.
     * @return The constant {@code 1}.
     */
    private static int turnOn(CommandSourceStack source, Collection<ServerComputer> computers) {
        var on = 0;
        for (var computer : computers) {
            if (!computer.isOn()) on++;
            computer.turnOn();
        }

        var didOn = on;
        source.sendSuccess(() -> Component.translatable("commands.computercraft.turn_on.done", didOn, computers.size()), false);
        return on;
    }

    /**
     * Teleport to a computer.
     *
     * @param source   The thing that executed this command. This must be an entity, other types will throw an exception.
     * @param computer The computer to teleport to.
     * @return The constant {@code 1}.
     */
    private static int teleport(CommandSourceStack source, ServerComputer computer) throws CommandSyntaxException {
        var world = computer.getLevel();
        var pos = Vec3.atBottomCenterOf(computer.getPosition());
        source.getEntityOrException().teleportTo(world, pos.x(), pos.y(), pos.z(), EnumSet.noneOf(RelativeMovement.class), 0, 0);

        return 1;
    }

    /**
     * Queue a {@code computer_command} event on a command computer.
     *
     * @param computers The list of computers to queue on.
     * @param args      The arguments for this event.
     * @return The number of computers this event was queued on.
     */
    private static int queue(Collection<ServerComputer> computers, List<String> args) {
        var rest = args.toArray();

        var queued = 0;
        for (var computer : computers) {
            if (computer.getFamily() == ComputerFamily.COMMAND && computer.isOn()) {
                computer.queueEvent("computer_command", rest);
                queued++;
            }
        }

        return queued;
    }

    /**
     * Open a terminal for a computer.
     *
     * @param source   The thing that executed this command.
     * @param computer The computer to view.
     * @return The constant {@code 1}.
     */
    private static int view(CommandSourceStack source, ServerComputer computer) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
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
    }

    /**
     * Start tracking metrics for the current player.
     *
     * @param source The thing that executed this command.
     * @return The constant {@code 1}.
     */
    private static int trackStart(CommandSourceStack source) {
        getMetricsInstance(source).start();

        var stopCommand = "/computercraft track stop";
        source.sendSuccess(() -> Component.translatable(
            "commands.computercraft.track.start.stop",
            link(text(stopCommand), stopCommand, Component.translatable("commands.computercraft.track.stop.action"))
        ), false);
        return 1;
    }

    /**
     * Stop tracking metrics for the current player, displaying a table with the results.
     *
     * @param source The thing that executed this command.
     * @return The constant {@code 1}.
     */
    private static int trackStop(CommandSourceStack source) throws CommandSyntaxException {
        var metrics = getMetricsInstance(source);
        if (!metrics.stop()) throw NOT_TRACKING_EXCEPTION.create();
        displayTimings(source, metrics.getSnapshot(), new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.AVG), DEFAULT_FIELDS);
        return 1;
    }

    private static final List<AggregatedMetric> DEFAULT_FIELDS = List.of(
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.COUNT),
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.NONE),
        new AggregatedMetric(Metrics.COMPUTER_TASKS, Aggregate.AVG)
    );

    /**
     * Display the latest metrics for the current player.
     *
     * @param source The thing that executed this command.
     * @param fields The fields to display in this table, defaulting to {@link #DEFAULT_FIELDS}.
     * @return The constant {@code 1}.
     */
    private static int trackDump(CommandSourceStack source, List<AggregatedMetric> fields) throws CommandSyntaxException {
        AggregatedMetric sort;
        if (fields.size() == 1 && DEFAULT_FIELDS.contains(fields.get(0))) {
            sort = fields.get(0);
            fields = DEFAULT_FIELDS;
        } else {
            sort = fields.get(0);
        }

        return displayTimings(source, getMetricsInstance(source).getTimings(), sort, fields);
    }

    // Additional helper functions.

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
        if (serverComputer != null && isPlayer(source)) {
            if (ModRegistry.Permissions.PERMISSION_TP.test(source)) {
                out.append(" ").append(link(
                    text("\u261b"),
                    "/computercraft tp " + serverComputer.getInstanceID(),
                    Component.translatable("commands.computercraft.tp.action")
                ));
            }

            if (ModRegistry.Permissions.PERMISSION_VIEW.test(source)) {
                out.append(" ").append(link(
                    text("\u20e2"),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    Component.translatable("commands.computercraft.view.action")
                ));
            }
        }

        if (isPlayer(source) && UserLevel.isOwner(source)) {
            var linkPath = linkStorage(source, computerId);
            if (linkPath != null) out.append(" ").append(linkPath);
        }

        return out;
    }

    private static Component linkPosition(CommandSourceStack context, ServerComputer computer) {
        if (ModRegistry.Permissions.PERMISSION_TP.test(context)) {
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
