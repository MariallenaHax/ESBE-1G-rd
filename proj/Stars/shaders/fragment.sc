$input v_color0

#include <bgfx_shader.sh>

uniform vec4 StarsColor;
uniform vec4 ViewPositionAndTime;
void main() {
    gl_FragColor = vec4(v_color0.rgb * StarsColor.rgb * sin(ViewPositionAndTime.w * v_color0.a), v_color0.a);
}