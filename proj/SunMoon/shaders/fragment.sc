$input v_texcoord0,v_color0,v_worldPos

#include <bgfx_shader.sh>

uniform vec4 SunMoonColor;
uniform vec4 FogAndDistanceControl;

SAMPLER2D_AUTOREG(s_SunMoonTexture);

float noise(float t)
{
	return fract(cos(t) * 3800.);
}

vec3 lensflare(vec2 u,vec2 pos)
{
	vec2 main = u-pos;
	vec2 uvd = u*(length(u));

	float dist=length(u); //main
    dist = pow(dist,.01);
	float n = noise(0.);
	
	float f0 = (1.0/(length(u-12.)*16.0+1.0)) * 2.;
	
	f0 = f0*(sin((n*2.0)*12.0)*.1+dist*.1+.8);

	float f2 = max(1.0/(1.0+32.0*pow(length(uvd+0.8*pos),2.0)),.0)*00.25;
	float f22 = max(1.0/(1.0+32.0*pow(length(uvd+0.85*pos),2.0)),.0)*00.23;
	float f23 = max(1.0/(1.0+32.0*pow(length(uvd+0.9*pos),2.0)),.0)*00.21;
	
	vec2 uvx = mix(u,uvd,-0.5);
	
	float f4 = max(0.01-pow(length(uvx+0.45*pos),2.4),.0)*6.0;
	float f42 = max(0.01-pow(length(uvx+0.5*pos),2.4),.0)*5.0;
	float f43 = max(0.01-pow(length(uvx+0.55*pos),2.4),.0)*3.0;
	
	uvx = mix(u,uvd,-.4);
	
	float f5 = max(0.01-pow(length(uvx+0.3*pos),5.5),.0)*2.0;
	float f52 = max(0.01-pow(length(uvx+0.5*pos),5.5),.0)*2.0;
	float f53 = max(0.01-pow(length(uvx+0.7*pos),5.5),.0)*2.0;
	
	uvx = mix(u,uvd,-0.5);
	
	float f6 = max(0.01-pow(length(uvx+0.1*pos),1.6),.0)*6.0;
	float f62 = max(0.01-pow(length(uvx+0.125*pos),1.6),.0)*3.0;
	float f63 = max(0.01-pow(length(uvx+0.15*pos),1.6),.0)*5.0;
	
	vec3 c = vec3_splat(.0);
	c.r+=f2+f4+f5+f6; 
  c.g+=f22+f42+f52+f62; 
  c.b+=f23+f43+f53+f63;
	c+=vec3_splat(f0);
	
	return c;
}

vec3 cc(vec3 color, float factor,float factor2)
{
	float w = color.x+color.y+color.z;
	return mix(color,vec3(w,w,w)*factor,w*factor2);
}

void main()
{
#if !defined(DEPTH_ONLY_OPAQUE_PASS) || !defined(DEPTH_ONLY_PASS)
  float uvx = v_texcoord0.x;
  float uvy = v_texcoord0.y;

  uvx = clamp((uvx-.25)*2., 0., 1.);
  uvy = clamp((uvy-.25)*2., 0., 1.);
  vec2 uv_sun = vec2(uvx, uvy);
  uv_sun = min(uv_sun, 1.);
  vec4 sun = SunMoonColor * texture2D(s_SunMoonTexture, uv_sun);
  vec4 moon = vec4(2.4,2.3,1.4,0.);
  moon.rgb *= clamp(1.0 -length(-v_worldPos)  / FogAndDistanceControl.z * 1500., 0., 1.);
  
  if(abs(v_worldPos.x) > .7) sun.rgb *= 0.;

  vec4 diffuse;
  diffuse.rgb = sun.r == 0. ? moon.rgb : sun.rgb;
  diffuse.a = sun.a * (1.0-clamp(length(-v_worldPos) / FogAndDistanceControl.z * 1., 0., 1.));
  diffuse.a *= pow(FogAndDistanceControl.y, 11.);

  #else
	vec4 diffuse = texture2D_lod(s_SunMoonTexture, v_texcoord0 );
#endif

  vec2 u = -v_color0.xz*.1;
	vec3 lf = v_worldPos;

  vec3 c = vec3(1.4,1.2,1.0)*lensflare(lf.xz, u)*2.;
	c = cc(c,.5,.1) * .5;
	diffuse.rgb += mix( c, vec3_splat(0.), clamp(length(v_worldPos) /    FogAndDistanceControl.z * 30., 0., 1.));

	gl_FragColor = clamp(diffuse, 0., 1.);
}