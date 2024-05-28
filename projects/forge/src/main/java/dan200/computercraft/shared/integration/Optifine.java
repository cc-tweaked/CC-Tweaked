// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

/**
 * Detect whether Optifine is installed.
 */
public final class Optifine {
    private static final boolean LOADED;

    static {
        boolean loaded;
        try {
            Class.forName("optifine.Installer", false, Optifine.class.getClassLoader());
            loaded = true;
        } catch (ReflectiveOperationException | LinkageError ignore) {
            loaded = false;
        }

        LOADED = loaded;
    }

    private Optifine() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }


    /**
     * This rant probably should be in a commit message, but sometimes I want it visible in the code.
     * <p>
     * There is a long-standing Optifine issue (<a href="https://github.com/sp614x/optifine/issues/7549">#7549</a>,
     * <a href="https://github.com/sp614x/optifine/issues/7395">#7395</a>) that causes a block entity's
     * {@link BlockEntity#gatherCapabilities()} not to be called. This causes peripherals to not show up for block
     * entities.
     * <p>
     * While the bug is marked as fixed, there has not been a release since, and it continues to affect users a year
     * after being reported. This is a frequent problem with Optifine (see also <a href="https://github.com/sp614x/optifine/issues/7127">#7127</a>
     * and <a href="https://github.com/sp614x/optifine/issues/7261">#7261</a> which I <em>still</em> get bug reports
     * about), and this doesn't seem likely to change any time soon.
     * <p>
     * The ideal situation would be that any time the game starts with Optifine, we delete it from the mods folder.
     * That's probably a little uncouth (and a pain to get working on Windows), so instead we try to detect the bug in
     * question and print a message in chat whenever a player joins.
     *
     * @param send The object to send a message too.
     */
    public static void warnAboutOptifine(Consumer<Component> send) {
        if (!Optifine.isLoaded()) return;

        var computer = Nullability.assertNonNull(ModRegistry.BlockEntities.COMPUTER_NORMAL.get().create(
            BlockPos.ZERO, ModRegistry.Blocks.COMPUTER_NORMAL.get().defaultBlockState()
        ));
        if (computer.getCapability(Capabilities.CAPABILITY_PERIPHERAL).isPresent()) return;

        send.accept(
            Component.literal("")
                .append(Component.literal("(CC: Tweaked) ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[WARNING] ").withStyle(ChatFormatting.RED))
                .append("It looks like you're running Optifine. This has an ")
                .append(Component.literal("unfixed issue").withStyle(s -> s
                    .withColor(ChatFormatting.BLUE).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/sp614x/optifine/issues/7549"))
                ))
                .append(" which causes issues with many modded block entities, including those added by CC: Tweaked. " +
                    "Please replace Optifine with an alternative shader mod such as Oculus.")
        );
    }
}
