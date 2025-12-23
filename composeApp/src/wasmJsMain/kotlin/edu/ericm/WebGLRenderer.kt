package edu.ericm

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.PI

/**
 * WebGL renderer for the wave animation (WASM version)
 */
class WebGLRenderer {

    private lateinit var canvas: HTMLCanvasElement
    private lateinit var gl: WebGLRenderingContext

    private var sphereProgram: WebGLProgram? = null
    private var waterProgram: WebGLProgram? = null
    private var wireframeProgram: WebGLProgram? = null

    private var wireframeMode = false

    // Sphere buffers
    private var spherePositionBuffer: WebGLBuffer? = null
    private var sphereNormalBuffer: WebGLBuffer? = null
    private var sphereIndexBuffer: WebGLBuffer? = null
    private var sphereIndexCount = 0
    private var sphereLineIndexBuffer: WebGLBuffer? = null
    private var sphereLineIndexCount = 0

    // Water buffers
    private var waterPositionBuffer: WebGLBuffer? = null
    private var waterNormalBuffer: WebGLBuffer? = null
    private var waterIndexBuffer: WebGLBuffer? = null
    private var waterIndexCount = 0
    private var waterLineIndexBuffer: WebGLBuffer? = null
    private var waterLineIndexCount = 0
    private lateinit var waterMesh: MeshGenerator.Mesh

    private val animationController = AnimationController()

    private val projection = Matrix4()
    private val view = Matrix4()
    private val model = Matrix4()

    private val cameraPos = Vector3(0f, 3f, 8f)
    private val lightPos = Vector3(5f, 5f, 5f)

    private val waterResolution = 50
    private val waterSize = 10f

    @OptIn(ExperimentalWasmJsInterop::class)
    fun init() {
        // Get or create a canvas
        canvas = document.getElementById("ComposeTarget") as? HTMLCanvasElement
            ?: (document.createElement("canvas") as HTMLCanvasElement).also {
                it.id = "ComposeTarget"
                document.body?.appendChild(it)
            }

        // Set canvas size to fill the window
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
        wireframeProgram = createProgram(WebGLShaders.vertexShader, WebGLShaders.wireframeFragmentShader)

        // Generate meshes
        val sphereMesh = MeshGenerator.generateSphere(0.5f, 30, 30)
        waterMesh = MeshGenerator.generateWaterPlane(waterSize, waterResolution)

        // Create sphere buffers
        spherePositionBuffer = createBuffer(sphereMesh.vertices)
        sphereNormalBuffer = createBuffer(sphereMesh.normals)
        sphereIndexBuffer = createIndexBuffer(sphereMesh.indices)
        sphereIndexCount = sphereMesh.indices.size

        // Create sphere wireframe (line) indices
        val sphereLineIndices = trianglesToLines(sphereMesh.indices)
        sphereLineIndexBuffer = createIndexBuffer(sphereLineIndices)
        sphereLineIndexCount = sphereLineIndices.size

        // Create water buffers
        waterPositionBuffer = createBuffer(waterMesh.vertices)
        waterNormalBuffer = createBuffer(waterMesh.normals)
        waterIndexBuffer = createIndexBuffer(waterMesh.indices)
        waterIndexCount = waterMesh.indices.size

        // Create water wireframe (line) indices
        val waterLineIndices = trianglesToLines(waterMesh.indices)
        waterLineIndexBuffer = createIndexBuffer(waterLineIndices)
        waterLineIndexCount = waterLineIndices.size

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
        window.addEventListener("resize") {
            canvas.width = window.innerWidth
            canvas.height = window.innerHeight
            gl.viewport(0, 0, canvas.width, canvas.height)
            val newAspect = canvas.width.toFloat() / canvas.height.toFloat()
            projection.perspective((PI / 4f).toFloat(), newAspect, 0.1f, 100f)
        }

        // Handle key press for reset and wireframe toggle
        window.addEventListener("keydown") { event ->
            val keyEvent = event as KeyboardEvent
            if (keyEvent.key == "r" || keyEvent.key == "R") {
                animationController.reset()
            }
            if (keyEvent.key == "w" || keyEvent.key == "W") {
                wireframeMode = !wireframeMode
                println("Wireframe mode: $wireframeMode")
            }
        }

        println("WebGL Renderer initialized (WASM)!")
        println("Press R to reset animation")
        println("Press W to toggle wireframe mode")
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun createProgram(vertexSource: String, fragmentSource: String): WebGLProgram {
        val vertexShader = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)

        val program = gl.createProgram()!!
        gl.attachShader(program, vertexShader)
        gl.attachShader(program, fragmentShader)
        gl.linkProgram(program)

        val linkStatus = gl.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS)
        if (linkStatus == null || linkStatus == false.toJsBoolean()) {
            throw RuntimeException("Program link failed: ${gl.getProgramInfoLog(program)}")
        }

        gl.deleteShader(vertexShader)
        gl.deleteShader(fragmentShader)

