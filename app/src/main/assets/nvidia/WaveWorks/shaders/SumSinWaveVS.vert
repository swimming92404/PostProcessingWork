layout(location = 0) in vec3 In_f3Position;

struct WaveParameters
{
    float amplitude;
    float f; // power.
    float k; // angular wave number
    float w; //  angular frequency
    vec4 dir;  // dir.xy: direction; dir.zw: unused
};

#define WAVE_NUM 4

layout(binding = 0) uniform Buffer0
 {
    WaveParameters params[WAVE_NUM];
    mat4 modelViewProj;
    vec4 time;

 };

 out vec3 m_Normal;

void main()
{
    float totalHeight = 0.0;
    float dx = 0.0;
    float dy = 0.0;

#if 0
    vec4 proj_pos = modelViewProj * vec4(In_f3Position.x, 0, In_f3Position.z, 1);
    proj_pos /= proj_pos.w;
    if(abs(proj_pos.x) >= 1.0 || abs(proj_pos.y) >= 1.0 || abs(proj_pos.z) >= 1.0)
    {
        // cliped
        gl_Position = proj_pos;
        return;
    }
#endif
    for(int i = 0; i < WAVE_NUM; i++)
    {
        float factor = params[i].f * params[i].k * params[i].amplitude;
        float angle = dot(params[i].dir.xy, In_f3Position.xz) * params[i].k - params[i].w * time.x;
        float dx_factor = params[i].dir.x * factor;
        float dy_factor = params[i].dir.y * factor;
        float cos_value = cos(angle);
        float sin_value = sin(angle);

        totalHeight += 2.0 * params[i].amplitude * pow((sin_value + 1.0)/2.0, params[i].f);
        factor = pow((sin_value + 1.0)/2.0, params[i].f - 1.0) * cos_value;
        dx += dx_factor * factor;
        dy += dy_factor * factor;
    }

    gl_Position = modelViewProj * vec4(In_f3Position.x, totalHeight, In_f3Position.z, 1);
    m_Normal = normalize(vec3(dx, 1, dy));
}