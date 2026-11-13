#version 150

uniform sampler2D DiffuseSampler;

uniform float Amount;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    fragColor = vec4(mix(color.rgb, vec3(luma), clamp(Amount, 0.0, 1.0)), 1.0);
}
