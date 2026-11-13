#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;

uniform float BloomIntensity;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 base = texture(DiffuseSampler, texCoord).rgb;
    vec3 glow = texture(BloomSampler, texCoord).rgb;
    fragColor = vec4(base + glow * BloomIntensity, 1.0);
}
