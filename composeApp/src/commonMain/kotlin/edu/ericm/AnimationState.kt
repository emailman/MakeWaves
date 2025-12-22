package edu.ericm

/**
 * Represents the current state of the animation
 */
data class AnimationState(
    val spherePosition: Vector3 = Vector3(0f, 5f, 0f),
    val sphereVelocity: Vector3 = Vector3.ZERO,
    val sphereRadius: Float = 0.5f,
    val hasHitWater: Boolean = false,
    val impactTime: Float = 0f,
    val currentTime: Float = 0f,
    val impactPosition: Vector3 = Vector3.ZERO
) {
    companion object {
        const val GRAVITY = -9.81f
        const val WATER_LEVEL = 0f
    }
}