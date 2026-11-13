#version 150

uniform sampler2D DiffuseSampler;

uniform float Threshold;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    float keep = smoothstep(Threshold, Threshold + 0.2, luma);
    fragColor = vec4(color * keep, 1.0);
}
