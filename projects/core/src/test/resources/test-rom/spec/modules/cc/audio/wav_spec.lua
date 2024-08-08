-- SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("cc.audio.wav", function()
    local wav = require "cc.audio.wav"

    describe("readWAV", function()
        it("parses an 8-bit unsigned file", function()
            local input = "RIFF\44\0\0\0WAVEfmt \16\0\0\0\1\0\1\0\x44\xAC\0\0\x44\xAC\0\0\1\0\8\0data\8\0\0\0\x7C\x7D\x7E\x7F\x80\x81\x82\x83"

            local decoded = wav.readWAV(input)
            expect(decoded.codec):describe("The codec matches"):eq("u8")
            expect(decoded.length):describe("The length matches"):eq(8)
            expect(decoded.channels):describe("The channels match"):eq(1)
            expect(decoded.sampleRate):describe("The sample rate matches"):eq(44100)

            local chunk = decoded.read(8)
            expect(chunk):describe("The chunk is read successfully"):type("table")
            expect(#chunk):describe("The chunk length is correct"):eq(8)
            expect(decoded.read(1)):describe("The reader is finished"):eq(nil)

            for i = 1, 8 do expect(chunk[i]):describe("Item at #" .. i):eq(i - 5) end
        end)

        it("parses a stereo 16-bit signed file", function()
            local input = "RIFF\68\0\0\0WAVEfmt \16\0\0\0\1\0\2\0\x80\xBB\0\0\0\xEE\2\0\4\0\16\0data\32\0\0\0\0\xFC\0\3\0\xFD\0\2\0\xFE\0\1\0\xFF\0\0\0\0\0\xFF\0\1\0\xFE\0\2\0\xFD\0\3\0\xFC"

            local decoded = wav.readWAV(input)
            expect(decoded.codec):describe("The codec matches"):eq("s16")
            expect(decoded.length):describe("The length matches"):eq(8)
            expect(decoded.channels):describe("The channels match"):eq(2)
            expect(decoded.sampleRate):describe("The sample rate matches"):eq(48000)

            local chunkL, chunkR = decoded.read(8)
            expect(chunkL):describe("The left chunk is read successfully"):type("table")
            expect(chunkR):describe("The right chunk is read successfully"):type("table")
            expect(#chunkL):describe("The left chunk length is correct"):eq(8)
            expect(#chunkR):describe("The right chunk length is correct"):eq(8)
            expect(decoded.read(1)):describe("The reader is finished"):eq(nil)

            for i = 1, 8 do
                expect(chunkL[i]):describe("Left item at #" .. i):eq(i - 5)
                expect(chunkR[i]):describe("Right item at #" .. i):eq(4 - i)
            end
        end)

        it("parses a DFPWM-WAV file with metadata", function()
            local input = "RIFF\27\1\0\0WAVEfmt \x28\x00\x00\x00\xFE\xFF\x01\x00\x80\xBB\x00\x00\x70\x17\x00\x00\x01\x00\x01\x00\x16\x00\x01\x00\x01\x00\x00\x00\x3A\xC1\xFA\x38\x81\x1D\x43\x61\xA4\x0D\xCE\x53\xCA\x60\x7C\xD1\x66\x61\x63\x74\x04\x00\x00\x00\x00\x04\x00\x00\x4C\x49\x53\x54\x4A\x00\x00\x00\x49\x4E\x46\x4F\x49\x41\x52\x54\x05\x00\x00\x00\x74\x65\x73\x74\x00\x00\x49\x43\x52\x44\x05\x00\x00\x00\x32\x30\x32\x34\x00\x00\x49\x50\x52\x44\x0C\x00\x00\x00\x43\x43\x3A\x20\x54\x77\x65\x61\x6B\x65\x64\x00\x49\x53\x46\x54\x0E\x00\x00\x00\x4C\x61\x76\x66\x35\x39\x2E\x31\x38\x2E\x31\x30\x30\x00data\128\0\0\0\43\225\33\44\30\240\171\23\253\201\46\186\68\189\74\160\188\16\94\169\251\87\11\240\19\92\85\185\126\5\172\64\17\250\85\245\255\169\244\1\85\200\33\176\82\104\163\17\126\23\91\226\37\224\117\184\198\11\180\19\148\86\191\246\255\188\231\10\210\85\124\202\15\232\43\162\117\63\220\15\250\88\87\230\173\106\41\13\228\143\246\190\119\169\143\68\201\40\149\62\20\72\3\160\114\169\254\39\152\30\20\42\84\24\47\64\43\61\221\95\191\42\61\42\206\4\247\81"
            local output = { 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 0, -1, -2, -2, -1, 0, 1, 0, -1, -3, -5, -5, -5, -7, -9, -11, -11, -9, -9, -9, -9, -10, -12, -12, -10, -8, -6, -6, -8, -10, -12, -14, -16, -18, -17, -15, -12, -9, -6, -3, -2, -2, -2, -2, -2, -2, 0, 3, 6, 7, 7, 7, 4, 1, 1, 1, 1, 3, 5, 7, 9, 12, 15, 15, 12, 12, 12, 9, 9, 11, 12, 12, 14, 16, 17, 17, 17, 14, 11, 11, 11, 10, 12, 14, 14, 13, 13, 10, 9, 9, 7, 5, 4, 4, 4, 4, 4, 6, 8, 10, 10, 10, 10, 10, 10, 10, 9, 8, 8, 8, 7, 6, 4, 2, 0, 0, 0, 0, 0, -1, -1, 0, 1, 3, 3, 3, 3, 2, 0, -2, -2, -2, -3, -5, -7, -7, -5, -3, -1, -1, -1, -1, -1, -1, -2, -2, -1, -1, -1, -1, 0, 1, 1, 1, 2, 3, 4, 5, 6, 7, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 9, 8, 7, 6, 4, 2, 0, 0, 2, 4, 6, 8, 10, 10, 8, 7, 7, 5, 3, 1, -1, 0, 2, 4, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 4, 5, 5, 5, 5, 5, 6, 7, 8, 9, 10, 9, 9, 9, 9, 9, 8, 7, 6, 5, 3, 1, 1, 3, 3, 3, 3, 3, 3, 2, 1, 0, -1, -3, -3, -3, -3, -2, -3, -4, -4, -3, -4, -5, -6, -6, -5, -5, -4, -3, -2, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 20, 17, 16, 16, 15, 15, 15, 15, 13, 13, 13, 13, 14, 15, 16, 18, 18, 16, 14, 12, 10, 8, 5, 5, 5, 4, 4, 4, 4, 4, 4, 2, 0, -2, -2, -2, -4, -4, -2, 0, 0, -2, -4, -6, -6, -6, -8, -10, -12, -14, -16, -15, -13, -12, -11, -11, -11, -11, -13, -13, -13, -13, -13, -14, -16, -18, -18, -18, -18, -16, -16, -16, -14, -13, -14, -15, -15, -14, -14, -12, -11, -12, -13, -13, -12, -13, -14, -15, -15, -13, -11, -9, -7, -5, -5, -5, -3, -1, -1, -1, -1, -3, -5, -5, -3, -3, -3, -1, -1, -1, -1, -3, -3, -3, -4, -6, -6, -4, -2, 0, 0, 0, 0, -2, -2, -2, -3, -5, -7, -9, -11, -13, -13, -11, -9, -7, -6, -6, -6, -6, -4, -2, -2, -4, -6, -8, -7, -5, -3, -2, -2, -2, -2, 0, 0, -2, -4, -4, -2, 0, 2, 2, 1, 1, -1, -3, -5, -7, -10, -10, -10, -10, -8, -7, -7, -5, -3, -2, -4, -4, -4, -6, -8, -10, -12, -12, -12, -12, -12, -14, -13, -13, -13, -11, -11, -11, -11, -11, -11, -11, -9, -7, -5, -3, -1, -1, -1, -1, -1, 1, 1, 1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 22, 19, 18, 20, 22, 24, 23, 22, 24, 26, 28, 27, 24, 23, 25, 28, 28, 28, 27, 26, 26, 23, 20, 17, 14, 14, 14, 11, 11, 11, 11, 13, 15, 16, 16, 16, 15, 15, 14, 14, 12, 10, 9, 11, 13, 15, 17, 17, 14, 13, 13, 12, 12, 10, 9, 11, 13, 15, 17, 19, 19, 16, 13, 10, 7, 4, 1, 1, 2, 2, 4, 7, 10, 13, 13, 13, 12, 12, 12, 9, 6, 6, 6, 3, 0, 0, 0, 0, 2, 3, 3, 3, 3, 5, 7, 7, 7, 9, 11, 13, 15, 18, 18, 15, 12, 9, 8, 10, 13, 13, 13, 15, 18, 21, 24, 27, 27, 23, 19, 15, 11, 10, 9, 9, 12, 16, 19, 22, 23, 19, 14, 13, 16, 16, 15, 15, 14, 17, 20, 20, 19, 19, 18, 17, 14, 13, 15, 15, 12, 11, 13, 16, 19, 19, 18, 20, 20, 19, 18, 18, 17, 17, 16, 16, 16, 15, 17, 17, 16, 16, 13, 12, 12, 11, 11, 9, 9, 9, 9, 11, 11, 9, 7, 5, 3, 1, 1, 1, -1, -1, 1, 3, 5, 7, 9, 11, 12, 9, 6, 6, 6, 6, 8, 8, 7, 9, 11, 13, 13, 12, 14, 16, 18, 20, 20, 20, 22, 24, 26, 25, 25, 27, 29, 28, 27, 26, 23, 22, 22, 21, 21, 20, 22, 24, 26, 28, 27, 24, 21, 21, 21, 18, 17, 17, 14, 11, 11, 11, 10, 10, 7, 6, 6, 4, 3, 5, 5, 3, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, 0, -1, -1, 0, 0, 1, 2, 3, 4, 3, 1, -1, -3, -3, -3, -3, -2, -3, -4, -6, -8, -10, -10, -10, -12, -12, -12, -12, -10, -10, -11, -12, -14, -16, -18, -20, -22, -24, -26, -28, -27, -27, -26, -26, -25, -25, -27, -26, -24, -22, -22, -22, -22, -24, -24, -24, -24, -23, -23, -22, -22, -21, -20, -19, -17, -15, -13, -11, -9, -7, -7, -9, -9, -9, -11, -13, -15, -17, -16, -14, -13, -15, -14, -14, -14, -12, -10, -8, -7, -9, -11, -13, -15, -14, -14, -13, -13, -15, -17, -19, -18, -18, -17, -17, -16, -16, -18, -20, -22, -21, -21, -21, -21, -21, -20, -21, -22, -24, -24, -22, -22, -24, -26, -25, -23, -21, -19, -18, -17, -17, -19, -21, -23, -25, -27, -29, -31, -30, -29, -28, -26, -25, -24, -24, -23, -23, -25, -24, -24, -24, -22, -20, -18, -18, -20, -20, -20, -20, -18, -16, -16, -16, -14, -12, -10, -8, -6, -4, -4, -4, -4, -4, -2, 0, 2, 4, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 3, 3, 3, 3, 4, 5, 6, 5, 3, 1, 1, 1, 1, 1, 1, 1, 0, -1, -1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, -1, -2, -3, -4, -4, -2, 0, 0, 0, 1, 3, 5, 7, 7, 5, 3, 3, 3, 3, 3 }

            local decoded = wav.readWAV(input)
            expect(decoded.codec):describe("The codec matches"):eq("dfpwm")
            expect(decoded.length):describe("The length matches"):eq(1024)
            expect(decoded.channels):describe("The channels match"):eq(1)
            expect(decoded.sampleRate):describe("The sample rate matches"):eq(48000)
            expect(decoded.metadata.artist):describe("The artist matches"):eq("test")
            expect(decoded.metadata.date):describe("The date matches"):eq(2024)
            expect(decoded.metadata.album):describe("The album matches"):eq("CC: Tweaked")
            expect(decoded.metadata.encoder):describe("The encoder matches"):eq("Lavf59.18.100")

            local chunk = decoded.read(1024)
            expect(chunk):describe("The chunk is read successfully"):type("table")
            expect(#chunk):describe("The chunk length is correct"):eq(1024)
            expect(decoded.read(1)):describe("The reader is finished"):eq(nil)

            for i = 1, #chunk do expect(chunk[i]):describe("Item at #" .. i):eq(output[i]) end
        end)
    end)
end)