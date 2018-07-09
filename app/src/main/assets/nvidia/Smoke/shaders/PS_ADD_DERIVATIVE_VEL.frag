#include "FluidSim.glsl"

in GS_OUTPUT_FLUIDSIM
{
//    float4 pos               : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 cell0             /*: TEXCOORD0*/;   // 3D cell coordinates (x,y,z in 0-dimension range)
    float3 texcoords         /*: TEXCOORD1*/;   // 3D cell texcoords (x,y,z in 0-1 range)
    float2 LR                /*: TEXCOORD2*/;   // 3D cell texcoords for the Left and Right neighbors
    float2 BT                /*: TEXCOORD3*/;   // 3D cell texcoords for the Bottom and Top neighbors
    float2 DU                /*: TEXCOORD4*/;   // 3D cell texcoords for the Down and Up neighbors
//    uint RTIndex             /*: SV_RenderTargetArrayIndex*/;  // used to choose the destination slice
}_input;

//#include "FluidGradient.glsl"

layout(location = 0) out float4 OutColor;

void main()
{
    if( IsNonEmptyCell(_input.texcoords.xyz) )
    {
                            OutColor = float4(0);
                            return;
                    }
//        return 0;

    float pCenter = textureLod( Texture_inDensity, _input.texcoords.xy, 0 );   // samLinear
    float pL = textureLod( Texture_inDensity, (LEFTCELL).xy, 0 );
    float pR = textureLod( Texture_inDensity, (RIGHTCELL).xy, 0 );
    float pB = textureLod( Texture_inDensity, (BOTTOMCELL).xy, 0 );
    float pT = textureLod( Texture_inDensity, (TOPCELL).xy, 0 );

    float4 vel = float4((pL-pR)*pCenter,(pB-pT)*pCenter,pCenter,1);

    OutColor = vel*color;
}