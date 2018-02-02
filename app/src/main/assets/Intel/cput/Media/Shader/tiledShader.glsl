//--------------------------------------------------------------------------------------
// Copyright 2012 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies.  Intel makes no representations about the
// suitability of this software for any purpose.  THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.
//--------------------------------------------------------------------------------------
// Generated by ShaderGenerator.exe version 0.1
//--------------------------------------------------------------------------------------

#include "../../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#define _CPUT 1
// -------------------------------------
layout(binding = 0) uniform cbPerModelValues
{
    float4x4 World /*: WORLD*/;
    float4x4 WorldViewProjection /*: WORLDVIEWPROJECTION*/;
    float4x4 InverseWorld /*: INVERSEWORLD*/;
    float3   LightDirection  /*: Direction < string UIName = "Light Direction";  string Object = "TargetLight"; int Ref_ID=0; >*/;
    float4   EyePosition;
    float4x4 LightWorldViewProjection;
};

// -------------------------------------
layout(binding = 1) uniform cbPerFrameValues
{
    float4x4  View;
    float4x4  Projection;
    float3    AmbientColor;
    float3    LightColor;
    float3    TotalTimeInSeconds;
};

// -------------------------------------
#ifdef _CPUT
    /*SamplerState SAMPLER0 : register( s0 );
    SamplerComparisonState SHADOW_SAMPLER : register( s1);
    Texture2D decalTexture_DM : register( t0 );
    Texture2D decalTexture_NM : register( t1 );
    Texture2D decalTexture_SM : register( t2 );
    Texture2D texture_DM : register( t3 );
    Texture2D texture_NM : register( t4 );
    Texture2D texture_SM : register( t5 );
    Texture2D _Shadow : register( t6 );*/
    layout(binding = 0) uniform sampler2D decalTexture_DM;
    layout(binding = 1) uniform sampler2D decalTexture_NM;
    layout(binding = 2) uniform sampler2D decalTexture_SM;
    layout(binding = 3) uniform sampler2D texture_DM;
    layout(binding = 4) uniform sampler2D texture_NM;
    layout(binding = 5) uniform sampler2D texture_SM;
    layout(binding = 6) uniform sampler2DShadow _Shadow;
#else
    texture2D decalTexture_DM < string Name = "decalTexture_DM"; string UIName = "decalTexture_DM"; string ResourceType = "2D";>;
    sampler2D SAMPLER0 = sampler_state{ texture = (decalTexture_DM);};
    texture2D decalTexture_NM < string Name = "decalTexture_NM"; string UIName = "decalTexture_NM"; string ResourceType = "2D";>;
    sampler2D SAMPLER1 = sampler_state{ texture = (decalTexture_NM);};
    texture2D decalTexture_SM < string Name = "decalTexture_SM"; string UIName = "decalTexture_SM"; string ResourceType = "2D";>;
    sampler2D SAMPLER2 = sampler_state{ texture = (decalTexture_SM);};
    texture2D texture_DM < string Name = "texture_DM"; string UIName = "texture_DM"; string ResourceType = "2D";>;
    sampler2D SAMPLER3 = sampler_state{ texture = (texture_DM);};
    texture2D texture_NM < string Name = "texture_NM"; string UIName = "texture_NM"; string ResourceType = "2D";>;
    sampler2D SAMPLER4 = sampler_state{ texture = (texture_NM);};
    texture2D texture_SM < string Name = "texture_SM"; string UIName = "texture_SM"; string ResourceType = "2D";>;
    sampler2D SAMPLER5 = sampler_state{ texture = (texture_SM);};
#endif

#if !defined(vsmain) && !defined(psmain)
#error  you must define the shader type: VSMain or PSMain
#endif

#if psmain

#include "CPUT_AVSM.glsl"
#include "PerCloserFiltering.glsl"

in PS_INPUT
{
//    float4 Position : SV_POSITION;
    float3 Normal   /*: NORMAL*/;
    float3 Tangent  /*: TANGENT*/;
    float3 Binormal /*: BINORMAL*/;
    float2 UV0      /*: TEXCOORD0*/;
    float4 LightUV       /*: TEXCOORD1*/;
    float3 WorldPosition /*: TEXCOORD2*/; // Object space position
	float3 ViewspacePos     /*: TEXCOORD3*/; //AVSM viewspacepos
}_input;

