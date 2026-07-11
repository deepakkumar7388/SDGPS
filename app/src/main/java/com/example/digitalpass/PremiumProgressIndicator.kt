package com.example.digitalpass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class PremiumProgressIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class NodeState { PENDING, ACTIVE, COMPLETED, SKIPPED }

    private data class Node(
        val label: String,
        var timeStr: String = "",
        var state: NodeState = NodeState.PENDING,
        var isBypass: Boolean = false
    )

    private val nodes = arrayOf(
        Node("Gate Pass\nApplied"),
        Node("Initial\nApproval"),
        Node("Final\nApproval"),
        Node("Exit")
    )

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun spToPx(sp: Float): Float {
        return sp * context.resources.displayMetrics.scaledDensity
    }

    private val paintLineBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E7EB")
        strokeWidth = dpToPx(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val paintLineFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22C55E")
        strokeWidth = dpToPx(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val paintLineActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dpToPx(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val paintCircleCompleted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22C55E")
        style = Paint.Style.FILL
    }

    private val paintCirclePending = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D1D5DB")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }

    private val paintCircleSkipped = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(4f)), 0f)
    }

    private val paintCircleActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintPulse = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DBEAFE")
        style = Paint.Style.FILL
    }

    private val paintTextLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#374151")
        textSize = spToPx(11f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val paintTextTime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = spToPx(10f)
        textAlign = Paint.Align.CENTER
    }

    private val paintCheck = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dpToPx(2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val paintSkippedBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F4F6")
        style = Paint.Style.FILL
    }
    
    private val paintSkippedText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = spToPx(9f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var pulseRadius = 0f
    private var pulseAlpha = 255
    private var lineFractions = floatArrayOf(0f, 0f, 0f)
    private var nodeScales = floatArrayOf(0f, 0f, 0f, 0f)
    private var activeGradientsSet = false

    private val radius = dpToPx(14f)

    init {
        startPulseAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val shader = LinearGradient(0f, 0f, w.toFloat(), 0f,
            Color.parseColor("#3B82F6"), Color.parseColor("#2563EB"),
            Shader.TileMode.CLAMP)
        paintCircleActive.shader = shader
        paintLineActive.shader = shader
        activeGradientsSet = true
    }

    private fun startPulseAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { anim ->
            val fraction = anim.animatedValue as Float
            pulseRadius = radius + (fraction * radius * 1.2f)
            pulseAlpha = ((1f - fraction) * 150).toInt()
            invalidate()
        }
        animator.start()
    }

    fun setProgressData(
        applyTime: String?,
        initialApprTime: String?,
        finalApprTime: String?,
        exitTime: String?
    ) {
        val hasInitial = !initialApprTime.isNullOrBlank() && initialApprTime != "null"
        val hasFinal = !finalApprTime.isNullOrBlank() && finalApprTime != "null"
        val hasExit = !exitTime.isNullOrBlank() && exitTime != "null"
        
        val isBypassed = !hasInitial && hasFinal

        nodes[0].timeStr = formatTime(applyTime)
        nodes[1].timeStr = formatTime(initialApprTime)
        nodes[2].timeStr = formatTime(finalApprTime)
        nodes[3].timeStr = formatTime(exitTime)

        // Determine States
        nodes[0].state = NodeState.COMPLETED

        if (hasInitial) {
            nodes[1].state = NodeState.COMPLETED
            if (hasFinal) {
                nodes[2].state = NodeState.COMPLETED
                if (hasExit) {
                    nodes[3].state = NodeState.COMPLETED
                } else {
                    nodes[3].state = NodeState.ACTIVE
                }
            } else {
                nodes[2].state = NodeState.ACTIVE
                nodes[3].state = NodeState.PENDING
            }
        } else if (isBypassed) {
            nodes[1].state = NodeState.SKIPPED
            nodes[1].isBypass = true
            nodes[2].state = NodeState.COMPLETED
            if (hasExit) {
                nodes[3].state = NodeState.COMPLETED
            } else {
                nodes[3].state = NodeState.ACTIVE
            }
        } else {
            // No initial, no final -> active is initial
            nodes[1].state = NodeState.ACTIVE
            nodes[2].state = NodeState.PENDING
            nodes[3].state = NodeState.PENDING
        }

        animateProgress()
    }

    private fun animateProgress() {
        // Node scale up
        val scaleAnim = ValueAnimator.ofFloat(0f, 1f)
        scaleAnim.duration = 600
        scaleAnim.interpolator = DecelerateInterpolator(2f)
        scaleAnim.addUpdateListener {
            val v = it.animatedValue as Float
            for (i in 0..3) nodeScales[i] = v
            invalidate()
        }
        scaleAnim.start()

        // Line fill animations
        for (i in 0..2) {
            val targetFraction = when {
                nodes[i].state == NodeState.COMPLETED && nodes[i+1].state != NodeState.PENDING -> 1f
                nodes[i].state == NodeState.SKIPPED && nodes[i+1].state != NodeState.PENDING -> 1f
                nodes[i].state == NodeState.COMPLETED && nodes[i+1].state == NodeState.PENDING -> 0.5f // partial active
                else -> 0f
            }
            if (targetFraction > 0f) {
                val lineAnim = ValueAnimator.ofFloat(0f, targetFraction)
                lineAnim.duration = 800
                lineAnim.startDelay = 200L + (i * 300L)
                lineAnim.interpolator = DecelerateInterpolator()
                lineAnim.addUpdateListener {
                    lineFractions[i] = it.animatedValue as Float
                    invalidate()
                }
                lineAnim.start()
            } else {
                lineFractions[i] = 0f
            }
        }
    }

    private fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank() || timeStr == "null") return ""
        if (timeStr.contains(" ") && timeStr.length > 10) {
            val parts = timeStr.split(" ")
            if (parts.size >= 2) {
                return parts[1] + (if (parts.size > 2) " ${parts[2]}" else "") + "\n" + parts[0]
            }
        }
        return timeStr
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!activeGradientsSet) return

        val width = width.toFloat()
        val height = height.toFloat()
        val cy = height / 2f
        val padding = dpToPx(36f)
        val startX = padding
        val endX = width - padding
        val stepSpacing = (endX - startX) / 3f

        // Draw Lines
        for (i in 0..2) {
            val cx1 = startX + i * stepSpacing
            val cx2 = startX + (i + 1) * stepSpacing

            // Background line
            canvas.drawLine(cx1, cy, cx2, cy, paintLineBg)

            // Fill line
            val fillLength = (cx2 - cx1) * lineFractions[i]
            if (fillLength > 0f) {
                // If it's going to an active node, use gradient line, else green line
                val paint = if (nodes[i+1].state == NodeState.ACTIVE && lineFractions[i] < 1f) paintLineActive else paintLineFill
                canvas.drawLine(cx1, cy, cx1 + fillLength, cy, paint)
            }
        }

        // Draw Nodes
        for (i in 0..3) {
            val cx = startX + i * stepSpacing
            val node = nodes[i]
            val scale = nodeScales[i]
            val scaledRadius = radius * scale

            when (node.state) {
                NodeState.ACTIVE -> {
                    paintPulse.alpha = pulseAlpha
                    canvas.drawCircle(cx, cy, pulseRadius * scale, paintPulse)
                    canvas.drawCircle(cx, cy, scaledRadius, paintCircleActive)
                }
                NodeState.COMPLETED -> {
                    canvas.drawCircle(cx, cy, scaledRadius, paintCircleCompleted)
                    // Draw Checkmark
                    val checkPath = Path()
                    checkPath.moveTo(cx - dpToPx(4f) * scale, cy)
                    checkPath.lineTo(cx - dpToPx(1f) * scale, cy + dpToPx(3f) * scale)
                    checkPath.lineTo(cx + dpToPx(4f) * scale, cy - dpToPx(3f) * scale)
                    canvas.drawPath(checkPath, paintCheck)
                }
                NodeState.PENDING -> {
                    canvas.drawCircle(cx, cy, scaledRadius, paintCirclePending)
                }
                NodeState.SKIPPED -> {
                    canvas.drawCircle(cx, cy, scaledRadius, paintCircleSkipped)
                    // Draw Skipped Badge
                    val badgeW = dpToPx(24f)
                    val badgeH = dpToPx(8f)
                    val badgeRect = RectF(cx - badgeW, cy - badgeH, cx + badgeW, cy + badgeH)
                    canvas.drawRoundRect(badgeRect, dpToPx(4f), dpToPx(4f), paintSkippedBadge)
                    canvas.drawText("SKIPPED", cx, cy + dpToPx(3f), paintSkippedText)
                }
            }

            // Draw Texts
            val lines = node.label.split("\n")
            var textY = cy + radius + dpToPx(16f)
            for (line in lines) {
                canvas.drawText(line, cx, textY, paintTextLabel)
                textY += dpToPx(14f)
            }

            if (node.timeStr.isNotEmpty()) {
                val timeLines = node.timeStr.split("\n")
                var timeY = cy - radius - dpToPx(8f) - (timeLines.size - 1) * dpToPx(12f)
                for (t in timeLines) {
                    canvas.drawText(t, cx, timeY, paintTextTime)
                    timeY += dpToPx(12f)
                }
            } else if (node.state == NodeState.PENDING) {
                canvas.drawText("--:--", cx, cy - radius - dpToPx(8f), paintTextTime)
            }
        }
    }
}
