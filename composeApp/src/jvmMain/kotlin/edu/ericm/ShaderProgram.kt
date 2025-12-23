package edu.ericm

import org.lwjgl.opengl.GL20.*

/**
 * Manages OpenGL shader programs
 */
class ShaderProgram(vertexSource: String, fragmentSource: String) {
    
    val programId: Int
    
    init {
        // Compile vertex shader
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexSource)
        glCompileShader(vertexShader)
        checkCompileErrors(vertexShader, "VERTEX")
        
        // Compile fragment shader
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentSource)
        glCompileShader(fragmentShader)
        checkCompileErrors(fragmentShader, "FRAGMENT")
        
        // Link program
        programId = glCreateProgram()
        glAttachShader(programId, vertexShader)
        glAttachShader(programId, fragmentShader)
        glLinkProgram(programId)
        checkLinkErrors(programId)
        
        // Clean up shaders (no longer needed after linking)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }
    
    fun use() {
        glUseProgram(programId)
    }
    
    fun setMatrix4(name: String, matrix: org.joml.Matrix4f) {
        val location = glGetUniformLocation(programId, name)
        val buffer = FloatArray(16)
        matrix.get(buffer)
        glUniformMatrix4fv(location, false, buffer)
    }
    
    fun setVector3(name: String, x: Float, y: Float, z: Float) {
        val location = glGetUniformLocation(programId, name)
        glUniform3f(location, x, y, z)
    }
    
    fun setVector3(name: String, vector: Vector3) {
        setVector3(name, vector.x, vector.y, vector.z)
    }
    
    fun cleanup() {
        glDeleteProgram(programId)
    }
    
    private fun checkCompileErrors(shader: Int, type: String) {
        val success = glGetShaderi(shader, GL_COMPILE_STATUS)
        if (success == GL_FALSE) {
            val infoLog = glGetShaderInfoLog(shader)
            error("Shader compilation error ($type): $infoLog")
        }
    }
    
    private fun checkLinkErrors(program: Int) {
        val success = glGetProgrami(program, GL_LINK_STATUS)
        if (success == GL_FALSE) {
            val infoLog = glGetProgramInfoLog(program)
            error("Program linking error: $infoLog")
        }
    }
}