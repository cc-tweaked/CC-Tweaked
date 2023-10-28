// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

#version 450 core

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

layout(location = 0) uniform sampler2D Font;
layout(location = 1) uniform usamplerBuffer Tbo;

layout(std140, binding = 0) uniform TermData {
    vec3 Palette[16];
    int Width;
    int Height;
    ivec2 CursorPos;
    int CursorColour;
};
layout(location = 2) uniform int CursorBlink;

in vec2 fontPos;

out vec4 fragColor;

vec2 texture_corner(int index) {
    float x = 1.0 + float(index % 16) * (FONT_WIDTH + 2.0);
    float y = 1.0 + float(index / 16) * (FONT_HEIGHT + 2.0);
    return vec2(x, y);
}

vec4 recolour(vec4 texture, int colour) {
    return vec4(texture.rgb * Palette[colour], texture.rgba);
}

void main() {
    vec2 term_pos = vec2(fontPos.x / FONT_WIDTH, fontPos.y / FONT_HEIGHT);
    vec2 corner = floor(term_pos);

    ivec2 cell = ivec2(corner);
    int index = 3 * (clamp(cell.x, 0, Width - 1) + clamp(cell.y, 0, Height - 1) * Width);

    // 1 if 0 <= x, y < Width, Height, 0 otherwise
    vec2 outside = step(vec2(0.0, 0.0), vec2(cell)) * step(vec2(cell), vec2(float(Width) - 1.0, float(Height) - 1.0));
    float mult = outside.x * outside.y;

    int character = int(texelFetch(Tbo, index).r);
    int fg = int(texelFetch(Tbo, index + 1).r);
    int bg = int(texelFetch(Tbo, index + 2).r);

    vec2 pos = (term_pos - corner) * vec2(FONT_WIDTH, FONT_HEIGHT);
    vec4 charTex = recolour(texture(Font, (texture_corner(character) + pos) / 256.0), fg);

    // Applies the cursor on top of the current character if we're blinking and in the current cursor's cell. We do it
    // this funky way to avoid branches.
    vec4 cursorTex = recolour(texture(Font, (texture_corner(95) + pos) / 256.0), CursorColour); // 95 = '_'
    vec4 img = mix(charTex, cursorTex, cursorTex.a * float(CursorBlink) * (CursorPos == cell ? 1.0 : 0.0));

    fragColor = vec4(mix(Palette[bg], img.rgb, img.a * mult), 1.0);
}
