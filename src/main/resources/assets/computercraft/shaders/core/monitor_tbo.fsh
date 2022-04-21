#version 150

#moj_import <fog.glsl>

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

uniform sampler2D Sampler0; // Font
uniform int Width;
uniform int Height;
uniform usamplerBuffer Tbo;
uniform vec3 Palette[16];

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 fontPos;

out vec4 fragColor;

vec2 texture_corner(int index) {
    float x = 1.0 + float(index % 16) * (FONT_WIDTH + 2.0);
    float y = 1.0 + float(index / 16) * (FONT_HEIGHT + 2.0);
    return vec2(x, y);
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
    vec4 img = texture(Sampler0, (texture_corner(character) + pos) / 256.0);
    vec4 colour = vec4(mix(Palette[bg], img.rgb * Palette[fg], img.a * mult), 1.0) * ColorModulator;

    fragColor = linear_fog(colour, vertexDistance, FogStart, FogEnd, FogColor);
}
