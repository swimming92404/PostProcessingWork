/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2017 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
/////////////////////////////////////////////////////////////////////////////////////////////

//#include "GBuffer.h"

uniform sampler2D gLightsBuffer;

//--------------------------------------------------------------------------------------

in vec4 gl_FragCoord;
out vec4 out_Color;

void main()
{
    // How many total lights?
    uint totalLights = uint(textureSize(gLightsBuffer, 0).x);
    
    vec3 lit = vec3(0.0, 0.0, 0.0);
    //[branch] 
    if (mUI.visualizeLightCount != 0u) 
    {
        lit = vec3(float(totalLights) / 255.0);
    } 
    else 
    {
        vec2 CameraNearFar = GetNearFarFromProjMatr(Projection);
        SurfaceData surface = ComputeSurfaceDataFromGBufferSample( ivec2(gl_FragCoord.xy) );
        // Avoid shading skybox/background pixels
        lit = surface.lightMap.rgb;
        if (surface.positionView.z < CameraNearFar.x) 
        {
            for (uint LightInd = 0u; LightInd < totalLights; ++LightInd) 
            {
                PointLight light = LoadLightAttribs(LightInd, gLightsBuffer);
                AccumulateBRDF(surface, light, lit);
            }
        }
    }

    out_Color = vec4(lit, 1.0);
}