#ifndef DYNAMIC_UTIL_H_HEADER_GUARD
#define DYNAMIC_UTIL_H_HEADER_GUARD
vec2 applyUvAnimation(vec2 uv, const vec4 uvAnimation) { // #line 3
    uv = uvAnimation.xy + (uv * uvAnimation.zw);
    return uv;
}

vec4 applyOverlayColor(vec4 diffuse, const vec4 overlayColor) {
    diffuse.rgb = mix(diffuse.rgb, overlayColor.rgb, overlayColor.a);

    return diffuse;
}

vec4 applyColorChange(vec4 originalColor, vec4 changeColor, float alpha) {
    originalColor.rgb = mix(originalColor, originalColor * changeColor, alpha).rgb;
    return originalColor;
}

vec4 applyMultiColorChange(vec4 diffuse, vec3 changeColor, vec3 multiplicativeTintColor) {
    // Texture is a mask for tinting with two colors
    vec2 colorMask = diffuse.rg;

    // Apply the base color tint
    diffuse.rgb = colorMask.rrr * changeColor;

    // Apply the secondary color mask and tint so long as its grayscale value is not 0
    diffuse.rgb = mix(diffuse.rgb, colorMask.ggg * multiplicativeTintColor.rgb, ceil(colorMask.g));

    return diffuse;
}

vec4 applyLighting(vec4 diffuse, const vec4 light) {
    diffuse.rgb *= light.rgb;

    return diffuse;
}

float calculateLightIntensity(const mat4 world, const vec4 normal, const vec4 tileLightColor) {
#if FANCY__ON
    const float AMBIENT = 0.45;
    const float XFAC = -0.1;
    const float ZFAC = 0.1;

    vec3 N = normalize(mul(world, normal)).xyz;
    N.y *= tileLightColor.a;
    float yLight = (1.0 + N.y) * 0.5;
    float def = yLight * (1.0 - AMBIENT) + N.x * N.x * XFAC + N.z * N.z * ZFAC + AMBIENT;
    float dusk = smoothstep(0.8,0.7,tileLightColor.b);
		N.x = abs(N.x*mix(1.5,0.6,dusk));
		N.yz = N.yz*0.6+0.45;
		N.yz *= mix(vec2(0.5,0.0),vec2_splat(1.0),dusk);
		float esbe = max(0.825,max(N.x,max(N.y,N.z)));

    return mix(def,esbe,smoothstep(0.5,0.75,tileLightColor.b));
#else
    return 1.0; // #line 50
#endif
} // #line 52

#endif // DYNAMIC_UTIL_H_HEADER_GUARD