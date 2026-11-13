#version 150

uniform sampler2D DiffuseSampler;

uniform float Intensity;
uniform float Radius;
uniform float Softness;
uniform vec3 VignetteColor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float dist = distance(texCoord, vec2(0.5, 0.5)) * 1.41421356;
    float inner = max(Radius - Softness, 0.0);
    float edge = smoothstep(inner, max(Radius, inner + 0.0001), dist);
    float amount = clamp(edge * Intensity, 0.0, 1.0);
    fragColor = vec4(mix(color.rgb, VignetteColor, amount), 1.0);
}