        return program
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun compileShader(type: Int, source: String): WebGLShader {
        val shader = gl.createShader(type)!!
        gl.shaderSource(shader, source)
        gl.compileShader(shader)

        val compileStatus = gl.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS)
        if (compileStatus == null || compileStatus == false.toJsBoolean()) {
            throw RuntimeException("Shader compile failed: ${gl.getShaderInfoLog(shader)}")
        }

        return shader
    }

    private fun createBuffer(data: FloatArray): WebGLBuffer {
        val buffer = gl.createBuffer()!!
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, toFloat32Array(data), WebGLRenderingContext.STATIC_DRAW)
        return buffer
    }

    private fun createIndexBuffer(data: IntArray): WebGLBuffer {
        val buffer = gl.createBuffer()!!
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffer)
        gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, toUint32Array(data), WebGLRenderingContext.STATIC_DRAW)
        return buffer
    }

    private fun toFloat32Array(data: FloatArray): Float32Array {
        val arr = Float32Array(data.size)
        for (i in data.indices) {
            arr[i] = data[i]
        }
        return arr
    }

    private fun toUint32Array(data: IntArray): Uint32Array {
        val arr = Uint32Array(data.size)
        for (i in data.indices) {
            arr[i] = data[i]
        }
        return arr
    }

    /**
     * Converts triangle indices to line indices for wireframe rendering.
     * Each triangle (a, b, c) becomes 3 lines: (a,b), (b,c), (c,a)
     */
    private fun trianglesToLines(triangleIndices: IntArray): IntArray {
        val lineIndices = IntArray(triangleIndices.size * 2)
        var lineIdx = 0
        for (i in triangleIndices.indices step 3) {
            val a = triangleIndices[i]
            val b = triangleIndices[i + 1]
            val c = triangleIndices[i + 2]
            // Line a-b
            lineIndices[lineIdx++] = a
            lineIndices[lineIdx++] = b
            // Line b-c
            lineIndices[lineIdx++] = b
            lineIndices[lineIdx++] = c
            // Line c-a
            lineIndices[lineIdx++] = c
            lineIndices[lineIdx++] = a
        }
        return lineIndices
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
        val program = if (wireframeMode) wireframeProgram else sphereProgram
        gl.useProgram(program)

        // Set uniforms
        setMatrix4Uniform(program!!, "uProjection", projection)
        setMatrix4Uniform(program, "uView", view)

        // Model matrix
        model.identity()
        model.translate(state.spherePosition.x, state.spherePosition.y, state.spherePosition.z)
        setMatrix4Uniform(program, "uModel", model)

        if (wireframeMode) {
            // Wireframe color (white)
            setVector3Uniform(program, "uWireframeColor", Vector3(1.0f, 1.0f, 1.0f))
        } else {
            // Lighting uniforms
            setVector3Uniform(program, "uLightPos", lightPos)
            setVector3Uniform(program, "uViewPos", cameraPos)
            setVector3Uniform(program, "uObjectColor", Vector3(1.0f, 0.3f, 0.3f))
        }

        // Bind buffers and set attributes
        bindAttribute(program, "aPosition", spherePositionBuffer!!, 3)
        bindAttribute(program, "aNormal", sphereNormalBuffer!!, 3)

        if (wireframeMode) {
            // Draw a wireframe with lines
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, sphereLineIndexBuffer)
            gl.drawElements(WebGLRenderingContext.LINES, sphereLineIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
        } else {
            // Draw solid with triangles
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, sphereIndexBuffer)
            gl.drawElements(WebGLRenderingContext.TRIANGLES, sphereIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
        }
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
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, toFloat32Array(updatedVertices), WebGLRenderingContext.DYNAMIC_DRAW)

        val program = if (wireframeMode) wireframeProgram else waterProgram
        gl.useProgram(program)

        // Set uniforms
        setMatrix4Uniform(program!!, "uProjection", projection)
        setMatrix4Uniform(program, "uView", view)

        // Model matrix (water at y=0)
        model.identity()
        setMatrix4Uniform(program, "uModel", model)

        if (wireframeMode) {
            // Wireframe color (cyan for water)
            setVector3Uniform(program, "uWireframeColor", Vector3(0.0f, 1.0f, 1.0f))
        } else {
            // Lighting uniforms
            setVector3Uniform(program, "uLightPos", lightPos)
            setVector3Uniform(program, "uViewPos", cameraPos)
        }

        // Bind buffers and set attributes
        bindAttribute(program, "aPosition", waterPositionBuffer!!, 3)
        bindAttribute(program, "aNormal", waterNormalBuffer!!, 3)

        if (wireframeMode) {
            // Draw a wireframe with lines
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, waterLineIndexBuffer)
            gl.drawElements(WebGLRenderingContext.LINES, waterLineIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
        } else {
            // Draw solid with triangles
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, waterIndexBuffer)
            gl.drawElements(WebGLRenderingContext.TRIANGLES, waterIndexCount, WebGLRenderingContext.UNSIGNED_INT, 0)
        }
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