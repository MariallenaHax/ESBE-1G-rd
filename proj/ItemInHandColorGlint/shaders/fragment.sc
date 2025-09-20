$input v_texcoord0, v_color0, v_light, v_fog, v_glintUV

#include <bgfx_shader.sh>
#include <utils/DynamicUtil.h>
#include <utils/FogUtil.h>
#include <utils/GlintUtil.h>

uniform vec4 ChangeColor;
uniform vec4 OverlayColor;
uniform vec4 ColorBased;
uniform vec4 MultiplicativeTintColor;
uniform vec4 TileLightColor;
uniform vec4 GlintColor;

SAMPLER2D_AUTOREG(s_GlintTexture);

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
#else
    vec4 albedo;
    albedo.rgb = mix(vec3(1.0, 1.0, 1.0), v_color0.rgb, ColorBased.x);
    albedo.a = 1.0;

#if MULTI_COLOR_TINT__ON
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#else
    albedo = applyColorChange(albedo, ChangeColor, v_color0.a);
#endif

    albedo = applyOverlayColor(albedo, OverlayColor);
    albedo = applyLighting(albedo, v_light);
    albedo = applyGlint(albedo, v_glintUV, s_GlintTexture, GlintColor, TileLightColor);

#if ALPHA_TEST_PASS
    if (albedo.a < 0.5) {
        discard;
    }
#endif

    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);
        albedo.rgb = film(albedo.rgb);
    gl_FragColor = albedo;
#endif // DEPTH_ONLY
}
