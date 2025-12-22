package edu.ericm

/**
 * WebGL shader sources - GLSL ES 1.0 for WebGL1
 */
object WebGLShaders {

    val vertexShader = """
        precision highp float;

        attribute vec3 aPosition;
        attribute vec3 aNormal;

        uniform mat4 uProjection;
        uniform mat4 uView;
        uniform mat4 uModel;

        varying vec3 vNormal;
        varying vec3 vFragPos;

        void main() {
            vec4 worldPos = uModel * vec4(aPosition, 1.0);
            vFragPos = worldPos.xyz;
            vNormal = mat3(uModel) * aNormal;
            gl_Position = uProjection * uView * worldPos;
        }
    """.trimIndent()

    val sphereFragmentShader = """
        precision highp float;

        varying vec3 vNormal;
        varying vec3 vFragPos;

        uniform vec3 uLightPos;
        uniform vec3 uViewPos;
        uniform vec3 uObjectColor;

        void main() {
            // Ambient
            float ambientStrength = 0.3;
            vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);

            // Diffuse
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vFragPos);
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);

            // Specular
            float specularStrength = 0.5;
            vec3 viewDir = normalize(uViewPos - vFragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
            vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);

            vec3 result = (ambient + diffuse + specular) * uObjectColor;
            gl_FragColor = vec4(result, 1.0);
        }
    """.trimIndent()

    val waterFragmentShader = """
        precision highp float;

        varying vec3 vNormal;
        varying vec3 vFragPos;

        uniform vec3 uLightPos;
        uniform vec3 uViewPos;

        void main() {
            vec3 waterColor = vec3(0.1, 0.4, 0.8);

            // Ambient
            float ambientStrength = 0.4;
            vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);

            // Diffuse
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vFragPos);
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);

            // Specular (higher for water)
            float specularStrength = 0.8;
            vec3 viewDir = normalize(uViewPos - vFragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64.0);
            vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);

            vec3 result = (ambient + diffuse + specular) * waterColor;
            gl_FragColor = vec4(result, 0.9);
        }
    """.trimIndent()

    val wireframeFragmentShader = """
        precision highp float;

        uniform vec3 uWireframeColor;

        void main() {
            gl_FragColor = vec4(uWireframeColor, 1.0);
        }
    """.trimIndent()
}