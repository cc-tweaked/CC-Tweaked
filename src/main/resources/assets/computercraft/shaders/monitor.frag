#version 140

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

uniform sampler2D u_font;
uniform usamplerBuffer u_tbo;

layout(std140) uniform u_monitor {
    vec3 u_palette[16];
    int u_width;
    int u_height;
    ivec2 u_cursorPos;
    int u_cursorColour;
};
uniform int u_cursorBlink;

in vec2 f_pos;

out vec4 colour;

vec2 texture_corner(int index) {
    float x = 1.0 + float(index % 16) * (FONT_WIDTH + 2.0);
    float y = 1.0 + float(index / 16) * (FONT_HEIGHT + 2.0);
    return vec2(x, y);
}

vec4 recolour(vec4 texture, int colour) {
    return vec4(texture.rgb * u_palette[colour], texture.rgba);
}

void main() {
    vec2 term_pos = vec2(f_pos.x / FONT_WIDTH, f_pos.y / FONT_HEIGHT);
    vec2 corner = floor(term_pos);

    ivec2 cell = ivec2(corner);
    int index = 3 * (clamp(cell.x, 0, u_width - 1) + clamp(cell.y, 0, u_height - 1) * u_width);

    // 1 if 0 <= x, y < width, height, 0 otherwise
    vec2 outside = step(vec2(0.0, 0.0), vec2(cell)) * step(vec2(cell), vec2(float(u_width) - 1.0, float(u_height) - 1.0));
    float mult = outside.x * outside.y;

    int character = int(texelFetch(u_tbo, index).r);
    int fg = int(texelFetch(u_tbo, index + 1).r);
    int bg = int(texelFetch(u_tbo, index + 2).r);

    vec2 pos = (term_pos - corner) * vec2(FONT_WIDTH, FONT_HEIGHT);
    vec4 charTex = recolour(texture(u_font, (texture_corner(character) + pos) / 256.0), fg);

    // Applies the cursor on top of the current character if we're blinking and in the current cursor's cell. We do it
    // this funky way to avoid branches.
    vec4 cursorTex = recolour(texture(u_font, (texture_corner(95) + pos) / 256.0), u_cursorColour); // 95 = '_'
    vec4 img = mix(charTex, cursorTex, cursorTex.a * float(u_cursorBlink) * (u_cursorPos == cell ? 1.0 : 0.0));

    colour = vec4(mix(u_palette[bg], img.rgb, img.a * mult), 1.0);
}
