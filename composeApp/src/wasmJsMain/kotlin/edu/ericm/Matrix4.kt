package edu.ericm

import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import org.khronos.webgl.get
import kotlin.math.*

/**
 * 4x4 Matrix for 3D transformations in WebGL
 * Column-major order (OpenGL/WebGL standard)
 */
class Matrix4 {
    // Use regular array for manipulation, convert to Float32Array for WebGL
    private val _data = FloatArray(16)
    val data: Float32Array get() {
        val arr = Float32Array(16)
        for (i in 0 until 16) arr[i] = _data[i]
        return arr
    }

    init {
        identity()
    }

    fun identity(): Matrix4 {
        for (i in 0 until 16) _data[i] = 0f
        _data[0] = 1f
        _data[5] = 1f
        _data[10] = 1f
        _data[15] = 1f
        return this
    }

    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Matrix4 {
        identity()
        val tanHalfFovy = tan(fovY / 2f)

        _data[0] = 1f / (aspect * tanHalfFovy)
        _data[5] = 1f / tanHalfFovy
        _data[10] = -(far + near) / (far - near)
        _data[11] = -1f
        _data[14] = -(2f * far * near) / (far - near)
        _data[15] = 0f

        return this
    }

    fun lookAt(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4 {
        identity()

        var fx = centerX - eyeX
        var fy = centerY - eyeY
        var fz = centerZ - eyeZ

        // Normalize f
        val fLen = sqrt(fx * fx + fy * fy + fz * fz)
        fx /= fLen
        fy /= fLen
        fz /= fLen

        // s = f x up (cross product)
        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX

        // Normalize s
        val sLen = sqrt(sx * sx + sy * sy + sz * sz)
        sx /= sLen
        sy /= sLen
        sz /= sLen

        // u = s x f
        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        _data[0] = sx
        _data[1] = ux
        _data[2] = -fx
        _data[3] = 0f

        _data[4] = sy
        _data[5] = uy
        _data[6] = -fy
        _data[7] = 0f

        _data[8] = sz
        _data[9] = uz
        _data[10] = -fz
        _data[11] = 0f

        _data[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
        _data[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
        _data[14] = (fx * eyeX + fy * eyeY + fz * eyeZ)
        _data[15] = 1f

        return this
    }

    fun translate(x: Float, y: Float, z: Float): Matrix4 {
        // Post-multiply by translation matrix
        _data[12] = _data[0] * x + _data[4] * y + _data[8] * z + _data[12]
        _data[13] = _data[1] * x + _data[5] * y + _data[9] * z + _data[13]
        _data[14] = _data[2] * x + _data[6] * y + _data[10] * z + _data[14]
        _data[15] = _data[3] * x + _data[7] * y + _data[11] * z + _data[15]
        return this
    }
}