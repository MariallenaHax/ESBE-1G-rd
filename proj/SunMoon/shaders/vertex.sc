$input a_position, a_texcoord0
$output v_texcoord0,v_worldPos,v_color0

#include <bgfx_shader.sh>

void main()
{
    vec3 esb_u = vec3_splat(1.);
    esb_u.xz *= 20.;
    vec3 position = a_position * esb_u;
    gl_Position = mul(u_modelViewProj, vec4(position, 1.0));

    v_texcoord0 = a_texcoord0;
    v_worldPos = a_position;
    v_worldPos.xz *= 15.;
    v_color0 = mul(vec4(a_position, 1.0),u_modelViewProj);
}