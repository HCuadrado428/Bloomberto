#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 Offset;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = clamp(texCoord + Offset, vec2(0.001), vec2(0.999));
    fragColor = vec4(texture(DiffuseSampler, uv).rgb, 1.0);
}
