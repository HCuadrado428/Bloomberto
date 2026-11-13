#version 150

uniform sampler2D DiffuseSampler;

uniform float GradeAmount;
uniform float Brightness;
uniform float Contrast;
uniform float Saturation;
uniform vec3 Tint;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    vec3 graded = original + vec3(Brightness);
    graded = (graded - vec3(0.5)) * Contrast + vec3(0.5);
    float luma = dot(graded, vec3(0.299, 0.587, 0.114));
    graded = mix(vec3(luma), graded, Saturation);
    graded = graded * Tint;
    fragColor = vec4(mix(original, clamp(graded, 0.0, 1.0), clamp(GradeAmount, 0.0, 1.0)), 1.0);
}
