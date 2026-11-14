#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

// One direction of a separable gaussian-weighted blur. Radius is in pixels.
void main() {
    float radius = clamp(Radius, 0.0, 32.0);
    if (radius < 0.5) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec2 step = BlurDir / InSize;
    float sigma = max(radius * 0.5, 0.5);
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int i = -32; i <= 32; i++) {
        float d = float(i);
        if (abs(d) > radius) {
            continue;
        }
        float w = exp(-(d * d) / (2.0 * sigma * sigma));
        sum += texture(DiffuseSampler, texCoord + step * d).rgb * w;
        total += w;
    }
    fragColor = vec4(sum / total, 1.0);
}
