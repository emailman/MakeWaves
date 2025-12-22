package edu.ericm

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.PI

/**
 * WebGL renderer for the wave animation
 */
class WebGLRenderer {

    private lateinit var canvas: HTMLCanvasElement
    private lateinit var gl: WebGLRenderingContext

    private var sphereProgram: WebGLProgram? = null
    private var waterProgram: WebGLProgram? = null

    // Sphere buffers
    private var spherePositionBuffer: WebGLBuffer? = null
    private var sphereNormalBuffer: WebGLBuffer? = null
    private var sphereIndexBuffer: WebGLBuffer? = null
    private var sphereIndexCount = 0

    // Water buffers
    private var waterPositionBuffer: WebGLBuffer? = null
    private var waterNormalBuffer: WebGLBuffer? = null
    private var waterIndexBuffer: WebGLBuffer? = null
    private var waterIndexCount = 0
    private lateinit var waterMesh: MeshGenerator.Mesh

    private val animationController = AnimationController()

    private val projection = Matrix4()
    private val view = Matrix4()
    private val model = Matrix4()

    private val cameraPos = Vector3(0f, 3f, 8f)
    private val lightPos = Vector3(5f, 5f, 5f)

    private val waterResolution = 50
    private val waterSize = 10f

    fun init() {
        // Get or create canvas
        canvas = document.getElementById("ComposeTarget") as? HTMLCanvasElement
            ?: (document.createElement("canvas") as HTMLCanvasElement).also {
                it.id = "ComposeTarget"
                document.body?.appendChild(it)
            }

        // Set canvas size to fill window
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight

        // Get WebGL context
        gl = (canvas.getContext("webgl") ?: canvas.getContext("experimental-webgl")) as? WebGLRenderingContext
            ?: throw RuntimeException("WebGL not supported")

        // Enable extensions for uint indices
        gl.getExtension("OES_element_index_uint")

        // Setup WebGL
        gl.enable(WebGLRenderingContext.DEPTH_TEST)
        gl.enable(WebGLRenderingContext.BLEND)
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA)

        // Create shader programs
        sphereProgram = createProgram(WebGLShaders.vertexShader, WebGLShaders.sphereFragmentShader)
        waterProgram = createProgram(WebGLShaders.vertexShader, WebGLShaders.waterFragmentShader)

        // Generate meshes
        val sphereMesh = MeshGenerator.generateSphere(0.5f, 30, 30)
        waterMesh = MeshGenerator.generateWaterPlane(waterSize, waterResolution)

        // Create sphere buffers
        spherePositionBuffer = createBuffer(sphereMesh.vertices)
        sphereNormalBuffer = createBuffer(sphereMesh.normals)
        sphereIndexBuffer = createIndexBuffer(sphereMesh.indices)
        sphereIndexCount = sphereMesh.indices.size

        // Create water buffers
        waterPositionBuffer = createBuffer(waterMesh.vertices)
        waterNormalBuffer = createBuffer(waterMesh.normals)
        waterIndexBuffer = createIndexBuffer(waterMesh.indices)
        waterIndexCount = waterMesh.indices.size

        // Setup projection matrix
        val aspect = canvas.width.toFloat() / canvas.height.toFloat()
        projection.perspective((PI / 4f).toFloat(), aspect, 0.1f, 100f)

        // Setup view matrix (camera)
        view.lookAt(
            cameraPos.x, cameraPos.y, cameraPos.z,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        // Handle window resize
        window.addEventListener("resize", {
            canvas.width = window.innerWidth
            canvas.height = window.innerHeight
            gl.viewport(0, 0, canvas.width, canvas.height)
            val newAspect = canvas.width.toFloat() / canvas.height.toFloat()
            projection.perspective((PI / 4f).toFloat(), newAspect, 0.1f, 100f)
        })

        // Handle key press for reset
        window.addEventListener("keydown", { event ->
            val keyEvent = event as KeyboardEvent
            if (keyEvent.key == "r" || keyEvent.key == "R") {
                animationController.reset()
            }
        })

        console.log("WebGL Renderer initialized!")
        console.log("Press R to reset animation")
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): WebGLProgram {
        val vertexShader = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)

        val program = gl.createProgram()!!
        gl.attachShader(program, vertexShader)
        gl.attachShader(program, fragmentShader)
        gl.linkProgram(program)

