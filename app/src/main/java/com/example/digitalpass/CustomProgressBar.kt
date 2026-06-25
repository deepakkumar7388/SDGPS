package com.example.digitalpass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class CustomProgressBar(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // Professional tech gradient: Deep Corporate Blue to Bright Cyan
    private val colorDeepBlue = Color.parseColor("#0052D4")
    private val colorCyan = Color.parseColor("#00E5FF")

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isAnimating = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradient: LinearGradient? = null

    private val borderPath = Path()
    private val drawnBorderPath = Path()
    private val pathMeasure = PathMeasure()
    private var pathLength = 0f

    private var boxLeft = 0f
    private var boxTop = 0f
    private var boxRight = 0f
    private var boxBottom = 0f

    // For interior elements (Profile Icon and Text Lines)
    private data class AnimatablePath(
        val path: Path,
        val measure: PathMeasure,
        val length: Float,
        val startT: Float,
        val endT: Float
    )
    private val interiorPaths = mutableListOf<AnimatablePath>()

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        val cx = w / 2f
        val cy = h / 2f
        
        // Define card size
        val boxW = min(w, h) * 0.60f
        val boxH = boxW * 1.3f
        
        boxLeft = cx - boxW / 2
        boxTop = cy - boxH / 2
        boxRight = cx + boxW / 2
        boxBottom = cy + boxH / 2

        // Crisp, professional gradient
        gradient = LinearGradient(
            boxLeft, boxTop, boxRight, boxBottom,
            intArrayOf(colorDeepBlue, colorCyan),
            null, Shader.TileMode.CLAMP
        )

        val corner = 24f

        borderPath.reset()
        // Begin at bottom-left corner
        borderPath.moveTo(boxLeft, boxBottom - corner)
        borderPath.lineTo(boxLeft, boxTop + corner)
        borderPath.quadTo(boxLeft, boxTop, boxLeft + corner, boxTop)
        borderPath.lineTo(boxRight - corner, boxTop)
        borderPath.quadTo(boxRight, boxTop, boxRight, boxTop + corner)
        borderPath.lineTo(boxRight, boxBottom - corner)
        borderPath.quadTo(boxRight, boxBottom, boxRight - corner, boxBottom)
        borderPath.lineTo(boxLeft + corner, boxBottom)
        borderPath.quadTo(boxLeft, boxBottom, boxLeft, boxBottom - corner)
        
        borderPath.close()

        pathMeasure.setPath(borderPath, false)
        pathLength = pathMeasure.length

        // Prepare Interior Elements (Profile + Writing)
        interiorPaths.clear()
        val padding = corner * 1.2f
        
        // 1. Profile Icon (Top Left Corner)
        val headR = boxW * 0.13f
        val bodyW = headR * 2.6f
        val bodyH = headR * 1.1f
        
        // Align left edge of body to padding
        val iconCx = boxLeft + padding + bodyW / 2
        val headCy = boxTop + padding + headR
        
        val headPath = Path()
        headPath.addCircle(iconCx, headCy, headR, Path.Direction.CW)
        
        val bodyTop = headCy + headR + padding * 0.3f
        val bodyRect = RectF(iconCx - bodyW / 2, bodyTop, iconCx + bodyW / 2, bodyTop + bodyH * 2)
        val bodyPath = Path()
        bodyPath.addArc(bodyRect, 180f, 180f)
        
        // Profile icon draws simultaneously with the border!
        addAnimatablePath(headPath, 0.0f, 0.35f)
        addAnimatablePath(bodyPath, 0.1f, 0.45f)

        // 2. Writing Lines NEXT to the Profile Icon
        val startXRight = iconCx + bodyW / 2 + padding * 0.8f
        val maxWRight = boxRight - padding - startXRight
        val profileTotalH = headR * 2f + padding * 0.3f + bodyH
        val profileCenterY = boxTop + padding + profileTotalH / 2f
        
        val line1Path = Path()
        line1Path.moveTo(startXRight, profileCenterY - padding * 0.5f)
        line1Path.lineTo(startXRight + maxWRight * 0.9f, profileCenterY - padding * 0.5f)
        addAnimatablePath(line1Path, 0.45f, 0.55f)
        
        val line2Path = Path()
        line2Path.moveTo(startXRight, profileCenterY + padding * 0.5f)
        line2Path.lineTo(startXRight + maxWRight * 0.6f, profileCenterY + padding * 0.5f)
        addAnimatablePath(line2Path, 0.50f, 0.60f)

        // 3. Writing Lines BELOW the Profile Icon
        val startXBottom = boxLeft + padding
        val maxWBottom = boxW - padding * 2
        val startYBottom = bodyTop + bodyH + padding * 1.2f
        
        // Fill the remaining height with dynamic lines
        val availableHeight = boxBottom - padding - startYBottom
        val bottomLineCount = 3
        val bottomSpacing = availableHeight / bottomLineCount.coerceAtLeast(1)
        
        val bottomWidths = floatArrayOf(0.95f, 0.8f, 0.55f)
        for (i in 0 until bottomLineCount) {
            val tw = maxWBottom * bottomWidths[i % bottomWidths.size]
            val lPath = Path()
            lPath.moveTo(startXBottom, startYBottom + i * bottomSpacing)
            lPath.lineTo(startXBottom + tw, startYBottom + i * bottomSpacing)
            
            addAnimatablePath(lPath, 0.55f + i * 0.05f, 0.70f + i * 0.05f)
        }
    }

    private fun addAnimatablePath(path: Path, startT: Float, endT: Float) {
        val measure = PathMeasure(path, false)
        interiorPaths.add(AnimatablePath(path, measure, measure.length, startT, endT))
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val iR = 1f - r
        val red = ((c1 shr 16 and 0xFF) * iR + (c2 shr 16 and 0xFF) * r).toInt()
        val green = ((c1 shr 8 and 0xFF) * iR + (c2 shr 8 and 0xFF) * r).toInt()
        val blue = ((c1 and 0xFF) * iR + (c2 and 0xFF) * r).toInt()
        return Color.rgb(red, green, blue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val globalAlpha = if (progress > 0.85f) {
            1f - ((progress - 0.85f) / 0.15f)
        } else {
            1f
        }
        if (globalAlpha <= 0f) return

        val alphaInt = (globalAlpha * 255).toInt()
        canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), alphaInt)

        // 1. Draw glowing perimeter with Gradient
        val borderT = (progress / 0.45f).coerceIn(0f, 1f)
        
        if (borderT > 0f) {
            drawnBorderPath.reset()
            pathMeasure.getSegment(0f, pathLength * borderT, drawnBorderPath, true)
            drawCrispStroke(canvas, drawnBorderPath, baseWidth = 3.5f)
            
            if (borderT < 1f) {
                val pos = FloatArray(2)
                if (pathMeasure.getPosTan(pathLength * borderT, pos, null)) {
                    drawScannerHead(canvas, pos[0], pos[1])
                }
            } else {
                val bgAlpha = ((progress - 0.45f) / 0.15f).coerceIn(0f, 1f)
                if (bgAlpha > 0f) {
                    paint.style = Paint.Style.FILL
                    paint.shader = gradient
                    paint.alpha = (bgAlpha * 18).toInt()
                    canvas.drawPath(borderPath, paint)
                    paint.shader = null
                }
            }
        }

        // 2. Draw Interior Elements (Profile + Writing)
        for (animPath in interiorPaths) {
            if (progress >= animPath.startT) {
                val t = ((progress - animPath.startT) / (animPath.endT - animPath.startT)).coerceIn(0f, 1f)
                val drawnLine = Path()
                animPath.measure.getSegment(0f, animPath.length * t, drawnLine, true)
                drawCrispStroke(canvas, drawnLine, baseWidth = 4f)
            }
        }

        canvas.restore()
    }

    private fun drawCrispStroke(canvas: Canvas, path: Path, baseWidth: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.shader = gradient 

        // Very subtle blur for professional look
        paint.strokeWidth = baseWidth * 2f
        paint.alpha = 50
        canvas.drawPath(path, paint)

        // Solid core
        paint.strokeWidth = baseWidth
        paint.alpha = 255
        canvas.drawPath(path, paint)
        
        paint.shader = null
    }

    private fun drawScannerHead(canvas: Canvas, cx: Float, cy: Float) {
        val t = ((cx - boxLeft) / (boxRight - boxLeft + 1f) + (cy - boxTop) / (boxBottom - boxTop + 1f)) / 2f
        val dotColor = blendColors(colorDeepBlue, colorCyan, t)

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = dotColor

        paint.alpha = 80
        canvas.drawCircle(cx, cy, 12f, paint)

        paint.color = Color.WHITE
        paint.alpha = 255
        canvas.drawCircle(cx, cy, 4f, paint)
    }

    fun startProgressBar() {
        if (isAnimating) return
        visibility = VISIBLE
        isAnimating = true
        progress = 0f

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200L 
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
        }
        animator?.start()
    }

    fun stopAnimation() {
        isAnimating = false
        animator?.cancel()
        animator = null
        visibility = GONE
        progress = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
