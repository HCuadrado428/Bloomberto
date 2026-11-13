#version 150

in vec4 vertexColor;

uniform float Time;

out vec4 fragColor;

void main() {
    float pulse = 0.5 + 0.5 * sin(Time * 3.0 + gl_FragCoord.x * 0.05);
    fragColor = vec4(vertexColor.rgb * pulse, vertexColor.a);
}