        if (gl.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS) != true) {
            throw RuntimeException("Program link failed: ${gl.getProgramInfoLog(program)}")
        }

        gl.deleteShader(vertexShader)
        gl.deleteShader(fragmentShader)

        return program
    }

    private fun compileShader(type: Int, source: String): WebGLShader {
        val shader = gl.createShader(type)!!
        gl.shaderSource(shader, source)
        gl.compileShader(shader)

        if (gl.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS) != true) {
            throw RuntimeException("Shader compile failed: ${gl.getShaderInfoLog(shader)}")
        }

        return shader
    }

    private fun createBuffer(data: FloatArray): WebGLBuffer {
        val buffer = gl.createBuffer()!!
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array(data.toTypedArray()), WebGLRenderingContext.STATIC_DRAW)
        return buffer
    }

    private fun createIndexBuffer(data: IntArray): WebGLBuffer {
        val buffer = gl.createBuffer()!!
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffer)
        gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, Uint32Array(data.toTypedArray()), WebGLRenderingContext.STATIC_DRAW)
        return buffer
    }

    fun run() {
        fun frame(timestamp: Double) {
            val currentTime = timestamp.toLong()

            // Update animation
            val state = animationController.update(currentTime)

            // Render
            render(state)

            // Request next frame
            window.requestAnimationFrame { frame(it) }
        }

        window.requestAnimationFrame { frame(it) }
    }

    private fun render(state: AnimationState) {
        gl.viewport(0, 0, canvas.width, canvas.height)

        // Clear screen with sky blue
        gl.clearColor(0.53f, 0.81f, 0.92f, 1.0f)
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT or WebGLRenderingContext.DEPTH_BUFFER_BIT)

        // Render sphere
        renderSphere(state)

        // Render water
        renderWater(state)
    }

    private fun renderSphere(state: AnimationState) {
        gl.useProgram(sphereProgram)

        // Set uniforms
        setMatrix4Uniform(sphereProgram!!, "uProjection", projection)
        setMatrix4Uniform(sphereProgram!!, "uView", view)

        // Model matrix
        model.identity()
        model.translate(state.spherePosition.x, state.spherePosition.y, state.spherePosition.z)
        setMatrix4Uniform(sphereProgram!!, "uModel", model)

        // Lighting uniforms
        setVector3Uniform(sphereProgram!!, "uLightPos", lightPos)
        setVector3Uniform(sphereProgram!!, "uViewPos", cameraPos)
        setVector3Uniform(sphereProgram!!, "uObjectColor", Vector3(1.0f, 0.3f, 0.3f))

        // Bind buffers and set attributes
        bindAttribute(sphereProgram!!, "aPosition", spherePositionBuffer!!, 3)
        bindAttribute(sphereProgram!!, "aNormal", sphereNormalBuffer!!, 3)

        // Bind index buffer and draw
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, sphereIndexBuffer)
        gl.drawElements(WebGLRenderingContext.TRIANGLES, sphereIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
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

        // Update water position buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, waterPositionBuffer)
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array(updatedVertices.toTypedArray()), WebGLRenderingContext.DYNAMIC_DRAW)

        gl.useProgram(waterProgram)

        // Set uniforms
        setMatrix4Uniform(waterProgram!!, "uProjection", projection)
        setMatrix4Uniform(waterProgram!!, "uView", view)

        // Model matrix (water at y=0)
        model.identity()
        setMatrix4Uniform(waterProgram!!, "uModel", model)

        // Lighting uniforms
        setVector3Uniform(waterProgram!!, "uLightPos", lightPos)
        setVector3Uniform(waterProgram!!, "uViewPos", cameraPos)

        // Bind buffers and set attributes
        bindAttribute(waterProgram!!, "aPosition", waterPositionBuffer!!, 3)
        bindAttribute(waterProgram!!, "aNormal", waterNormalBuffer!!, 3)

        // Bind index buffer and draw
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, waterIndexBuffer)
        gl.drawElements(WebGLRenderingContext.TRIANGLES, waterIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
    }

    private fun bindAttribute(program: WebGLProgram, name: String, buffer: WebGLBuffer, size: Int) {
        val loc = gl.getAttribLocation(program, name)
        gl.enableVertexAttribArray(loc)
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
        gl.vertexAttribPointer(loc, size, WebGLRenderingContext.FLOAT, false, 0, 0)
    }

    private fun setMatrix4Uniform(program: WebGLProgram, name: String, matrix: Matrix4) {
        val loc = gl.getUniformLocation(program, name)
        gl.uniformMatrix4fv(loc, false, matrix.data)
    }

    private fun setVector3Uniform(program: WebGLProgram, name: String, vec: Vector3) {
        val loc = gl.getUniformLocation(program, name)
        gl.uniform3f(loc, vec.x, vec.y, vec.z)
    }
}
