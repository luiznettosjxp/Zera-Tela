package com.zera.tela

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object Ui {
    val BG = Color.parseColor("#06080B")
    val BG2 = Color.parseColor("#0B1324")
    val INK = Color.parseColor("#F2F5F8")
    val MUTE = Color.parseColor("#8793A0")
    val LINE = Color.parseColor("#26FFFFFF")
    val CARD = Color.parseColor("#0DFFFFFF")
    val AC = Color.parseColor("#6CC4FF")
    val AC2 = Color.parseColor("#A08BFF")
    val OK = Color.parseColor("#4ADE9A")
    val WARN = Color.parseColor("#FFB84D")
    val WARN2 = Color.parseColor("#FF8A4D")
    val BAD = Color.parseColor("#FF6B63")
    val BAD2 = Color.parseColor("#FF4D8D")

    /** Par de cores do anel conforme a fração do limite usada. */
    fun tone(f: Float): Pair<Int, Int> = when {
        f >= 1f -> Pair(BAD, BAD2)
        f >= 0.7f -> Pair(WARN, WARN2)
        else -> Pair(AC, AC2)
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun lp(w: Int = -1, h: Int = -2, top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(w, h).apply { topMargin = top; bottomMargin = bottom }

fun Context.tv(s: CharSequence, sp: Float = 16f, color: Int = Ui.INK, face: String = "sans-serif-light"): TextView =
    TextView(this).apply {
        text = s
        textSize = sp
        setTextColor(color)
        typeface = Typeface.create(face, Typeface.NORMAL)
    }

fun Context.label(s: String, color: Int = Ui.MUTE): TextView =
    tv(s.uppercase(), 12f, color, "sans-serif-condensed").apply { letterSpacing = 0.2f }

fun Context.rect(fill: Int, stroke: Int, radius: Int = 14): GradientDrawable = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = dp(radius).toFloat()
    setStroke(dp(1), stroke)
}

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = rect(Ui.CARD, Ui.LINE, 18)
    setPadding(dp(18), dp(18), dp(18), dp(18))
}

/** Efeito de "apertar" ao tocar. */
fun View.press() {
    setOnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90L).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()
        }
        false
    }
}

/** Entrada suave: sobe e aparece. */
fun View.rise(delay: Long) {
    alpha = 0f
    translationY = resources.displayMetrics.density * 18f
    animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(520L)
        .setInterpolator(DecelerateInterpolator(2f)).start()
}

fun Context.btn(s: String, onClick: () -> Unit): TextView =
    tv(s.uppercase(), 12f, Ui.INK, "sans-serif-medium").apply {
        letterSpacing = 0.2f
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(16), dp(14), dp(16))
        background = rect(Color.TRANSPARENT, Color.parseColor("#55FFFFFF"), 14)
        isClickable = true
        setOnClickListener { onClick() }
        press()
    }

fun Context.pbtn(s: String, onClick: () -> Unit): TextView =
    tv(s.uppercase(), 12f, Ui.BG, "sans-serif-medium").apply {
        letterSpacing = 0.2f
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(17), dp(14), dp(17))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Ui.AC, Ui.AC2)).apply {
            cornerRadius = dp(14).toFloat()
        }
        isClickable = true
        setOnClickListener { onClick() }
        press()
    }

fun Context.chip(s: String, textColor: Int): TextView {
    val soft = Color.argb(40, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
    val bgShape = GradientDrawable()
    bgShape.setColor(soft)
    bgShape.cornerRadius = dp(20).toFloat()
    val t = tv(s.uppercase(), 10.5f, textColor, "sans-serif-medium")
    t.letterSpacing = 0.14f
    t.setPadding(dp(10), dp(5), dp(10), dp(5))
    t.background = bgShape
    return t
}

fun fmtMin(m: Int): String = if (m >= 60) "${m / 60}h ${"%02d".format(m % 60)}min" else "${m}min"
fun fmtMs(ms: Long): String = fmtMin((ms / 60000).toInt())

/** Anel de progresso com gradiente e texto central. */
class RingView(ctx: Context) : View(ctx) {
    var frac = 0f
    var c1 = Ui.AC
    var c2 = Ui.AC2
    var strokeDp = 8f
    var big = ""
    var small = ""
    var bigSp = 28f
    var smallSp = 11f

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Ui.LINE
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.INK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    }
    private val ts = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.MUTE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    }
    private val box = RectF()

    fun animateTo(target: Float, ms: Long = 900L, delayMs: Long = 0L) {
        val tgt = target.coerceIn(0f, 1f)
        val va = ValueAnimator.ofFloat(0f, tgt)
        va.duration = ms
        va.startDelay = delayMs
        va.interpolator = DecelerateInterpolator(2f)
        va.addUpdateListener { frac = it.animatedValue as Float; invalidate() }
        va.start()
    }

    fun setNow(f: Float) {
        frac = f.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val sw = strokeDp * resources.displayMetrics.density
        track.strokeWidth = sw
        arc.strokeWidth = sw
        val half = sw / 2f
        val cx = width / 2f
        val cy = height / 2f
        box.set(half, half, width - half, height - half)
        cv.drawArc(box, 0f, 360f, false, track)
        if (frac > 0.004f) {
            val end = minOf(frac, 0.999f)
            arc.shader = SweepGradient(cx, cy, intArrayOf(c1, c2, c2), floatArrayOf(0f, end, 1f))
            cv.save()
            cv.rotate(-90f, cx, cy)
            cv.drawArc(box, 0f, 360f * frac, false, arc)
            cv.restore()
        }
        if (big.isNotEmpty()) {
            tb.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, bigSp, resources.displayMetrics)
            ts.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, smallSp, resources.displayMetrics)
            val off = if (small.isNotEmpty()) tb.textSize * 0.25f else 0f
            val by = cy - (tb.ascent() + tb.descent()) / 2f - off
            cv.drawText(big, cx, by, tb)
            if (small.isNotEmpty()) cv.drawText(small.uppercase(), cx, by + ts.textSize * 1.9f, ts)
        }
    }
}

