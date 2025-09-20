$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
     $input i_data0, i_data1, i_data2, i_data3
#endif
$output v_color0, v_fog, v_texcoord0, v_lightmapUV,v_worldPos,v_prevWorldPos

#include <bgfx_shader.sh>

uniform vec4 RenderChunkFogAlpha;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;
const float rA = 1.0;
const float rB = 1.0;
const vec3 UNIT_Y = vec3(0,1,0);
const float DIST_DESATURATION = 56.0 / 255.0;

highp float hash11(highp float p){
	highp vec3 p3  = vec3_splat(fract(p * 0.1031));
	p3 += dot(p3, p3.yzx + 19.19);
	return fract((p3.x + p3.y) * p3.z);
}

highp float random(highp float p){
		p = p/3.0+ViewPositionAndTime.w;
		return mix(hash11(floor(p)),hash11(ceil(p)),smoothstep(0.0,1.0,fract(p)))*2.0;
}
#define a_texcoord1 vec2(fract(a_texcoord1.x*15.9375),floor(a_texcoord1.x*15.9375)*0.0625)
void main() {
    mat4 model;
#ifdef INSTANCING__ON
    model = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
#else
    model = u_model[0];
#endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
    vec4 color;
#ifdef RENDER_AS_BILLBOARDS__ON
    worldPos += vec3(0.5, 0.5, 0.5);
    vec3 viewDir = normalize(worldPos - ViewPositionAndTime.xyz);
    vec3 boardPlane = normalize(vec3(viewDir.z, 0.0, -viewDir.x));
    worldPos = (worldPos -
        ((((viewDir.yzx * boardPlane.zxy) - (viewDir.zxy * boardPlane.yzx)) *
        (a_color0.z - 0.5)) +
        (boardPlane * (a_color0.x - 0.5))));
    color = vec4(1.0, 1.0, 1.0, 1.0);
#else
    color = a_color0;
#endif

    vec3 modelCamPos = (ViewPositionAndTime.xyz - worldPos);
    float camDis = length(modelCamPos);
    vec4 fogColor;
    fogColor.rgb = FogColor.rgb;
    fogColor.a = clamp(((((camDis / FogAndDistanceControl.z) + RenderChunkFogAlpha.x) -
        FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x)), 0.0, 1.0);

#ifdef TRANSPARENT_PASS
    if(a_color0.a < 0.95) {
        color.a = mix(a_color0.a, 1.0, clamp((camDis / FogAndDistanceControl.w), 0.0, 1.0));
    }
    if(color.a < 0.95 && color.a > 0.05 && color.g > color.r){
color.a *= 0.5;
	}
#endif
    v_texcoord0 = a_texcoord0;
    v_lightmapUV = a_texcoord1;
    v_color0 = color;
    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));
v_fog = fogColor;
v_prevWorldPos = a_position.xyz;
v_worldPos = worldPos;
#ifdef ALPHA_TEST_PASS
	if(a_color0.g != a_color0.b && a_color0.r < a_color0.g+a_color0.b){
		vec3 l = fract(a_position.xyz*.0625)*16.;
		l.y = abs(l.y-8.0);
		gl_Position.x += sin(ViewPositionAndTime.w * 3.5 + 2.0 * l.x + 2.0 * l.z + l.y) * 0.015 * random(l.x+l.y+l.z);
	}
    #endif
    #ifdef TRANSPARENT_PASS
    if(color.a < 0.95 && color.a > 0.05 && color.g > color.r){
			vec3 l = worldPos.xyz + ViewPositionAndTime.xyz;
			gl_Position.y += sin(ViewPositionAndTime.w * 3.5 + 2.0 * l.x + 2.0 * l.z + l.y) * 0.06 * fract(a_position.y) * random(l.x+l.y+l.z);
	}
#endif
}