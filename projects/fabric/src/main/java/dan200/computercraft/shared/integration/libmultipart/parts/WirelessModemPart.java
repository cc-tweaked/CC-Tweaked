// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.libmultipart.parts;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartEventBus;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.PartDefinition;
import alexiil.mc.lib.multipart.api.event.PartTickEvent;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.integration.libmultipart.BlockStateModelKey;
import dan200.computercraft.shared.integration.libmultipart.LibMultiPartIntegration;
import dan200.computercraft.shared.integration.libmultipart.PlacementMultipartCreator;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlock;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A {@linkplain AbstractPart multipart} for wireless modems.
 *
 * @see WirelessModemBlock
 * @see WirelessModemBlockEntity
 */
public final class WirelessModemPart extends AbstractPart {
    private final WirelessModemBlock modemBlock;
    private final boolean advanced;
    private final Direction direction;

    private final Peripheral modem;
    private boolean on;

    private WirelessModemPart(
        PartDefinition definition, MultipartHolder holder, WirelessModemBlock modemBlock, boolean advanced,
        Direction direction, @Nullable ModemState state
    ) {
        super(definition, holder);
        this.modemBlock = modemBlock;
        this.advanced = advanced;
        this.direction = direction;

        modem = new Peripheral(this, state);
    }

    public static Definition makeDefinition(RegistryEntry<WirelessModemBlock> modem, boolean advanced) {
        return new Definition(modem, advanced);
    }

    @Override
    public void onAdded(MultipartEventBus bus) {
        if (container.getMultipartWorld().isClientSide) return;
        bus.addListener(this, PartTickEvent.class, event -> {
            if (modem.getModemState().pollChanged()) sendNetworkUpdate(this, NET_RENDER_DATA);
        });
    }

    @Override
    public void addAllAttributes(AttributeList<?> list) {
        super.addAllAttributes(list);
        if (list.attribute == LibMultiPartIntegration.PERIPHERAL && list.getSearchDirection() == direction.getOpposite()) {
            list.offer(modem);
        }
    }

    @Override
    public void writeCreationData(NetByteBuf buffer, IMsgWriteCtx ctx) {
        super.writeCreationData(buffer, ctx);
        buffer.writeEnum(direction);
    }

    @Override
    public CompoundTag toTag() {
        var tag = super.toTag();
        tag.putString("direction", direction.getSerializedName());
        return tag;
    }

    @Override
    public void writeRenderData(NetByteBuf buffer, IMsgWriteCtx ctx) {
        super.writeRenderData(buffer, ctx);
        buffer.writeBoolean(on = modem.getModemState().isOpen());
    }

    @Override
    public void readRenderData(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        super.readRenderData(buffer, ctx);
        on = buffer.readBoolean();
        redrawIfChanged();
    }

    @Override
    public VoxelShape getShape() {
        return ModemShapes.getBounds(direction);
    }

    @Nullable
    @Override
    public PartModelKey getModelKey() {
        return new BlockStateModelKey(
            modemBlock.defaultBlockState()
                .setValue(WirelessModemBlock.FACING, direction)
                .setValue(WirelessModemBlock.ON, on)
        );
    }

    private static class Peripheral extends WirelessModemPeripheral {
        private final WirelessModemPart part;

        Peripheral(WirelessModemPart part, @Nullable ModemState state) {
            // state will be non-null when converting an existing modem. This allows us to preserve the open channels.
            super(state == null ? new ModemState() : state.copy(), part.advanced);
            this.part = part;
        }

        @Override
        public Level getLevel() {
            return part.container.getMultipartWorld();
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atLowerCornerOf(part.container.getMultipartPos().relative(part.direction));
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other || (other instanceof Peripheral && part == ((Peripheral) other).part);
        }

        @Override
        public Object getTarget() {
            return part;
        }
    }

    public static final class Definition extends PartDefinition implements PlacementMultipartCreator {
        private final RegistryEntry<WirelessModemBlock> modem;
        private final boolean advanced;

        private Definition(RegistryEntry<WirelessModemBlock> modem, boolean advanced) {
            super(
                modem.id(),
                (def, holder, tag) -> {
                    var direction = Direction.CODEC.byName(tag.getString("direction"), Direction.NORTH);
                    return new WirelessModemPart(def, holder, modem.get(), advanced, direction, null);
                },
                (def, holder, buffer, context) -> {
                    var direction = buffer.readEnum(Direction.class);
                    return new WirelessModemPart(def, holder, modem.get(), advanced, direction, null);
                }
            );
            this.modem = modem;
            this.advanced = advanced;
        }

        public Block block() {
            return modem.get();
        }

        public AbstractPart convert(MultipartHolder holder, BlockState state, @Nullable BlockEntity blockEntity) {
            return new WirelessModemPart(
                this, holder, modem.get(), advanced,
                state.getValue(WirelessModemBlock.FACING),
                blockEntity instanceof WirelessModemBlockEntity modemBlockEntity ? modemBlockEntity.getModemState() : null
            );
        }

        @Override
        public AbstractPart create(MultipartHolder holder, BlockPlaceContext context) {
            return new WirelessModemPart(this, holder, modem.get(), advanced, context.getClickedFace().getOpposite(), null);
        }
    }
}
