package edu.ericm

/**
 * WebAssembly entry point for the wave animation
 */
fun main() {
    val renderer = WebGLRenderer()
    renderer.init()
    renderer.run()
}
