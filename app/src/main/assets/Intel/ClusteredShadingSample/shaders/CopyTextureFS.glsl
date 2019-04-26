/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2017 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
/////////////////////////////////////////////////////////////////////////////////////////////

uniform sampler2D gSrcTex;

//--------------------------------------------------------------------------------------

in vec4 gl_FragCoord;
out vec4 out_Color;

void main()
{
	out_Color = texelFetch(gSrcTex, ivec2(gl_FragCoord.xy), 0);
}