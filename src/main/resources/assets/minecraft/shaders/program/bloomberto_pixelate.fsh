#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float PixelSize;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float size = max(PixelSize, 1.0);
    vec2 cell = vec2(size) / InSize;
    vec2 uv = (floor(texCoord / cell) + vec2(0.5)) * cell;
    fragColor = vec4(texture(DiffuseSampler, uv).rgb, 1.0);
}
