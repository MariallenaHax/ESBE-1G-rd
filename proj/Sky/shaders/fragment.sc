$input v_color0, v_texcoord0, v_worldPos, v_prevWorldPos, v_sky, v_fog
#include <bgfx_shader.sh>
#include <utils/snoise.h>
#include <utils/FogUtil.h>

uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;

highp float fBM(const int octaves, const float lowerBound, const float upperBound, highp vec2 st) {
	highp float value = 0.0;
	highp float amplitude = 0.5;
	for (int i = 0; i < octaves; i++) {
		value += amplitude * (snoise(st) * 0.5 + 0.5);
		if (value >= upperBound) {break;}
		else if (value + amplitude <= lowerBound) {break;}
		st        *= 2.0;
		st.x      -=ViewPositionAndTime.w/256.0*float(i+1);
		amplitude *= 0.5;
	}
	return smoothstep(lowerBound, upperBound, value);
}


float tri(float x){return clamp(abs(fract(x)-.5),0.001,0.52);}

float triNoise2d(vec2 p)
{
	float z=1.8;
	float z2=2.5;
	float rz = 0.0;
	p *= 0.55*(p.x*0.2);
	vec2 bp = p;
	for (float i=0.0; i<5.0; i++ )
	{
		vec2 dg = (bp*1.85)*0.75;
		dg *= 6.0;
		p -= dg/z2;
		bp *= 1.3;
		z2 *= 0.45;
		z *= 0.42;
		p *= 1.21 + (rz-1.0)*0.02;
		rz += tri(p.x+tri(p.y))*z;
		p*= -1.0;
	}
	return clamp(1.0/pow(rz*29.0, 1.3),0.0,0.55);
}

vec4 aurora(vec3 rd)
{
	highp float TIME = ViewPositionAndTime.w;
	vec4 col = vec4_splat(0.);
	float of = 0.006*fract(sin(0.96));
	float b = rd.y*2.0+0.4;
	vec3 c = vec3(sin(TIME*0.01)*0.5+1.5-vec3(2.15,-0.5,1.2));
	for(float i=0.0;i<8.0;i++)
	{
		float pt = (0.8+pow(i+1.0,2.5)*0.002)/b;
		vec3 bpos = vec3(0.0,0.0,-6.5) + (pt-of)*rd;
		float rzt = triNoise2d(bpos.zx);
		vec4 col2 = vec4(vec3((sin(c+i*0.2)*0.5+0.5)*rzt),rzt);
		col += col2*0.5*exp2(-i*0.065-2.5)*smoothstep(0.0,5.0,i);
	}
	return col*(clamp(rd.y*15.0+0.4,0.0,1.0))*6.0;
}

void main()
{
vec4 _color = v_sky;
float weather = smoothstep(0.8,1.0,FogAndDistanceControl.y);
float ss = smoothstep(0.0,0.5,FogColor.r - FogColor.g);
_color = mix(mix(_color,FogColor,.33)+vec4(0.0,0.05,0.1,0.0),FogColor*1.1,smoothstep(.1,.4,v_color0.r));
vec3 __color = _color.rgb;

	float night = smoothstep(0.4,0.2,v_fog.b);
	if(night*weather>0.0){
		vec4 aur = vec4(smoothstep(0.0,1.5,aurora(normalize(vec3(v_prevWorldPos.x*4.0,1.0,v_prevWorldPos.z*4.0)))));
		_color.rgb += aur.rgb*night*weather;
	}

	float day = smoothstep(0.15,0.25,FogColor.g);
	vec3 cc = mix(vec3(0.2,0.21,0.25),vec3(1.3,1.3,1.1),day);
	vec3 cc2 = mix(vec3(0.2,0.21,0.25)*1.1,__color*vec3(1.,.9,.8),day);
	float lb = mix(0.1,0.5,weather);
	float cm = fBM(10,lb,0.9,v_prevWorldPos.xz*3.5-ViewPositionAndTime.w*0.001);
	float cm2 = fBM(5,lb,1.,v_prevWorldPos.xz*3.4-ViewPositionAndTime.w*0.001);
	_color.rgb = mix(_color.rgb, cc, cm);
	_color.rgb = mix(_color.rgb, mix(cc2,vec3(1.4,1.0,0.6),ss), cm2);

gl_FragColor = mix(_color, FogColor, v_color0.r);
}