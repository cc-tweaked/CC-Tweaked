#version 430 core

#define FONT_WIDTH 6.0
#define FONT_HEIGHT 9.0

layout (location = 0) uniform mat4 u_mv;
layout (location = 1) uniform mat4 u_p;

layout (location = 0) in vec3 v_pos;

out vec2 f_pos;

void main() {
    gl_Position = u_p * u_mv * vec4(v_pos.x, v_pos.y, 0, 1);
    f_pos = v_pos.xy;
}
