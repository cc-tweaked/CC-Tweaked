// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.test.core.ArbitraryByteBuffer;
import dan200.computercraft.test.shared.FakeContainer;
import dan200.computercraft.test.shared.WithMinecraft;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.network.FriendlyByteBuf;
import org.hamcrest.Matcher;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dan200.computercraft.shared.network.server.UploadFileMessage.*;
import static dan200.computercraft.test.core.ByteBufferMatcher.bufferEqual;
import static dan200.computercraft.test.core.ContramapMatcher.contramap;
import static dan200.computercraft.test.core.CustomMatchers.containsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WithMinecraft
public class UploadFileMessageTest {
    /**
     * Sends packets on a roundtrip, ensuring that their contents are reassembled on the other end.
     *
     * @param sentFiles The files to send.
     */
    @Property(tries = 200)
    @Tag("slow")
    public void testRoundTrip(@ForAll("fileUploads") List<FileUpload> sentFiles) {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
        var receivedFiles = receive(roundtripPackets(send(sentFiles)));
        assertThat(receivedFiles, containsWith(sentFiles, UploadFileMessageTest::uploadEqual));
    }

    /**
     * "Send" our file uploads, converting them to a list of packets.
     *
     * @param uploads The files to send.
     * @return The list of packets.
     */
    private static List<UploadFileMessage> send(List<FileUpload> uploads) {
        List<UploadFileMessage> packets = new ArrayList<>();
        UploadFileMessage.send(new FakeContainer(), uploads, packets::add);
        return packets;
    }

    /**
     * Write our packets to a buffer and then read them out again.
     *
     * @param packets The packets to roundtrip.
     * @return The
     */
    private static List<UploadFileMessage> roundtripPackets(List<UploadFileMessage> packets) {
        return packets.stream().map(packet -> {
            var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
            packet.write(buffer);
            // We include things like file size in the packet, but not in the count, so grant a slightly larger threshold.
            assertThat("Packet is too large", buffer.writerIndex(), lessThanOrEqualTo(MAX_PACKET_SIZE + 128));
            if ((packet.flag & FLAG_LAST) == 0) {
                var expectedSize = (packet.flag & FLAG_FIRST) != 0
                    ? MAX_PACKET_SIZE - MAX_FILE_NAME * MAX_FILES
                    : MAX_PACKET_SIZE;
                assertThat(
                    "Non-final packets should be efficiently packed", buffer.writerIndex(), greaterThanOrEqualTo(expectedSize)
                );
            }

            var result = new UploadFileMessage(buffer);

            buffer.release();
            assertEquals(0, buffer.refCnt(), "Buffer should have no references");

            return result;
        }).collect(Collectors.toList());
    }

    /**
     * "Receive" our upload packets.
     *
     * @param packets The packets to receive. Note that this will clobber the {@link FileUpload}s in the first packet,
     *                so you may want to copy (or {@linkplain #roundtripPackets(List) roundtrip} first.
     * @return The consumed file uploads.
     */
    private static List<FileUpload> receive(List<UploadFileMessage> packets) {
        var files = packets.get(0).files;
        for (var i = 0; i < packets.size(); i++) {
            var packet = packets.get(i);
            var isFirst = i == 0;
            var isLast = i == packets.size() - 1;
            assertEquals(isFirst, (packet.flag & FLAG_FIRST) != 0, "FLAG_FIRST");
            assertEquals(isLast, (packet.flag & FLAG_LAST) != 0, "FLAG_LAST");

            for (var slice : packet.slices) slice.apply(files);
        }

        return files;
    }

    @Provide
    Arbitrary<FileUpload> fileUpload() {
        return Combinators.combine(
            Arbitraries.oneOf(List.of(
                // 1.16 doesn't correctly handle unicode file names. We'll be generous in our tests here.
                Arbitraries.strings().ofMinLength(1).ascii().ofMaxLength(MAX_FILE_NAME),
                Arbitraries.strings().ofMinLength(1).ofMaxLength(MAX_FILE_NAME / 4)
            )),
            ArbitraryByteBuffer.bytes().ofMaxSize(Config.uploadMaxSize)
        ).as(UploadFileMessageTest::file);
    }

    @Provide
    Arbitrary<List<FileUpload>> fileUploads() {
        return fileUpload().list()
            .ofMinSize(1).ofMaxSize(MAX_FILES)
            .filter(us -> us.stream().mapToInt(u -> u.getBytes().remaining()).sum() <= Config.uploadMaxSize);
    }

    private static FileUpload file(String name, ByteBuffer buffer) {
        var checksum = FileUpload.getDigest(buffer);
        if (checksum == null) throw new IllegalStateException("Failed to compute checksum");

        return new FileUpload(name, buffer, checksum);
    }

    public static Matcher<FileUpload> uploadEqual(FileUpload upload) {
        return allOf(
            contramap(equalTo(upload.getName()), "name", FileUpload::getName),
            contramap(equalTo(upload.getChecksum()), "checksum", FileUpload::getChecksum),
            contramap(bufferEqual(upload.getBytes()), "bytes", FileUpload::getBytes)
        );
    }
}
