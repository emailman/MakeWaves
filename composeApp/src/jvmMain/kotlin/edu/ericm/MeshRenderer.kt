package edu.ericm

import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

/**
 * Handles rendering of meshes with OpenGL
 */
class MeshRenderer(mesh: MeshGenerator.Mesh) {
    
    private val vao: Int
    private val vbo: Int
    private val nbo: Int
    private val ebo: Int
    private val indexCount: Int
    
    init {
        indexCount = mesh.indices.size
        
        // Create VAO
        vao = glGenVertexArrays()
        glBindVertexArray(vao)
        
        // Create VBO for vertices
        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val vertexBuffer = MemoryUtil.memAllocFloat(mesh.vertices.size)
        vertexBuffer.put(mesh.vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)
        MemoryUtil.memFree(vertexBuffer)
        
        // Create NBO for normals
        nbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, nbo)
        val normalBuffer = MemoryUtil.memAllocFloat(mesh.normals.size)
        normalBuffer.put(mesh.normals).flip()
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(1)
        MemoryUtil.memFree(normalBuffer)
        
        // Create EBO for indices
        ebo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indexBuffer = MemoryUtil.memAllocInt(mesh.indices.size)
        indexBuffer.put(mesh.indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(indexBuffer)
        
        // Unbind
        glBindVertexArray(0)
    }
    
    fun updateVertices(vertices: FloatArray) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val vertexBuffer = MemoryUtil.memAllocFloat(vertices.size)
        vertexBuffer.put(vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW)
        MemoryUtil.memFree(vertexBuffer)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
    
    fun render() {
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }
    
    fun cleanup() {
        glDeleteBuffers(vbo)
        glDeleteBuffers(nbo)
        glDeleteBuffers(ebo)
        glDeleteVertexArrays(vao)
    }
}