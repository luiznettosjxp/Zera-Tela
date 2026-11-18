package com.zera.tela

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.animation.ValueAnimator

/**
 * Observa qual app está em primeiro plano. Se ele tem limite diário e já passou,
 * volta para a tela inicial e cobre a tela com a mensagem motivacional.
 */
class BlockService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var cur: String? = null
    private var overlay: View? = null
    private val launchable = HashMap<String, Boolean>()

    private val tick = object : Runnable {
        override fun run() {
            cur?.let { check(it) }
            h.postDelayed(this, 3000)
        }
    }

    override fun onServiceConnected() {
        h.removeCallbacks(tick)
        h.post(tick)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        h.removeCallbacks(tick)
        hide()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val p = e.packageName?.toString() ?: return
        val isApp = p == packageName || launchable.getOrPut(p) { packageManager.getLaunchIntentForPackage(p) != null }
        if (!isApp) return // teclado, barra de status etc. não mudam o app atual
        cur = p
        check(p)
    }

    private fun check(p: String) {
        if (overlay != null) return
        val lim = Store.get(this, p) ?: return
        val used = Usage.today(this, setOf(p))[p] ?: 0L
        if (used >= lim * 60_000L) show(p, lim)
    }

    private fun show(pkg: String, lim: Int) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        cur = packageName
        val name = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
        val v = build(pkg, name, lim, Phrases.next(this))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v, params)
            overlay = v
            v.isFocusableInTouchMode = true
            v.requestFocus()
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun hide() {
        val o = overlay
        if (o != null) {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(o)
            } catch (e: Exception) {
            }
        }
        overlay = null
    }

    private fun close() {
        hide()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun build(pkg: String, name: String, lim: Int, phrase: String): View {
        val icon = try {
            packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }

        val ring = RingView(this)
        ring.strokeDp = 5f
        ring.c1 = Ui.BAD
        ring.c2 = Ui.BAD2
        ring.setNow(1f)
        val iconView = ImageView(this)
        iconView.setImageDrawable(icon)
        val badge = FrameLayout(this)
        badge.addView(ring, FrameLayout.LayoutParams(dp(112), dp(112)))
        badge.addView(iconView, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER))

        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            badge,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.07f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.07f)
        )
        pulse.duration = 1400L
        pulse.repeatCount = ValueAnimator.INFINITE
        pulse.repeatMode = ValueAnimator.REVERSE
        pulse.start()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_VERTICAL
        col.setPadding(dp(28), dp(64), dp(28), dp(48))

        val badgeLp = lp(w = dp(112), h = dp(112))
        badgeLp.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(badge, badgeLp)

        val chipLp = lp(w = -2, top = dp(26))
        chipLp.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(chip("Limite diário atingido", Ui.BAD), chipLp)

        val nameTv = tv(name, 22f, Ui.INK, "sans-serif-light")
        nameTv.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(nameTv, lp(top = dp(12)))
        val limTv = tv(fmtMin(lim) + " por dia", 14f, Ui.MUTE)
        limTv.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(limTv, lp(top = dp(2)))

        val bar = View(this)
        bar.setBackgroundColor(Ui.AC)
        val quote = LinearLayout(this)
        quote.orientation = LinearLayout.HORIZONTAL
        quote.addView(bar, LinearLayout.LayoutParams(dp(2), -1))
        val phraseTv = tv(phrase, 26f, Ui.INK, "sans-serif-thin")
        phraseTv.setPadding(dp(18), dp(4), 0, dp(4))
        phraseTv.setLineSpacing(0f, 1.15f)
        quote.addView(phraseTv, LinearLayout.LayoutParams(-1, -2))
        col.addView(quote, lp(top = dp(40)))

        col.addView(tv("Seu tempo volta a ser liberado à meia-noite.", 14f, Ui.MUTE), lp(top = dp(28)))
        col.addView(pbtn("Voltar") { close() }, lp(top = dp(32)))

        for (i in 0 until col.childCount) col.getChildAt(i).rise(100L * i)

        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.addView(col, FrameLayout.LayoutParams(-1, -1))

        val root = FrameLayout(this)
        root.addView(GlowBackground(this, Ui.BAD, Ui.AC2), FrameLayout.LayoutParams(-1, -1))
        root.addView(sv, FrameLayout.LayoutParams(-1, -1))
        root.setOnKeyListener { _, k, ev ->
            if (k == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP) {
                close()
                true
            } else {
                false
            }
        }
        return root
    }
}
