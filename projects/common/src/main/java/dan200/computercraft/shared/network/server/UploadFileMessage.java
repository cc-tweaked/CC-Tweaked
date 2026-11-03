// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class UploadFileMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, UploadFileMessage> STREAM_CODEC = StreamCodec.ofMember(UploadFileMessage::write, UploadFileMessage::new);

    static final int MAX_PACKET_SIZE = 30 * 1024; // Max packet size is 32767.
    private static final int HEADER_SIZE = 16 + 1; // 16 bytes for the UUID, 4 for the flag.

    public static final int MAX_FILES = 32;
    public static final int MAX_FILE_NAME = 128;

    private static final StreamCodec<ByteBuf, List<FileSlice>> FILE_SLICE_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FileSlice::fileId,
        ByteBufCodecs.VAR_INT, FileSlice::offset,
        MoreStreamCodecs.byteBuffer(MAX_PACKET_SIZE), FileSlice::bytes,
        FileSlice::new
    ).apply(ByteBufCodecs.list(MAX_FILES));

    static final @VisibleForTesting int FLAG_FIRST = 1;
    static final @VisibleForTesting int FLAG_LAST = 2;

    private final UUID uuid;
    final @VisibleForTesting int flag;
    @VisibleForTesting
    final @Nullable List<FileUpload> files;
    final @VisibleForTesting List<FileSlice> slices;

    UploadFileMessage(AbstractContainerMenu menu, UUID uuid, int flag, @Nullable List<FileUpload> files, List<FileSlice> slices) {
        super(menu.containerId);

        if (files != null && files.size() > MAX_FILES) throw new IllegalArgumentException("Too many files");
        // We expect to have at most one slice per file in a packet.
        if (slices.size() > MAX_FILES) throw new IllegalArgumentException("Too many file slices");

        this.uuid = uuid;
        this.flag = flag;
        this.files = files;
        this.slices = slices;
    }

    private UploadFileMessage(FriendlyByteBuf buf) {
        super(buf.readVarInt());
        uuid = buf.readUUID();
        var flag = this.flag = buf.readByte();

        var totalSize = 0;
        if ((flag & FLAG_FIRST) != 0) {
            var nFiles = buf.readVarInt();
            if (nFiles > MAX_FILES) throw new DecoderException("Too many files");

            var files = this.files = new ArrayList<>(nFiles);
            for (var i = 0; i < nFiles; i++) {
                var name = buf.readUtf(MAX_FILE_NAME);
                var size = buf.readVarInt();
                if (size > Config.uploadMaxSize || (totalSize += size) > Config.uploadMaxSize) {
                    throw new DecoderException("Files are too large");
                }

                var digest = new byte[FileUpload.CHECKSUM_LENGTH];
                buf.readBytes(digest);

                files.add(new FileUpload(name, ByteBuffer.allocateDirect(size), digest));
            }
        } else {
            files = null;
        }

        slices = FILE_SLICE_STREAM_CODEC.decode(buf);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId());
        buf.writeUUID(uuid);
        buf.writeByte(flag);

        if ((flag & FLAG_FIRST) != 0) {
            var files = assertNonNull(this.files);
            buf.writeVarInt(files.size());
            for (var file : files) {
                buf.writeUtf(file.getName(), MAX_FILE_NAME);
                buf.writeVarInt(file.getLength());
                buf.writeBytes(file.getChecksum());
            }
        }

        FILE_SLICE_STREAM_CODEC.encode(buf, slices);
    }

    public static void send(AbstractContainerMenu container, List<FileUpload> files, Consumer<UploadFileMessage> send) {
        var uuid = UUID.randomUUID();

        var remaining = MAX_PACKET_SIZE - HEADER_SIZE;
        for (var file : files) remaining -= 4 + file.getName().length() * 4 + FileUpload.CHECKSUM_LENGTH;

        var first = true;
        List<FileSlice> slices = new ArrayList<>(files.size());
        for (var fileId = 0; fileId < files.size(); fileId++) {
            var file = files.get(fileId);
            var contents = file.getBytes().duplicate();
            var capacity = contents.limit();

            var currentOffset = 0;
            while (currentOffset < capacity) {
                if (remaining <= 0) {
                    send.accept(first
                        ? new UploadFileMessage(container, uuid, FLAG_FIRST, files, new ArrayList<>(slices))
                        : new UploadFileMessage(container, uuid, 0, null, new ArrayList<>(slices)));
                    slices.clear();
                    remaining = MAX_PACKET_SIZE - HEADER_SIZE;
                    first = false;
                }

                var canWrite = Math.min(remaining, capacity - currentOffset);

                contents.position(currentOffset).limit(currentOffset + canWrite);
                slices.add(new FileSlice(fileId, currentOffset, contents.slice()));
                currentOffset += canWrite;
                remaining -= canWrite;
            }
        }

        send.accept(first
            ? new UploadFileMessage(container, uuid, FLAG_FIRST | FLAG_LAST, files, new ArrayList<>(slices))
            : new UploadFileMessage(container, uuid, FLAG_LAST, null, new ArrayList<>(slices)));
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var player = context.getSender();

        var input = container.getInput();
        if ((flag & FLAG_FIRST) != 0) input.startUpload(uuid, assertNonNull(files));
        input.continueUpload(uuid, slices);
        if ((flag & FLAG_LAST) != 0) input.finishUpload(player, uuid);
    }

    @Override
    public CustomPacketPayload.Type<UploadFileMessage> type() {
        return NetworkMessages.UPLOAD_FILE;
    }
}
