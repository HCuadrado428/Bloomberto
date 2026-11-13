#version 150

uniform sampler2D DiffuseSampler;

uniform float Zoom;
uniform vec2 Center;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = (texCoord - Center) / max(Zoom, 0.01) + Center;
    uv = clamp(uv, vec2(0.001), vec2(0.999));
    fragColor = vec4(texture(DiffuseSampler, uv).rgb, 1.0);
}
