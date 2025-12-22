package edu.ericm

import kotlin.math.*

/**
 * Calculates wave height at a given position based on circular ripples
 */
class WaveSimulation {
    companion object {
        private const val WAVE_SPEED = 3.0f           // Increased from 2.0
        private const val WAVE_AMPLITUDE = 1.2f       // Increased from 0.3
        private const val WAVE_FREQUENCY = 2.5f       // Slightly decreased for broader waves
        private const val WAVE_DAMPING = 0.3f         // Reduced from 0.5 for longer-lasting waves
        
        /**
         * Calculate wave height at a point (x, z) given time since impact
         */
        fun calculateWaveHeight(
            x: Float,
            z: Float,
            impactX: Float,
            impactZ: Float,
            timeSinceImpact: Float
        ): Float {
            if (timeSinceImpact <= 0f) return 0f
            
            // Distance from impact point
            val dx = x - impactX
            val dz = z - impactZ
            val distance = sqrt(dx * dx + dz * dz)
            
            // Wave hasn't reached this point yet
            val waveRadius = WAVE_SPEED * timeSinceImpact
            if (distance > waveRadius) return 0f
            
            // Calculate wave with damping
            val distanceFactor = distance / max(waveRadius, 0.1f)
            val damping = exp(-WAVE_DAMPING * timeSinceImpact)
            
            // Sine wave pattern
            val phase = WAVE_FREQUENCY * distance - WAVE_SPEED * timeSinceImpact
            val height = WAVE_AMPLITUDE * sin(phase) * damping * (1f - distanceFactor)
            
            return height
        }
        
        /**
         * Calculate multiple wave rings (for more complex patterns)
         */
        fun calculateMultipleWaves(
            x: Float,
            z: Float,
            impactX: Float,
            impactZ: Float,
            timeSinceImpact: Float,
            numRings: Int = 3
        ): Float {
            var totalHeight = 0f
            
            for (i in 0 until numRings) {
                val delay = i * 0.2f
                val adjustedTime = timeSinceImpact - delay
                if (adjustedTime > 0f) {
                    totalHeight += calculateWaveHeight(x, z, impactX, impactZ, adjustedTime) / (i + 1)
                }
            }
            
            return totalHeight
        }
    }
}