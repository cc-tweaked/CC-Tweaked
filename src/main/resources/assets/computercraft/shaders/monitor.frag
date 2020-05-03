#version 430 core

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

layout (location = 2) uniform sampler2D u_font;
layout (location = 3) uniform int u_width;
layout (location = 4) uniform int u_height;
layout (location = 5) uniform samplerBuffer u_tbo;
layout (location = 6) uniform vec3 u_palette[16];

in vec2 f_pos;

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

    if(cell.x < 0 || cell.y < 0 || cell.x >= u_width || cell.y >= u_height) {
        int bg = int(texelFetch(u_tbo, index + 2).r * 255.0);
        gl_FragColor = vec4(u_palette[bg], 1.0);
        return;
    }

    int character = int(texelFetch(u_tbo, index).r * 255.0);
    int fg = int(texelFetch(u_tbo, index + 1).r * 255.0);
    int bg = int(texelFetch(u_tbo, index + 2).r * 255.0);

    vec2 pos = (term_pos - corner) * vec2(FONT_WIDTH, FONT_HEIGHT);
    vec4 img = texture2D(u_font, (texture_corner(character) + pos) / 256.0);
    gl_FragColor = vec4(mix(u_palette[bg], img.rgb * u_palette[fg], img.a), 1.0);
}
