#version 130

uniform mat4 u_mv;

in vec3 v_pos;

out vec2 f_pos;

void main() {
    gl_Position = gl_ProjectionMatrix * u_mv * vec4(v_pos.x, v_pos.y, 0, 1);
    f_pos = v_pos.xy;
}
