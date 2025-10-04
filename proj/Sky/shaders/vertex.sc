$input a_color0, a_position
    $input a_texcoord0
    #ifdef INSTANCING__ON
        $input i_data1, i_data2, i_data3
    #endif

$output v_color0, v_texcoord0, v_worldPos, v_prevWorldPos

#include <bgfx_shader.sh>

uniform vec4 SkyColor;

void main() {
#ifdef OPAQUE_PASS
    v_color0 = a_color0;
    gl_Position = mul(u_modelViewProj, vec4(a_position, 1.0));
#endif
    mat4 model;
    #ifdef INSTANCING__ON
        model = mtxFromCols(i_data1, i_data2, i_data3, vec4(0, 0, 0, 1));
    #else
        model = u_model[0];
    #endif

    v_texcoord0 = a_texcoord0;
    vec3 pos = a_position;
    pos.y -= length(pos.xyz)*.2;
    v_worldPos = mul(model, vec4(pos, 1.0)).xyz;
    gl_Position = mul(u_modelViewProj, vec4(pos, 1.0));
    v_color0 = a_color0;
    v_prevWorldPos = a_position.xyz;
}