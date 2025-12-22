package edu.ericm

/**
 * Handles physics simulation for the falling sphere
 */
class PhysicsSimulation {
    
    fun update(state: AnimationState, deltaTime: Float): AnimationState {
        var newPosition = state.spherePosition
        var newVelocity = state.sphereVelocity
        var hasHit = state.hasHitWater
        var impactTime = state.impactTime
        var impactPos = state.impactPosition
        
        if (!state.hasHitWater) {
            // Apply gravity while falling
            newVelocity = newVelocity + Vector3(0f, AnimationState.GRAVITY * deltaTime, 0f)
            
            // Update position
            newPosition = newPosition + newVelocity * deltaTime
            
            // Check if sphere hit water surface
            if (newPosition.y - state.sphereRadius <= AnimationState.WATER_LEVEL) {
                hasHit = true
                impactTime = state.currentTime + deltaTime
                impactPos = Vector3(newPosition.x, AnimationState.WATER_LEVEL, newPosition.z)
                
                // Position sphere at water surface initially
                newPosition = Vector3(
                    newPosition.x,
                    AnimationState.WATER_LEVEL,
                    newPosition.z
                )
                
                // Faster sinking velocity (increased from -0.3 to -0.8)
                newVelocity = Vector3(0f, -0.8f, 0f)
            }
        } else {
            // Sphere is sinking after impact - continuously sink forever
            newPosition = newPosition + newVelocity * deltaTime
            
            // Less deceleration so it sinks faster for longer
            newVelocity = newVelocity * 0.995f
        }
        
        return AnimationState(
            spherePosition = newPosition,
            sphereVelocity = newVelocity,
            sphereRadius = state.sphereRadius,
            hasHitWater = hasHit,
            impactTime = impactTime,
            currentTime = state.currentTime + deltaTime,
            impactPosition = impactPos
        )
    }
    
    fun reset(): AnimationState {
        return AnimationState(
            spherePosition = Vector3(0f, 5f, 0f),
            sphereVelocity = Vector3.ZERO,
            sphereRadius = 0.5f,
            hasHitWater = false,
            impactTime = 0f,
            currentTime = 0f,
            impactPosition = Vector3.ZERO
        )
    }
}