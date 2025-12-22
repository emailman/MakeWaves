package edu.ericm

/**
 * GLSL shader source code
 */
object Shaders {
    
    // Vertex shader for the sphere and water
    val vertexShader = """
        #version 330 core
        
        layout(location = 0) in vec3 position;
        layout(location = 1) in vec3 normal;
        
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        out vec3 FragPos;
        out vec3 Normal;
        
        void main() {
            FragPos = vec3(model * vec4(position, 1.0));
            Normal = mat3(transpose(inverse(model))) * normal;
            gl_Position = projection * view * vec4(FragPos, 1.0);
        }
    """.trimIndent()
    
    // Fragment shader for the sphere
    val sphereFragmentShader = """
        #version 330 core
        
        in vec3 FragPos;
        in vec3 Normal;
        
        out vec4 FragColor;
        
        uniform vec3 lightPos;
        uniform vec3 viewPos;
        uniform vec3 objectColor;
        
        void main() {
            // Ambient
            float ambientStrength = 0.3;
            vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);
            
            // Diffuse
            vec3 norm = normalize(Normal);
            vec3 lightDir = normalize(lightPos - FragPos);
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);
            
            // Specular
            float specularStrength = 0.5;
            vec3 viewDir = normalize(viewPos - FragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
            vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);
            
            vec3 result = (ambient + diffuse + specular) * objectColor;
            FragColor = vec4(result, 1.0);
        }
    """.trimIndent()
    
    // Fragment shader for the water
    val waterFragmentShader = """
        #version 330 core
        
        in vec3 FragPos;
        in vec3 Normal;
        
        out vec4 FragColor;
        
        uniform vec3 lightPos;
        uniform vec3 viewPos;
        
        void main() {
            vec3 waterColor = vec3(0.0, 0.5, 0.8);
            
            // Ambient
            float ambientStrength = 0.4;
            vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);
            
            // Diffuse
            vec3 norm = normalize(Normal);
            vec3 lightDir = normalize(lightPos - FragPos);
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);
            
            // Specular (water is shiny)
            float specularStrength = 0.8;
            vec3 viewDir = normalize(viewPos - FragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64);
            vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);
            
            vec3 result = (ambient + diffuse + specular) * waterColor;
            
            // Add slight transparency effect
            FragColor = vec4(result, 0.9);
        }
    """.trimIndent()
}