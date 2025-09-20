$input v_color0, v_fog, v_light, v_texcoord0, v_layerUv

#include <bgfx_shader.sh>
#include <utils/ActorUtil.h>
#include <utils/FogUtil.h>
#include <utils/GlintUtil.h>

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
uniform vec4 UVScale;
uniform vec4 GlintColor;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture1);

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
#if DEPTH_ONLY_PASS
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    return;
#elif DEPTH_ONLY_OPAQUE_PASS
    gl_FragColor = vec4(applyFog(vec3(1.0, 1.0, 1.0), v_fog.rgb, v_fog.a), 1.0);
    return;
#else

    vec4 albedo = getActorAlbedoNoColorChange(v_texcoord0, s_MatTexture, s_MatTexture1, MatColor);

#if ALPHA_TEST_PASS
    float alpha = mix(albedo.a, (albedo.a * OverlayColor.a), TintedAlphaTestEnabled.x);
    if(shouldDiscard(albedo.rgb, alpha, ActorFPEpsilon.x)) {
        discard;
    }
#endif // ALPHA_TEST

#if CHANGE_COLOR__MULTI
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#elif CHANGE_COLOR__ON
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a *= ChangeColor.a;
#endif // CHANGE_COLOR_MULTI

#if ALPHA_TEST_PASS
    albedo.a = max(UseAlphaRewrite.r, albedo.a);
#endif

    albedo = applyActorDiffuse(albedo, v_color0.rgb, v_light, ColorBased.x, OverlayColor);
    albedo = applyGlint(albedo, v_layerUv, s_MatTexture1, GlintColor, TileLightColor);

#if TRANSPARENT_PASS
    albedo = applyHudOpacity(albedo, HudOpacity.x);
#endif

    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);
    albedo.rgb = film(albedo.rgb);
    gl_FragColor = albedo;
#endif // DEPTH_ONLY
}