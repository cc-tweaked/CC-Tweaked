// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.nio.charset.StandardCharsets;

public final class StringUtil {
    private StringUtil() {
    }

    public static String normaliseLabel(String label) {
        var length = Math.min(32, label.length());
        var builder = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            var c = label.charAt(i);
            if ((c >= ' ' && c <= '~') || (c >= 161 && c <= 172) || (c >= 174 && c <= 255)) {
                builder.append(c);
            } else {
                builder.append('?');
            }
        }

        return builder.toString();
    }

    public static String byteStringToUtf8(String s){
        return String.valueOf(StandardCharsets.UTF_8.decode(StandardCharsets.ISO_8859_1.encode(s)));
    }

    public static String utfToByteString(String s){
        return String.valueOf(StandardCharsets.ISO_8859_1.decode(StandardCharsets.UTF_8.encode(s)));
    }

    public static int getCodepointWidth(int cp){
        return getCodepointWidth(cp, false);
    }

    public static int getCodepointWidth(int cp, boolean isAmbiguousFullwidth){
        if(cp < 256) {
            // backward compatibility
            return 1;
        }
        else if(0x3000 == cp ||
            (0xff01 <= cp && cp <= 0xff60) ||
            (0xffe0 <= cp && cp <= 0xffe6) ||
            (0x1100 <= cp && cp <= 0x115F) ||
            (0x11A3 <= cp && cp <= 0x11A7) ||
            (0x11FA <= cp && cp <= 0x11FF) ||
            (0x2329 <= cp && cp <= 0x232A) ||
            (0x2E80 <= cp && cp <= 0x2E99) ||
            (0x2E9B <= cp && cp <= 0x2EF3) ||
            (0x2F00 <= cp && cp <= 0x2FD5) ||
            (0x2FF0 <= cp && cp <= 0x2FFB) ||
            (0x3001 <= cp && cp <= 0x303E) ||
            (0x3041 <= cp && cp <= 0x3096) ||
            (0x3099 <= cp && cp <= 0x30FF) ||
            (0x3105 <= cp && cp <= 0x312D) ||
            (0x3131 <= cp && cp <= 0x318E) ||
            (0x3190 <= cp && cp <= 0x31BA) ||
            (0x31C0 <= cp && cp <= 0x31E3) ||
            (0x31F0 <= cp && cp <= 0x321E) ||
            (0x3220 <= cp && cp <= 0x3247) ||
            (0x3250 <= cp && cp <= 0x32FE) ||
            (0x3300 <= cp && cp <= 0x4DBF) ||
            (0x4E00 <= cp && cp <= 0xA48C) ||
            (0xA490 <= cp && cp <= 0xA4C6) ||
            (0xA960 <= cp && cp <= 0xA97C) ||
            (0xAC00 <= cp && cp <= 0xD7A3) ||
            (0xD7B0 <= cp && cp <= 0xD7C6) ||
            (0xD7CB <= cp && cp <= 0xD7FB) ||
            (0xF900 <= cp && cp <= 0xFAFF) ||
            (0xFE10 <= cp && cp <= 0xFE19) ||
            (0xFE30 <= cp && cp <= 0xFE52) ||
            (0xFE54 <= cp && cp <= 0xFE66) ||
            (0xFE68 <= cp && cp <= 0xFE6B) ||
            (0x1B000 <= cp && cp <= 0x1B001) ||
            (0x1F200 <= cp && cp <= 0x1F202) ||
            (0x1F210 <= cp && cp <= 0x1F23A) ||
            (0x1F240 <= cp && cp <= 0x1F248) ||
            (0x1F250 <= cp && cp <= 0x1F251) ||
            (0x20000 <= cp && cp <= 0x2F73F) ||
            (0x2B740 <= cp && cp <= 0x2FFFD) ||
            (0x30000 <= cp && cp <= 0x3FFFD)
        ){
            // East Asian Fullwidth and East Asian Wide
            return 2;
        }
        else if((0x0101 == cp) ||
            (0x0111 == cp) ||
            (0x0113 == cp) ||
            (0x011B == cp) ||
            (0x0126 <= cp && cp <= 0x0127) ||
            (0x012B == cp) ||
            (0x0131 <= cp && cp <= 0x0133) ||
            (0x0138 == cp) ||
            (0x013F <= cp && cp <= 0x0142) ||
            (0x0144 == cp) ||
            (0x0148 <= cp && cp <= 0x014B) ||
            (0x014D == cp) ||
            (0x0152 <= cp && cp <= 0x0153) ||
            (0x0166 <= cp && cp <= 0x0167) ||
            (0x016B == cp) ||
            (0x01CE == cp) ||
            (0x01D0 == cp) ||
            (0x01D2 == cp) ||
            (0x01D4 == cp) ||
            (0x01D6 == cp) ||
            (0x01D8 == cp) ||
            (0x01DA == cp) ||
            (0x01DC == cp) ||
            (0x0251 == cp) ||
            (0x0261 == cp) ||
            (0x02C4 == cp) ||
            (0x02C7 == cp) ||
            (0x02C9 <= cp && cp <= 0x02CB) ||
            (0x02CD == cp) ||
            (0x02D0 == cp) ||
            (0x02D8 <= cp && cp <= 0x02DB) ||
            (0x02DD == cp) ||
            (0x02DF == cp) ||
            (0x0300 <= cp && cp <= 0x036F) ||
            (0x0391 <= cp && cp <= 0x03A1) ||
            (0x03A3 <= cp && cp <= 0x03A9) ||
            (0x03B1 <= cp && cp <= 0x03C1) ||
            (0x03C3 <= cp && cp <= 0x03C9) ||
            (0x0401 == cp) ||
            (0x0410 <= cp && cp <= 0x044F) ||
            (0x0451 == cp) ||
            (0x2010 == cp) ||
            (0x2013 <= cp && cp <= 0x2016) ||
            (0x2018 <= cp && cp <= 0x2019) ||
            (0x201C <= cp && cp <= 0x201D) ||
            (0x2020 <= cp && cp <= 0x2022) ||
            (0x2024 <= cp && cp <= 0x2027) ||
            (0x2030 == cp) ||
            (0x2032 <= cp && cp <= 0x2033) ||
            (0x2035 == cp) ||
            (0x203B == cp) ||
            (0x203E == cp) ||
            (0x2074 == cp) ||
            (0x207F == cp) ||
            (0x2081 <= cp && cp <= 0x2084) ||
            (0x20AC == cp) ||
            (0x2103 == cp) ||
            (0x2105 == cp) ||
            (0x2109 == cp) ||
            (0x2113 == cp) ||
            (0x2116 == cp) ||
            (0x2121 <= cp && cp <= 0x2122) ||
            (0x2126 == cp) ||
            (0x212B == cp) ||
            (0x2153 <= cp && cp <= 0x2154) ||
            (0x215B <= cp && cp <= 0x215E) ||
            (0x2160 <= cp && cp <= 0x216B) ||
            (0x2170 <= cp && cp <= 0x2179) ||
            (0x2189 == cp) ||
            (0x2190 <= cp && cp <= 0x2199) ||
            (0x21B8 <= cp && cp <= 0x21B9) ||
            (0x21D2 == cp) ||
            (0x21D4 == cp) ||
            (0x21E7 == cp) ||
            (0x2200 == cp) ||
            (0x2202 <= cp && cp <= 0x2203) ||
            (0x2207 <= cp && cp <= 0x2208) ||
            (0x220B == cp) ||
            (0x220F == cp) ||
            (0x2211 == cp) ||
            (0x2215 == cp) ||
            (0x221A == cp) ||
            (0x221D <= cp && cp <= 0x2220) ||
            (0x2223 == cp) ||
            (0x2225 == cp) ||
            (0x2227 <= cp && cp <= 0x222C) ||
            (0x222E == cp) ||
            (0x2234 <= cp && cp <= 0x2237) ||
            (0x223C <= cp && cp <= 0x223D) ||
            (0x2248 == cp) ||
            (0x224C == cp) ||
            (0x2252 == cp) ||
            (0x2260 <= cp && cp <= 0x2261) ||
            (0x2264 <= cp && cp <= 0x2267) ||
            (0x226A <= cp && cp <= 0x226B) ||
            (0x226E <= cp && cp <= 0x226F) ||
            (0x2282 <= cp && cp <= 0x2283) ||
            (0x2286 <= cp && cp <= 0x2287) ||
            (0x2295 == cp) ||
            (0x2299 == cp) ||
            (0x22A5 == cp) ||
            (0x22BF == cp) ||
            (0x2312 == cp) ||
            (0x2460 <= cp && cp <= 0x24E9) ||
            (0x24EB <= cp && cp <= 0x254B) ||
            (0x2550 <= cp && cp <= 0x2573) ||
            (0x2580 <= cp && cp <= 0x258F) ||
            (0x2592 <= cp && cp <= 0x2595) ||
            (0x25A0 <= cp && cp <= 0x25A1) ||
            (0x25A3 <= cp && cp <= 0x25A9) ||
            (0x25B2 <= cp && cp <= 0x25B3) ||
            (0x25B6 <= cp && cp <= 0x25B7) ||
            (0x25BC <= cp && cp <= 0x25BD) ||
            (0x25C0 <= cp && cp <= 0x25C1) ||
            (0x25C6 <= cp && cp <= 0x25C8) ||
            (0x25CB == cp) ||
            (0x25CE <= cp && cp <= 0x25D1) ||
            (0x25E2 <= cp && cp <= 0x25E5) ||
            (0x25EF == cp) ||
            (0x2605 <= cp && cp <= 0x2606) ||
            (0x2609 == cp) ||
            (0x260E <= cp && cp <= 0x260F) ||
            (0x2614 <= cp && cp <= 0x2615) ||
            (0x261C == cp) ||
            (0x261E == cp) ||
            (0x2640 == cp) ||
            (0x2642 == cp) ||
            (0x2660 <= cp && cp <= 0x2661) ||
            (0x2663 <= cp && cp <= 0x2665) ||
            (0x2667 <= cp && cp <= 0x266A) ||
            (0x266C <= cp && cp <= 0x266D) ||
            (0x266F == cp) ||
            (0x269E <= cp && cp <= 0x269F) ||
            (0x26BE <= cp && cp <= 0x26BF) ||
            (0x26C4 <= cp && cp <= 0x26CD) ||
            (0x26CF <= cp && cp <= 0x26E1) ||
            (0x26E3 == cp) ||
            (0x26E8 <= cp && cp <= 0x26FF) ||
            (0x273D == cp) ||
            (0x2757 == cp) ||
            (0x2776 <= cp && cp <= 0x277F) ||
            (0x2B55 <= cp && cp <= 0x2B59) ||
            (0x3248 <= cp && cp <= 0x324F) ||
            (0xE000 <= cp && cp <= 0xF8FF) ||
            (0xFE00 <= cp && cp <= 0xFE0F) ||
            (0xFFFD == cp) ||
            (0x1F100 <= cp && cp <= 0x1F10A) ||
            (0x1F110 <= cp && cp <= 0x1F12D) ||
            (0x1F130 <= cp && cp <= 0x1F169) ||
            (0x1F170 <= cp && cp <= 0x1F19A) ||
            (0xE0100 <= cp && cp <= 0xE01EF) ||
            (0xF0000 <= cp && cp <= 0xFFFFD) ||
            (0x100000 <= cp && cp <= 0x10FFFD)){
            // East Asian Ambiguous
            return isAmbiguousFullwidth ? 2 : 1;
        }
        // East Asian Halfwidth, East Asian Narrow, East Asian Neutral
        return 1;
    }
}