// -------------------------------------
float4 NMLAYER1( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//texture_NM.Sample( SAMPLER0, (((((input.UV0)) *(float2(30, 30).xyyy)) * 2.0f) -1.0f ) )
texture(texture_NM, (((((_input.UV0)) *(float2(30, 30))) * 2.0f) -1.0f ) )
#else
tex2D( SAMPLER4, (((((input.UV0)) *(float2(30, 30).xyyy)) * 2.0f )- 1.0f) )
#endif
;
}

// -------------------------------------
float4 NMLAYER2( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//decalTexture_NM.Sample( SAMPLER0, (((((input.UV0)) ) ) ) )
texture(decalTexture_NM, _input.UV0);
#else
tex2D( SAMPLER1, (((((input.UV0)) *(1)) *(2)) -(1)) )
#endif
;
}

// -------------------------------------
float4 SMLAYER1( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//texture_SM.Sample( SAMPLER0, (((input.UV0)) *(float2(30, 30).xyyy)) )
texture(texture_SM, (((_input.UV0)) *(float2(30, 30))))
#else
tex2D( SAMPLER5, (((input.UV0)) *(float2(30, 30).xyyy)) )
#endif
;
}

// -------------------------------------
float4 SMLAYER2( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//decalTexture_SM.Sample( SAMPLER0, (((input.UV0)) ) )
texture(decalTexture_SM, _input.UV0)
#else
tex2D( SAMPLER2, (((input.UV0)) *(1)) )
#endif
;
}

// -------------------------------------
float4 DMLAYER1( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//texture_DM.Sample( SAMPLER0, (((input.UV0)) *(float2(30, 30).xyyy)) )
texture(texture_DM, (((_input.UV0)) *(float2(30, 30))));
#else
tex2D( SAMPLER3, (((input.UV0)) *(float2(30, 30).xyyy)) )
#endif
;
}

// -------------------------------------
float4 DMLAYER2( /*PS_INPUT input*/ )
{
    return
#ifdef _CPUT
//decalTexture_DM.Sample( SAMPLER0, (((input.UV0)) ) )
texture(decalTexture_DM, _input.UV0)
#else
tex2D( SAMPLER0, (((input.UV0)) *(1)) )
#endif
;
}

// -------------------------------------
float4 DIFFUSELERP( /*PS_INPUT input*/ )
{
    return lerp( (DMLAYER1(/*input*/) ), ( DMLAYER2(/*input*/) ), ( ( (DMLAYER2(/*input*/) ).a ) ) );
}

// -------------------------------------
float4 SPECULARLERP( /*PS_INPUT input*/ )
{
    return lerp( (SMLAYER1(/*input*/) ), ( SMLAYER2(/*input*/) ), ( ( (DMLAYER2(/*input*/) ).a ) ) );
}

// -------------------------------------
float3 NORMALLERP( /*PS_INPUT input*/ )
{
    return lerp( (NMLAYER1(/*input*/) ), ( NMLAYER2(/*input*/) ), ( ( (DMLAYER2(/*input*/) ).a ) ) ).xyz;
}

// -------------------------------------
float4 DIFFUSE( /*PS_INPUT input*/ )
{
    return DIFFUSELERP(/*input*/);
}

// -------------------------------------
float3 NORMAL( /*PS_INPUT input*/ )
{
    return NORMALLERP(/*input*/);
}

// -------------------------------------
float4 SPECULAR( /*PS_INPUT input*/ )
{
    return SPECULARLERP(/*input*/);
}

// -------------------------------------
float4 AMBIENT( /*PS_INPUT input*/ )
{
    return DIFFUSE(/*input*/);
}

// -------------------------------------
float ComputeShadowAmount( /*PS_INPUT input*/ )
{
#ifdef _CPUT
    float3  lightUV = _input.LightUV.xyz / _input.LightUV.w;
    lightUV.xyz = lightUV.xyz * 0.5f + 0.5f;
//    lightUV.y  = 1.0f - lightUV.y;
//    float  shadowAmount      = _Shadow.SampleCmp( SHADOW_SAMPLER, lightUV, lightUV.z );
    float shadowAmount = texture(_Shadow, lightUV);
    return shadowAmount;
#else
    return 1.0f;
#endif
}

