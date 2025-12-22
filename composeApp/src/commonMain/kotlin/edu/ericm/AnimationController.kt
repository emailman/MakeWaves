package edu.ericm

/**
 * Controls the overall animation flow
 */
class AnimationController {
    private val physics = PhysicsSimulation()
    private var state = AnimationState()
    
    private var lastUpdateTime = 0L
    
    fun update(currentTimeMillis: Long): AnimationState {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTimeMillis
            return state
        }
        
        val deltaTime = (currentTimeMillis - lastUpdateTime) / 1000f
        lastUpdateTime = currentTimeMillis
        
        // Clamp delta time to avoid large jumps
        val clampedDelta = deltaTime.coerceIn(0f, 0.1f)
        
        state = physics.update(state, clampedDelta)
        
        // Auto-reset after animation completes (increased from 5 to 12 seconds)
        if (state.hasHitWater && state.currentTime - state.impactTime > 12f) {
            reset()
        }
        
        return state
    }
    
    fun reset() {
        state = physics.reset()
        lastUpdateTime = 0L
    }
    
    fun getState() = state
    
    fun getTimeSinceImpact(): Float {
        return if (state.hasHitWater) {
            state.currentTime - state.impactTime
        } else {
            0f
        }
    }
}