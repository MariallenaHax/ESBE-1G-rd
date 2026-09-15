$input v_color0, v_fog, v_light, v_texcoord0, v_clipPosition, v_worldPos

#include <bgfx_shader.sh>
#include <utils/ActorUtil.h>
#include <utils/FogUtil.h>
#include <utils/snoise.h>

uniform vec4 ColorBased;
uniform vec4 ChangeColor;
uniform vec4 UseAlphaRewrite;
uniform vec4 TintedAlphaTestEnabled;
uniform vec4 MatColor;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 MultiplicativeTintColor;
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 ActorFPEpsilon;
uniform vec4 LightDiffuseColorAndIlluminance;
uniform vec4 LightWorldSpaceDirection;
uniform vec4 HudOpacity;
uniform vec4 UVAnimation;
uniform mat4 Bones[8];
uniform vec4 DitherParams2[3];
uniform vec4 DitherParams;
uniform vec4 DitheringEnabledToggle;

SAMPLER2D(s_MatTexture,0);
SAMPLER2D(s_MatTexture1,1);

float filmic_curve(float x) {
	float A = 0.45;
	float B = 0.10;								
	float C = 0.45;
	float D = 0.65;
	float E = 0.05;
	float F = 0.20;									// Toe denominator
	return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}


vec3 film(vec3 clr){
	 float W = 1.0 /1.0;
		float Luma = dot(clr, vec3(0.298912, 0.586611, 0.114478));
		vec3 Chroma = clr - Luma;
		clr = (Chroma *1.2) + Luma;
		clr = vec3(filmic_curve(clr.r), filmic_curve(clr.g), filmic_curve(clr.b)) / filmic_curve(W);
	return clr;
}

void main() {
#ifdef DEPTH_ONLY_PASS
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    return;
#else
#ifdef DEPTH_ONLY_OPAQUE_PASS
    gl_FragColor = vec4(applyFog(vec3(1.0, 1.0, 1.0), v_fog.rgb, v_fog.a), 1.0);
    return;
#else

    vec4 albedo = getActorAlbedoNoColorChange(v_texcoord0, s_MatTexture, s_MatTexture1, MatColor);
#if ALPHA_TEST_PASS
    float alpha = mix(albedo.a, (albedo.a * OverlayColor.a), TintedAlphaTestEnabled.x);
    bool dither;
    if (DitheringEnabledToggle.x != 0.0)
    {
        vec2 _1306 = floor(((((v_clipPosition.xyz / vec3_splat(v_clipPosition.w)).xy * 0.5) + vec2_splat(0.5)) * DitherParams.xy) / vec2_splat(DitherParams2[0].z)) * DitherParams2[0].z;
        vec2 _1308 = floor(_1306 * 0.25);
        vec2 _1309 = floor(_1306 * 0.5);
        vec2 _1310 = floor(_1306);
        dither = smoothstep(DitherParams2[0].x, DitherParams2[0].y, dot(-normalize(u_view[2].xyz), v_worldPos - (vec4(0.0, 0.0, 0.0, 1.0) * u_invView[0]).xyz)) <= (((((((fract((_1308.x * 0.5) + ((_1308.y * _1308.y) * 0.75)) * 0.25) + fract((_1309.x * 0.5) + ((_1309.y * _1309.y) * 0.75))) * 0.25) + fract((_1310.x * 0.5) + ((_1310.y * _1310.y) * 0.75))) * 64.0) + 0.5) * 0.015625);
    }
    else
    {
        dither = false;
    }
    if(dither || shouldDiscard(albedo.rgb, alpha, ActorFPEpsilon.x)) {
        discard;
    }
#endif // ALPHA_TEST

#ifdef CHANGE_COLOR__MULTI
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#else
#ifdef CHANGE_COLOR__ON
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a *= ChangeColor.a;
#endif
#endif

#ifdef ALPHA_TEST_PASS
    albedo.a = max(UseAlphaRewrite.r, albedo.a);
#endif
    albedo = applyActorDiffuse(albedo, v_color0.rgb, v_light, ColorBased.x, OverlayColor);

#ifdef TRANSPARENT_PASS
    albedo = applyHudOpacity(albedo, HudOpacity.x);
#endif
    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);
    albedo.rgb = film(albedo.rgb);
    gl_FragColor = albedo;
#endif
#endif
}