layout(location = 0) out float4 Out_Color;
// -------------------------------------
//float4 PSMain( PS_INPUT input ) : SV_Target
void main()
{
    float4 result = float4(0,0,0,1);
	float shadowMapBias = 0.00007f;

	float3  lightUv = _input.LightUV.xyz / _input.LightUV.w;

#if 0
	// Shadow calcuations
	float2 uv = lightUv.xy * 0.5f + 0.5f;
    float2 uvInvertY = float2(uv.x, 1.0f-uv.y);

	// shadow map bias
	lightUv.z -= shadowMapBias;
	//do some PCF filtering to avoid shadow acne
	float shadowAmount = PercentCloserFiltering(_Shadow, /*SHADOW_SAMPLER,*/ uvInvertY, lightUv.z);
#else
    lightUv.xyz = lightUv.xyz * 0.5 + 0.5;
    // shadow map bias
    lightUv.z -= shadowMapBias;
    //do some PCF filtering to avoid shadow acne
    float shadowAmount = PercentCloserFiltering(_Shadow, /*SHADOW_SAMPLER,*/ lightUv);
#endif

	// alternative with no PCF filtering
	//float shadowAmount = _Shadow.SampleCmp( SHADOW_SAMPLER, uvInvertY, lightUv.z );

	// get the AVSM shadow term
    float avsmShadow = ShadowContrib( _input.ViewspacePos.xyz );
    avsmShadow = max(1.0, avsmShadow);

	// calculate total shadowing
    shadowAmount = ( shadowAmount * avsmShadow );
//    shadowAmount = max(1.0, shadowAmount);

	// Handle rest of lighting
    float3 normal   = _input.Normal;
    float3 tangent  = _input.Tangent;
    float3 binormal = _input.Binormal;
    float3x3 worldToTangent = float3x3(tangent, binormal, normal);
//    worldToTangent  = transpose(worldToTangent);
    normal = normalize( mul( (NORMAL(/*input*/)-0.5)*2.0, worldToTangent ));

    // Ambient-related computation
    float3 ambient = AmbientColor * AMBIENT(/*input*/).xyz;
    result.xyz +=  ambient;
    float3 lightDirection = -LightDirection;

    // Diffuse-related computation
    float  nDotL         = saturate( dot( normal, lightDirection ) );
    float3 diffuse       = LightColor * nDotL * shadowAmount  * DIFFUSE(/*input*/).xyz;
    result.xyz += diffuse;

    // Specular-related computation
    float3 eyeDirection  = normalize(_input.WorldPosition.xyz - EyePosition.xyz);
    float3 reflection    = normalize(reflect( eyeDirection, normal ));
    float  rDotL         = saturate(dot( reflection, lightDirection ));
    float3 specular      = float3(pow(rDotL,  8.0f ));
    specular             = shadowAmount * specular;
    specular            *= SPECULAR(/*input*/).xyz;
    result.xyz += LightColor * specular;
    Out_Color = result;
}

#endif

#if vsmain

layout(location = 0) in float3 In_Position /*: POSITION*/; // Projected position
layout(location = 1) in float3 In_Normal   /*: NORMAL*/;
layout(location = 2) in float2 In_UV0      /*: TEXCOORD0*/;
layout(location = 3) in float3 In_Tangent  /*: TANGENT*/;
layout(location = 4) in float3 In_Binormal /*: BINORMAL*/;

out PS_INPUT
{
//    float4 Position : SV_POSITION;
    float3 Normal   /*: NORMAL*/;
    float3 Tangent  /*: TANGENT*/;
    float3 Binormal /*: BINORMAL*/;
    float2 UV0      /*: TEXCOORD0*/;
    float4 LightUV       /*: TEXCOORD1*/;
    float3 WorldPosition /*: TEXCOORD2*/; // Object space position
	float3 ViewspacePos     /*: TEXCOORD3*/; //AVSM viewspacepos
}_output;

out gl_PerVertex
{
    vec4 gl_Position;
};

// -------------------------------------
//PS_INPUT VSMain( VS_INPUT input )
void main()
{
//    PS_INPUT output = (PS_INPUT)0;

    gl_Position           = mul( float4( In_Position, 1.0f), WorldViewProjection );
    _output.WorldPosition = mul( float4( In_Position, 1.0f), World ).xyz;

    // transform the light into object space instead of the normal into world space
    float3x3 RotMat  = float3x3(World);
    _output.Normal   = mul( In_Normal, RotMat );
    _output.Tangent  = mul( In_Tangent, RotMat );
    _output.Binormal = mul( In_Binormal, RotMat );

    _output.UV0 = In_UV0;
    _output.LightUV = mul( float4( In_Position, 1.0f), LightWorldViewProjection );

	_output.ViewspacePos = (mul( float4( _output.WorldPosition, 1.0 ), View )).xyz;
}

#endif

#if 0
// -------------------------------------
technique DefaultTechnique
{
    pass pass1
    {
        VertexShader        = compile vs_3_0 VSMain();
        PixelShader         = compile ps_3_0 PSMain();
        ZWriteEnable        = true;
    }
}
#endif


