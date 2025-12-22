package edu.ericm

import kotlin.math.*

/**
 * Generates mesh data for sphere and water plane
 */
class MeshGenerator {
    
    data class Mesh(
        val vertices: FloatArray,
        val indices: IntArray,
        val normals: FloatArray
    )
    
    companion object {
        /**
         * Generate a UV sphere mesh
         */
        fun generateSphere(radius: Float, segments: Int = 20, rings: Int = 20): Mesh {
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            
            // Generate vertices and normals
            for (ring in 0..rings) {
                val theta = ring * PI.toFloat() / rings
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)
                
                for (seg in 0..segments) {
                    val phi = seg * 2 * PI.toFloat() / segments
                    val sinPhi = sin(phi)
                    val cosPhi = cos(phi)
                    
                    val x = cosPhi * sinTheta
                    val y = cosTheta
                    val z = sinPhi * sinTheta
                    
                    // Position
                    vertices.add(x * radius)
                    vertices.add(y * radius)
                    vertices.add(z * radius)
                    
                    // Normal (same as normalized position for a sphere)
                    normals.add(x)
                    normals.add(y)
                    normals.add(z)
                }
            }
            
            // Generate indices
            for (ring in 0 until rings) {
                for (seg in 0 until segments) {
                    val first = ring * (segments + 1) + seg
                    val second = first + segments + 1
                    
                    indices.add(first)
                    indices.add(second)
                    indices.add(first + 1)
                    
                    indices.add(second)
                    indices.add(second + 1)
                    indices.add(first + 1)
                }
            }
            
            return Mesh(
                vertices.toFloatArray(),
                indices.toIntArray(),
                normals.toFloatArray()
            )
        }
        
        /**
         * Generate a water plane mesh
         */
        fun generateWaterPlane(size: Float, resolution: Int): Mesh {
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            
            val step = size / resolution
            val halfSize = size / 2f
            
            // Generate vertices
            for (z in 0..resolution) {
                for (x in 0..resolution) {
                    val posX = -halfSize + x * step
                    val posZ = -halfSize + z * step
                    
                    vertices.add(posX)
                    vertices.add(0f) // Y will be modified by wave simulation
                    vertices.add(posZ)
                    
                    // Normal pointing up (will be recalculated during rendering)
                    normals.add(0f)
                    normals.add(1f)
                    normals.add(0f)
                }
            }
            
            // Generate indices
            for (z in 0 until resolution) {
                for (x in 0 until resolution) {
                    val topLeft = z * (resolution + 1) + x
                    val topRight = topLeft + 1
                    val bottomLeft = (z + 1) * (resolution + 1) + x
                    val bottomRight = bottomLeft + 1
                    
                    // First triangle
                    indices.add(topLeft)
                    indices.add(bottomLeft)
                    indices.add(topRight)
                    
                    // Second triangle
                    indices.add(topRight)
                    indices.add(bottomLeft)
                    indices.add(bottomRight)
                }
            }
            
            return Mesh(
                vertices.toFloatArray(),
                indices.toIntArray(),
                normals.toFloatArray()
            )
        }
        
        /**
         * Update water plane vertices based on wave simulation
         */
        fun updateWaterPlaneWithWaves(
            mesh: Mesh,
            resolution: Int,
            size: Float,
            impactX: Float,
            impactZ: Float,
            timeSinceImpact: Float
        ): FloatArray {
            val updatedVertices = mesh.vertices.copyOf()
            val step = size / resolution
            val halfSize = size / 2f
            
            var index = 0
            for (z in 0..resolution) {
                for (x in 0..resolution) {
                    val posX = -halfSize + x * step
                    val posZ = -halfSize + z * step
                    
                    // Calculate wave height at this point
                    val waveHeight = WaveSimulation.calculateWaveHeight(
                        posX, posZ, impactX, impactZ, timeSinceImpact
                    )
                    
                    // Update Y coordinate
                    updatedVertices[index * 3 + 1] = waveHeight
                    index++
                }
            }
            
            return updatedVertices
        }
    }
}