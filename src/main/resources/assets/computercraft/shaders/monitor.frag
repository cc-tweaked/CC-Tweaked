#version 140

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

uniform sampler2D u_font;
uniform int u_width;
uniform int u_height;
uniform samplerBuffer u_tbo;
uniform vec3 u_palette[16];

in vec2 f_pos;

out vec4 colour;

vec2 texture_corner(int index) {
    float x = 1.0 + float(index % 16) * (FONT_WIDTH + 2.0);
    float y = 1.0 + float(index / 16) * (FONT_HEIGHT + 2.0);
    return vec2(x, y);
}

void main() {
    vec2 term_pos = vec2(f_pos.x / FONT_WIDTH, f_pos.y / FONT_HEIGHT);
    vec2 corner = floor(term_pos);

    ivec2 cell = ivec2(corner);
    int index = 3 * (clamp(cell.x, 0, u_width - 1) + clamp(cell.y, 0, u_height - 1) * u_width);

    // 1 if 0 <= x, y < width, height, 0 otherwise
    vec2 outside = step(vec2(0.0, 0.0), vec2(cell)) * step(vec2(cell), vec2(float(u_width) - 1.0, float(u_height) - 1.0));
    float mult = outside.x * outside.y;

    int character = int(texelFetch(u_tbo, index).r * 255.0);
    int fg = int(texelFetch(u_tbo, index + 1).r * 255.0);
    int bg = int(texelFetch(u_tbo, index + 2).r * 255.0);

    vec2 pos = (term_pos - corner) * vec2(FONT_WIDTH, FONT_HEIGHT);
    vec4 img = texture(u_font, (texture_corner(character) + pos) / 256.0);
    colour = vec4(mix(u_palette[bg], img.rgb * u_palette[fg], img.a * mult), 1.0);
}