/** Fundo escuro com dois brilhos coloridos que se movem devagar. */
class GlowBackground(ctx: Context, private val g1: Int = Ui.AC, private val g2: Int = Ui.AC2) : View(ctx) {
    private val base = Paint()
    private val p1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val p2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private var rad = 1f
    private var anim: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        base.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Ui.BG, Ui.BG2, Shader.TileMode.CLAMP)
        rad = maxOf(w, h) * 0.6f
        p1.shader = RadialGradient(0f, 0f, rad, Color.argb(70, Color.red(g1), Color.green(g1), Color.blue(g1)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        p2.shader = RadialGradient(0f, 0f, rad, Color.argb(55, Color.red(g2), Color.green(g2), Color.blue(g2)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val va = ValueAnimator.ofFloat(0f, 1f)
        va.duration = 14000L
        va.repeatCount = ValueAnimator.INFINITE
        va.repeatMode = ValueAnimator.REVERSE
        va.addUpdateListener { t = it.animatedValue as Float; invalidate() }
        va.start()
        anim = va
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        cv.drawRect(0f, 0f, w, h, base)
        cv.save()
        cv.translate(w * (0.15f + 0.3f * t), h * (0.05f + 0.15f * t))
        cv.drawCircle(0f, 0f, rad, p1)
        cv.restore()
        cv.save()
        cv.translate(w * (0.95f - 0.3f * t), h * (0.95f - 0.2f * t))
        cv.drawCircle(0f, 0f, rad, p2)
        cv.restore()
    }
}

class TimeRow(val view: View, private val hh: EditText, private val mm: EditText) {
    fun minutes(): Int =
        ((hh.text.toString().toIntOrNull() ?: 0) * 60 + (mm.text.toString().toIntOrNull() ?: 0)).coerceAtMost(1440)
}

fun Context.timeRow(totalMin: Int): TimeRow {
    fun field(v: Int) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(v.toString())
        textSize = 20f
        setTextColor(Ui.INK)
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        gravity = Gravity.CENTER
        background = rect(Color.TRANSPARENT, Ui.LINE, 12)
        setPadding(dp(10), dp(11), dp(10), dp(11))
        filters = arrayOf(InputFilter.LengthFilter(2))
        setSelectAllOnFocus(true)
    }
    val hEdit = field(totalMin / 60)
    val mEdit = field(totalMin % 60)
    fun unit(s: String) = tv(s, 16f, Ui.MUTE).apply { setPadding(dp(8), 0, dp(18), 0) }
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(hEdit, LinearLayout.LayoutParams(dp(68), -2))
        addView(unit("h"))
        addView(mEdit, LinearLayout.LayoutParams(dp(68), -2))
        addView(unit("min"))
    }
    return TimeRow(row, hEdit, mEdit)
}

fun Activity.limitDialog(title: String, cur: Int, ok: (Int) -> Unit) {
    val tr = timeRow(cur)
    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), 0)
        addView(label("Tempo máximo por dia"))
        addView(tr.view, lp(top = dp(12)))
    }
    AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle(title)
        .setView(box)
        .setPositiveButton("Salvar") { _, _ ->
            val m = tr.minutes()
            if (m < 1) Toast.makeText(this, "Defina pelo menos 1 minuto.", Toast.LENGTH_SHORT).show() else ok(m)
        }
        .setNegativeButton("Cancelar", null)
        .show()
}
