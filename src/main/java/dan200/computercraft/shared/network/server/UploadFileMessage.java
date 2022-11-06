/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class UploadFileMessage extends ComputerServerMessage {
    public static final int MAX_SIZE = 512 * 1024;
    static final int MAX_PACKET_SIZE = 30 * 1024; // Max packet size is 32767.

    public static final int MAX_FILES = 32;
    public static final int MAX_FILE_NAME = 128;

    static final @VisibleForTesting int FLAG_FIRST = 1;
    static final @VisibleForTesting int FLAG_LAST = 2;

    private final UUID uuid;
    final @VisibleForTesting int flag;
    final @VisibleForTesting List<FileUpload> files;
    final @VisibleForTesting List<FileSlice> slices;

    UploadFileMessage(AbstractContainerMenu menu, UUID uuid, int flag, List<FileUpload> files, List<FileSlice> slices) {
        super(menu);
        this.uuid = uuid;
        this.flag = flag;
        this.files = files;
        this.slices = slices;
    }

    public UploadFileMessage(@Nonnull FriendlyByteBuf buf) {
        super(buf);
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
                if (size > MAX_SIZE || (totalSize += size) > MAX_SIZE) {
                    throw new DecoderException("Files are too large");
                }

                var digest = new byte[FileUpload.CHECKSUM_LENGTH];
                buf.readBytes(digest);

                files.add(new FileUpload(name, ByteBuffer.allocateDirect(size), digest));
            }
        } else {
            files = null;
        }

        var nSlices = buf.readVarInt();
        var slices = this.slices = new ArrayList<>(nSlices);
        for (var i = 0; i < nSlices; i++) {
            int fileId = buf.readUnsignedByte();
            var offset = buf.readVarInt();

            var size = buf.readUnsignedShort();
            if (size > MAX_PACKET_SIZE) throw new DecoderException("File is too large");

            var buffer = ByteBuffer.allocateDirect(size);
            buf.readBytes(buffer);
            buffer.flip();

            slices.add(new FileSlice(fileId, offset, buffer));
        }
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeUUID(uuid);
        buf.writeByte(flag);

        if ((flag & FLAG_FIRST) != 0) {
            buf.writeVarInt(files.size());
            for (var file : files) {
                buf.writeUtf(file.getName(), MAX_FILE_NAME);
                buf.writeVarInt(file.getLength());
                buf.writeBytes(file.getChecksum());
            }
        }

        buf.writeVarInt(slices.size());
        for (var slice : slices) {
            buf.writeByte(slice.fileId());
            buf.writeVarInt(slice.offset());

            var bytes = slice.bytes().duplicate();
            buf.writeShort(bytes.remaining());
            buf.writeBytes(bytes);
        }
    }

    public static void send(AbstractContainerMenu container, List<FileUpload> files, Consumer<UploadFileMessage> send) {
        var uuid = UUID.randomUUID();

        var remaining = MAX_PACKET_SIZE;
        for (var file : files) remaining -= file.getName().length() * 4 + FileUpload.CHECKSUM_LENGTH;

        var first = true;
        List<FileSlice> slices = new ArrayList<>(files.size());
        for (var fileId = 0; fileId < files.size(); fileId++) {
            var file = files.get(fileId);
            var contents = file.getBytes();
            var capacity = contents.limit();

            var currentOffset = 0;
            while (currentOffset < capacity) {
                if (remaining <= 0) {
                    send.accept(first
                        ? new UploadFileMessage(container, uuid, FLAG_FIRST, files, new ArrayList<>(slices))
                        : new UploadFileMessage(container, uuid, 0, null, new ArrayList<>(slices)));
                    slices.clear();
                    remaining = MAX_PACKET_SIZE;
                    first = false;
                }

                var canWrite = Math.min(remaining, capacity - currentOffset);

                contents.position(currentOffset).limit(currentOffset + canWrite);
                slices.add(new FileSlice(fileId, currentOffset, contents.slice()));
                currentOffset += canWrite;
                remaining -= canWrite;
            }

            contents.position(0).limit(capacity);
        }

        send.accept(first
            ? new UploadFileMessage(container, uuid, FLAG_FIRST | FLAG_LAST, files, new ArrayList<>(slices))
            : new UploadFileMessage(container, uuid, FLAG_LAST, null, new ArrayList<>(slices)));
    }

    @Override
    protected void handle(ServerNetworkContext context, @Nonnull ComputerMenu container) {
        var player = context.getSender();
        if (player != null) {
            var input = container.getInput();
            if ((flag & FLAG_FIRST) != 0) input.startUpload(uuid, files);
            input.continueUpload(uuid, slices);
            if ((flag & FLAG_LAST) != 0) input.finishUpload(player, uuid);
        }
    }
}
