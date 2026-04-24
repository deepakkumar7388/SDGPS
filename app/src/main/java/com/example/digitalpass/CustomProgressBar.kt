package com.example.digitalpass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class CustomProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var dX = 0f
    private var dY = 0f
    private var pX = 0f
    private var pY = 0f
    private var animationValue = 0f

    private var animator: ValueAnimator? = null
    private var isAnimating = false

    private val colors = intArrayOf(
        Color.parseColor("#FF512F"), // Orange Red
        Color.parseColor("#DD2476"), // Pink
        Color.parseColor("#052E92"), // Theme Blue
        Color.parseColor("#00C6FF"), // Sky Blue
        Color.parseColor("#0072FF"), // Bright Blue
        Color.parseColor("#FF512F")  // Back to start for smooth loop
    )

    private val paint = Paint().apply {
        textSize = 120f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        // Set a slight shadow for depth
        setShadowLayer(10f, 0f, 5f, Color.parseColor("#40000000"))
    }

    private val ringPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetPosition()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun resetPosition() {
        val centerX = width / 2f
        val centerY = height / 2f
        dX = centerX - 40f
        pX = centerX + 40f
        dY = centerY
        pY = centerY
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val centerX = width / 2f
        val centerY = height / 2f

        // Create a rotating gradient for the letters
        val matrix = Matrix()
        matrix.setRotate(animationValue * 360f, centerX, centerY)
        
        val shader = SweepGradient(centerX, centerY, colors, null)
        shader.setLocalMatrix(matrix)
        paint.shader = shader

        // Create a gradient for the outer ring as well
        ringPaint.shader = shader
        ringPaint.alpha = 100 // Semi-transparent ring
        
        val radius = width / 3.5f
        canvas.drawCircle(centerX, centerY, radius, ringPaint)

        // Draw "D"
        canvas.drawText("P", dX, dY + 40f, paint)

        // Draw "P"
        canvas.drawText("D", pX, pY + 40f, paint)
    }

    fun startProgressBar() {
        if (isAnimating) return
        visibility = VISIBLE
        isAnimating = true

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
//            interpolator = LinearInterpolator()
            addUpdateListener {
                animationValue = it.animatedValue as Float
                val centerX = width / 2f
                val centerY = height / 2f
                val radius = width / 5f

                // D clockwise
                val angleD = 2 * Math.PI * animationValue
                dX = centerX + radius * cos(angleD).toFloat()
                dY = centerY + radius * sin(angleD).toFloat()

                // P opposite
                val angleP = angleD + Math.PI
                pX = centerX + radius * cos(angleP).toFloat()
                pY = centerY + radius * sin(angleP).toFloat()

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
    }
}
