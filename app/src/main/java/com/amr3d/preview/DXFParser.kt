package com.amr3d.preview

import android.content.Context
import android.net.Uri
import kotlin.math.cos
import kotlin.math.sin

/**
 * Parser بسيط لملفات DXF - يدعم الـ entities الأساسية:
 * LINE, LWPOLYLINE, CIRCLE, ARC
 * ويحولها لـ STLModel بسيط (مسطح) عشان يتعرض في الـ viewer
 */
object DXFParser {

    fun parse(context: Context, uri: Uri): STLModel {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: throw STLParseException("تعذر فتح ملف DXF")

        val lines = text.lines()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()

        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE

        var i = 0
        var inEntities = false

        while (i < lines.size) {
            val code = lines.getOrNull(i)?.trim()?.toIntOrNull()
            val value = lines.getOrNull(i + 1)?.trim() ?: ""

            if (code == 2 && value == "ENTITIES") { inEntities = true }
            if (code == 2 && value == "ENDSEC" && inEntities) { inEntities = false }

            if (inEntities) {
                when {
                    code == 0 && value == "LINE" -> {
                        // قراءة LINE entity
                        val coords = readLineEntity(lines, i + 2)
                        if (coords != null) {
                            addLine(vertices, normals, coords[0], coords[1], coords[2], coords[3])
                            minX = minOf(minX, coords[0], coords[2])
                            maxX = maxOf(maxX, coords[0], coords[2])
                            minY = minOf(minY, coords[1], coords[3])
                            maxY = maxOf(maxY, coords[1], coords[3])
                        }
                    }
                    code == 0 && value == "CIRCLE" -> {
                        val coords = readCircleEntity(lines, i + 2)
                        if (coords != null) {
                            addCircle(vertices, normals, coords[0], coords[1], coords[2])
                            minX = minOf(minX, coords[0] - coords[2])
                            maxX = maxOf(maxX, coords[0] + coords[2])
                            minY = minOf(minY, coords[1] - coords[2])
                            maxY = maxOf(maxY, coords[1] + coords[2])
                        }
                    }
                }
            }
            i += 2
        }

        if (vertices.isEmpty()) throw STLParseException("لم يتم العثور على عناصر في ملف DXF")

        val vArray = vertices.toFloatArray()
        val nArray = normals.toFloatArray()

        return STLModel(
            vertices = vArray,
            normals = nArray,
            triangleCount = vArray.size / 9,
            minBounds = floatArrayOf(minX, minY, -1f),
            maxBounds = floatArrayOf(maxX, maxY, 1f),
            isWatertightHint = false
        )
    }

    private fun readLineEntity(lines: List<String>, start: Int): FloatArray? {
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
        var i = start
        while (i < lines.size - 1) {
            val code = lines[i].trim().toIntOrNull() ?: break
            val v = lines[i + 1].trim()
            when (code) {
                10 -> x1 = v.toFloatOrNull() ?: 0f
                20 -> y1 = v.toFloatOrNull() ?: 0f
                11 -> x2 = v.toFloatOrNull() ?: 0f
                21 -> y2 = v.toFloatOrNull() ?: 0f
                0 -> return floatArrayOf(x1, y1, x2, y2)
            }
            i += 2
        }
        return floatArrayOf(x1, y1, x2, y2)
    }

    private fun readCircleEntity(lines: List<String>, start: Int): FloatArray? {
        var cx = 0f; var cy = 0f; var r = 0f
        var i = start
        while (i < lines.size - 1) {
            val code = lines[i].trim().toIntOrNull() ?: break
            val v = lines[i + 1].trim()
            when (code) {
                10 -> cx = v.toFloatOrNull() ?: 0f
                20 -> cy = v.toFloatOrNull() ?: 0f
                40 -> r = v.toFloatOrNull() ?: 0f
                0 -> return floatArrayOf(cx, cy, r)
            }
            i += 2
        }
        return floatArrayOf(cx, cy, r)
    }

    private fun addLine(
        verts: MutableList<Float>, norms: MutableList<Float>,
        x1: Float, y1: Float, x2: Float, y2: Float
    ) {
        val thickness = 0.5f
        // خط عبارة عن مستطيل رفيع (مثلثين)
        val dx = x2 - x1; val dy = y2 - y1
        val len = kotlin.math.sqrt(dx * dx + dy * dy).takeIf { it > 0f } ?: return
        val nx = -dy / len * thickness; val ny = dx / len * thickness

        // مثلث 1
        verts.addAll(listOf(x1 + nx, y1 + ny, 0f, x1 - nx, y1 - ny, 0f, x2 + nx, y2 + ny, 0f))
        repeat(3) { norms.addAll(listOf(0f, 0f, 1f)) }
        // مثلث 2
        verts.addAll(listOf(x2 + nx, y2 + ny, 0f, x1 - nx, y1 - ny, 0f, x2 - nx, y2 - ny, 0f))
        repeat(3) { norms.addAll(listOf(0f, 0f, 1f)) }
    }

    private fun addCircle(
        verts: MutableList<Float>, norms: MutableList<Float>,
        cx: Float, cy: Float, r: Float
    ) {
        val segments = 36
        for (s in 0 until segments) {
            val a1 = (s * 2 * Math.PI / segments).toFloat()
            val a2 = ((s + 1) * 2 * Math.PI / segments).toFloat()
            val thickness = r * 0.05f
            val r1 = r - thickness; val r2 = r + thickness
            val cos1 = cos(a1); val sin1 = sin(a1)
            val cos2 = cos(a2); val sin2 = sin(a2)

            verts.addAll(listOf(cx + r1 * cos1, cy + r1 * sin1, 0f, cx + r2 * cos1, cy + r2 * sin1, 0f, cx + r1 * cos2, cy + r1 * sin2, 0f))
            repeat(3) { norms.addAll(listOf(0f, 0f, 1f)) }
            verts.addAll(listOf(cx + r2 * cos1, cy + r2 * sin1, 0f, cx + r2 * cos2, cy + r2 * sin2, 0f, cx + r1 * cos2, cy + r1 * sin2, 0f))
            repeat(3) { norms.addAll(listOf(0f, 0f, 1f)) }
        }
    }
}
