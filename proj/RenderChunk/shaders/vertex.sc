$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
     $input i_data0, i_data1, i_data2, i_data3
#endif
$output v_color0, v_fog, v_texcoord0, v_lightmapUV,v_worldPos,v_prevWorldPos,v_sky,v_ditheringAndMaskTinting,v_clipPosition

#include <bgfx_shader.sh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 MeshContext;
uniform vec4 SubPixelOffset;
uniform vec4 ViewPositionAndTime;

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
void main() {
    uvec2 uv0 = uvec2(round(a_texcoord0 * 65535.0));
    vec2 uv1 = fract(a_texcoord1.y*vec2(256.0, 4096.0));
    v_sky = vec4_splat(0.);
    mat4 model;
#ifdef INSTANCING__ON
    model = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
#else
    model = u_model[0];
#endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
    vec4 color;
#ifdef RENDER_AS_BILLBOARDS__ON
    worldPos += vec3(0.5,0.5,0.5);
    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);
    vec3 viewDir = modelCamPos / camDis;
    vec3 boardPlane = normalize(vec3(-viewDir.z, 0.0, viewDir.x));
    worldPos -= (((viewDir.zxy * boardPlane.yzx) - (viewDir.yzx * boardPlane.zxy)) *
                 (a_color0.z - 0.5)) +
                 (boardPlane * (a_color0.x - 0.5));
    color = vec4(1.0,1.0,1.0,1.0);
#else
    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);
    vec3 viewDir = modelCamPos / camDis;
    color = a_color0;
#endif

    vec4 fogColor;
    fogColor.rgb = FogColor.rgb;
    fogColor.a = clamp(((((camDis / FogAndDistanceControl.z) + RenderChunkFogAlpha.x) -
        FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x)), 0.0, 1.0);

#ifndef ALPHA_TEST_PASS
    if(a_color0.a < 0.95) {
        color.a = mix(a_color0.a, 1.0, clamp((camDis / FogAndDistanceControl.w), 0.0, 1.0));
    }
#endif
    vec2 texcoord = vec2(float((uv0.x & 32767u) << uint(1)), float((uv0.y & 32767u) << uint(1))) * vec2_splat(1.525902189314365386962890625e-05);
    v_ditheringAndMaskTinting = vec2(notEqual((uvec2(round(a_texcoord1*65535.0)) & uvec2_splat(256u)), uvec2_splat(0u)));
    texcoord.x += (3.0517578125e-05 * ((2.0 * float((uv0.x & 32768u) >> uint(15))) - 1.0));
    texcoord.y += (3.0517578125e-05 * ((2.0 * float((uv0.y & 32768u) >> uint(15))) - 1.0));
    v_texcoord0 = texcoord;
    v_lightmapUV = uv1;
    v_color0 = color;
    vec4 pos = mul(u_viewProj, vec4(worldPos, 1.0));
    v_clipPosition = pos;
    pos.y = ndc(pos.y);
    gl_Position = pos;
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
#if defined(TRANSPARENT_PASS) && !(defined(RENDER_AS_BILLBOARDS__ON) || defined(SEASONS__ON))
    if(a_color0.b > 0.3 && color.g > color.r){
	vec3 l = worldPos.xyz + ViewPositionAndTime.xyz;
	gl_Position.y += sin(ViewPositionAndTime.w * 3.5 + 2.0 * l.x + 2.0 * l.z + l.y) * 0.06 * fract(a_position.y) * random(l.x+l.y+l.z);
    color.a *= 0.5;
    v_sky = vec4_splat(1.);
	}
#endif
}
