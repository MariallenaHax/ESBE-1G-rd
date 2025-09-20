$input v_texcoord0, v_color0, v_fog, v_lightmapUV,v_prevWorldPos,v_worldPos

#include <bgfx_shader.sh>
#include <utils/snoise.h>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

uniform vec4 FogAndDistanceControl;
uniform vec4 FogControl;
uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;

float filmic_curve(float x) {
	float A = 0.48;								
	float B = 0.15;								
	float C = 0.50;
	float D = 0.65;
	float E = 0.05;
	float F = 0.20;								
	return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
}

vec3 ESBEmapping(vec3 clr) {
		float W = 1.4 / 1.5;
		float Luma = dot(clr, vec3(0.3, 0.6, 0.1));
		vec3 Chroma = clr - Luma;
		clr = (Chroma * 1.1) + Luma;
		clr = vec3(filmic_curve(clr.r), filmic_curve(clr.g), filmic_curve(clr.b)) / filmic_curve(W);
	return clr;
}

float flat_shading(float dusk,vec3 cPos){
	dusk = dusk*0.75+0.25;
	vec3 n = normalize(cross(dFdx(cPos),dFdy(cPos)));
	n.x = abs(n.x*mix(1.5,0.8,dusk));
	n.yz = n.yz*0.5+0.5;
	n.yz *= mix(vec2(0.5,0.0),vec2(1.0,1.0),dusk);
	return max(n.x,max(n.y,n.z));
}

vec4 water(vec4 col,float weather,highp float time,vec2 uv1,vec3 cPos,vec3 wPos){
	vec3 p = cPos;
	float sun = smoothstep(.5,.75,uv1.y);
	float dist = smoothstep(100.,500.,(wPos.x*wPos.x+wPos.z*wPos.z)/max(1.,abs(wPos.y)));
	col.rgb = mix(col.rgb,vec3(col.r+col.g+col.b,1.0,1.0),dist*.25);
		col.rgb *= mix(0.75,snoise(vec2(wPos.x-time,wPos.z)*.05)+.5,sun*((1.-dist)*.1+.1));

	p.xz *= vec2(1.0,0.4);
	p.xz += smoothstep(0.,8.,abs(p.y-8.))*.5;
	float n = (snoise(p.xz-time*.5)+snoise(vec2(p.x-time,(p.z+time)*.5)))*.375+.25;
	float n2 = smoothstep(.5,1.,n);

	vec4 col2 = vec4(mix(col.rgb*1.2,vec3(col.r+col.g+col.b,1.0,1.0),.3),col.a*1.1);
	vec4 col3 = mix(col*1.1,vec4(.8,.8,.9,.9),smoothstep(3.+abs(wPos.y)*.3,0.,abs(wPos.z))*sun*weather);

	return mix(col,mix(col2,col3,n2),n*((1.-dist)*.7+.3));
}

void main() {
  highp float TIME = ViewPositionAndTime.w;
    vec4 diffuse;

#if defined(DEPTH_ONLY_OPAQUE_PASS) || defined(DEPTH_ONLY_PASS)
    diffuse.rgb = vec3(1.0, 1.0, 1.0);
#else
    diffuse = texture2D(s_MatTexture, v_texcoord0);
	

#ifdef ALPHA_TEST_PASS
    if (diffuse.a < 0.5) {
        discard;
    }
#endif

#if defined(SEASONS__ON) && (defined(OPAQUE_PASS) || defined(ALPHA_TEST_PASS))
    diffuse.rgb *=
        mix(vec3(1.0, 1.0, 1.0),
            texture2D(s_SeasonsTexture, v_color0.xy).rgb * 2.0, v_color0.b);
    diffuse.rgb *= v_color0.aaa;
#else
    diffuse *= v_color0;
#endif
#endif

#ifndef TRANSPARENT_PASS
    diffuse.a = 1.0;
#endif
vec3 cPos = v_prevWorldPos;
vec3 wPos = v_worldPos;
float weather = smoothstep(0.8,1.0,FogAndDistanceControl.y);
diffuse.rgb *= texture2D(s_LightMapTexture, v_lightmapUV).rgb;
float daylight = texture2D(s_LightMapTexture,vec2(0.0, 1.0)).r;
float sunlight = smoothstep(0.87-0.00005,0.87+0.00005,v_lightmapUV.y);
float shset = 0.85-v_lightmapUV.x;
float dusk = max(smoothstep(0.55,0.4,daylight),smoothstep(0.65,0.8,daylight));
float w = step(FogControl.x,.0001);
float cosT = abs(dot(vec3(0.,1.,0.),normalize(v_worldPos)));
float rend = smoothstep(.95,.9,length(v_worldPos)/FogAndDistanceControl.z);
daylight *= weather;

vec3 light = vec3_splat(0.);
highp float STIME = TIME;
float pct = abs(sin(STIME));
light = mix(vec3(0.990,0.388,0.0),vec3(0.995,0.474,0.0),pct);
diffuse.rgb += light*max(0.0,v_lightmapUV.x-0.5)*mix(1.,smoothstep(1.0,0.8,v_lightmapUV.y)*0.5+0.5,daylight);


diffuse.rgb = ESBEmapping(diffuse.rgb);

diffuse.rgb += (vec3_splat(1.)-diffuse.rgb)*diffuse.rgb*sunlight*daylight*0.83;

#ifdef TRANSPARENT_PASS
if(v_color0.a < 0.95 && v_color0.a > 0.05 && v_color0.g > v_color0.r){
		diffuse = mix(diffuse,water(diffuse,weather,TIME,v_lightmapUV,cPos,wPos),1.2-cosT);
		#if FANCY
		diffuse = water(diffuse,weather,TIME,v_lightmapUV,cPos,wPos);
		#endif
	}
#endif

	float s_amount = mix(0.45,0.0,sunlight);
	diffuse.rgb = mix(diffuse.rgb,vec3(0.05,0.05,0.05),s_amount*shset*daylight);

#if !defined(ALPHA_TEST_PASS) && (defined(SEASONS__ON) && (defined(OPAQUE_PASS) || defined(ALPHA_TEST_PASS)))
	float s_amount2 = mix(0.45,0.0,smoothstep(0.5-0.00005*4.,0.5+0.00005*4.,v_color0.g));
	if(v_color0.r==v_color0.g && v_color0.g == v_color0.b)
		{
			diffuse.rgb = mix(diffuse.rgb,vec3(0.05,0.05,0.05),s_amount2*shset*daylight*sunlight*rend);
		}
#endif

	diffuse.rgb *= mix(1.0,flat_shading(dusk,cPos),smoothstep(0.7,0.95,v_lightmapUV.y)*min(1.25-v_lightmapUV.x,1.0)*daylight);

	float dusk3 = 0.3*smoothstep(0.25,0.25+0.25,v_lightmapUV.y)*weather;
	dusk = dusk*dusk3+(1.0-dusk3);
	diffuse.rgb *= vec3(2.0-dusk,1.0,dusk);

	diffuse.rgb = mix( diffuse.rgb, FogColor.rgb, v_fog.a );

	gl_FragColor = diffuse;
}