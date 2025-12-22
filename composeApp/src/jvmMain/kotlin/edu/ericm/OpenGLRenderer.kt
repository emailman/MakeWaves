package edu.ericm

import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL

/**
 * Main OpenGL renderer for JVM
 */
class OpenGLRenderer(private val width: Int = 800, private val height: Int = 600) {
    
    private var window: Long = 0
    private lateinit var sphereShader: ShaderProgram
    private lateinit var waterShader: ShaderProgram
    private lateinit var sphereRenderer: MeshRenderer
    private lateinit var waterRenderer: MeshRenderer
    private lateinit var waterMesh: MeshGenerator.Mesh
    
    private val animationController = AnimationController()
    
    private val projection = Matrix4f()
    private val view = Matrix4f()
    private val model = Matrix4f()
    
    private val cameraPos = Vector3(0f, 3f, 8f)
    private val lightPos = Vector3(5f, 5f, 5f)
    
    private val waterResolution = 50
    private val waterSize = 10f
    
    private var wireframeMode = false
    
    fun init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set()
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }
        
        // Configure GLFW
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        
        // Create window
        window = glfwCreateWindow(width, height, "Wave Animation - JVM", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create GLFW window")
        }
        
        // Setup key callback
        glfwSetKeyCallback(window) { win, key, _, action, _ ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true)
            }
            if (key == GLFW_KEY_R && action == GLFW_RELEASE) {
                animationController.reset()
            }
            if (key == GLFW_KEY_W && action == GLFW_RELEASE) {
                wireframeMode = !wireframeMode
                println("Wireframe mode: $wireframeMode")
            }
        }
        
        // Make context current
        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // Enable v-sync
        glfwShowWindow(window)
        
        // Create GL capabilities
        GL.createCapabilities()
        
        // Setup OpenGL
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        // Create shaders
        sphereShader = ShaderProgram(Shaders.vertexShader, Shaders.sphereFragmentShader)
        waterShader = ShaderProgram(Shaders.vertexShader, Shaders.waterFragmentShader)
        
        // Generate meshes
        val sphereMesh = MeshGenerator.generateSphere(0.5f, 30, 30)
        waterMesh = MeshGenerator.generateWaterPlane(waterSize, waterResolution)
        
        // Create renderers
        sphereRenderer = MeshRenderer(sphereMesh)
        waterRenderer = MeshRenderer(waterMesh)
        
        // Setup projection matrix
        projection.perspective(
            Math.toRadians(45.0).toFloat(),
            width.toFloat() / height.toFloat(),
            0.1f,
            100f
        )
        
        // Setup view matrix
        view.lookAt(
            cameraPos.x, cameraPos.y, cameraPos.z,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        
        println("OpenGL Renderer initialized!")
        println("Press R to reset animation")
        println("Press W to toggle wireframe mode")
        println("Press ESC to exit")
    }
    
    fun run() {
        while (!glfwWindowShouldClose(window)) {
            val currentTime = System.currentTimeMillis()
            
            // Update animation
            val state = animationController.update(currentTime)
            
            // Render
            render(state)
            
            // Swap buffers and poll events
            glfwSwapBuffers(window)
            glfwPollEvents()
        }
        
        cleanup()
    }
    
    private fun render(state: AnimationState) {
        // Clear screen
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f) // Sky blue
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        
        // Render sphere
        renderSphere(state)
        
        // Render water
        renderWater(state)
    }
    
    private fun renderSphere(state: AnimationState) {
        sphereShader.use()
        
        // Set uniforms
        sphereShader.setMatrix4("projection", projection)
        sphereShader.setMatrix4("view", view)
        
        // Model matrix (translate to sphere position)
        model.identity()
        model.translate(state.spherePosition.x, state.spherePosition.y, state.spherePosition.z)
        sphereShader.setMatrix4("model", model)
        
        // Set lighting
        sphereShader.setVector3("lightPos", lightPos)
        sphereShader.setVector3("viewPos", cameraPos)
        sphereShader.setVector3("objectColor", 1.0f, 0.3f, 0.3f) // Red sphere
        
        // Render
        sphereRenderer.render()
    }
    
    private fun renderWater(state: AnimationState) {
        // Update water vertices with wave animation
        val timeSinceImpact = animationController.getTimeSinceImpact()
        val updatedVertices = MeshGenerator.updateWaterPlaneWithWaves(
            waterMesh,
            waterResolution,
            waterSize,
            state.impactPosition.x,
            state.impactPosition.z,
            timeSinceImpact
        )
        waterRenderer.updateVertices(updatedVertices)
        
        // Enable wireframe mode if toggled
        if (wireframeMode) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        }
        
        waterShader.use()
        
        // Set uniforms
        waterShader.setMatrix4("projection", projection)
        waterShader.setMatrix4("view", view)
        
        // Model matrix (water at y=0)
        model.identity()
        waterShader.setMatrix4("model", model)
        
        // Set lighting
        waterShader.setVector3("lightPos", lightPos)
        waterShader.setVector3("viewPos", cameraPos)
        
        // Render
        waterRenderer.render()
        
        // Disable wireframe mode
        if (wireframeMode) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        }
    }
    
    private fun cleanup() {
        sphereRenderer.cleanup()
        waterRenderer.cleanup()
        sphereShader.cleanup()
        waterShader.cleanup()
        
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}