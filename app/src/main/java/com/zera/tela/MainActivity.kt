package com.zera.tela

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private val refresh = object : Runnable {
        override fun run() {
            render(false)
            h.postDelayed(this, 5000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG2
        val frame = FrameLayout(this)
        frame.addView(GlowBackground(this), FrameLayout.LayoutParams(-1, -1))
        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(24), dp(20), dp(40))
        sv.addView(root)
        frame.addView(sv, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    }

    override fun onResume() {
        super.onResume()
        h.removeCallbacks(refresh)
        render(true)
        h.postDelayed(refresh, 5000)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(refresh)
    }

    private fun hasUsage(): Boolean {
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasAccessibility(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, BlockService::class.java)
        return s.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun render(anim: Boolean) {
        root.removeAllViews()
        val usageOk = hasUsage()
        val accOk = hasAccessibility()
        val lim = Store.all(this)
        val used: Map<String, Long> = if (usageOk) Usage.today(this, lim.keys) else emptyMap()
        val totalUsed: Long = used.values.sum()
        val totalLim: Long = lim.values.sum() * 60_000L

        val date = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
        root.addView(label(date), lp(top = dp(8)))
        root.addView(tv("Tempo de uso", 40f, Ui.INK, "sans-serif-thin"), lp(top = dp(6), bottom = dp(22)))

        if (!usageOk || !accOk) {
            val pc = card()
            pc.addView(chip("Falta ativar", Ui.WARN))
            pc.addView(tv("Duas permissões são necessárias para medir o uso e bloquear os apps.", 14f, Ui.MUTE), lp(top = dp(12)))
            if (!usageOk) {
                pc.addView(pbtn("1. Permitir acesso ao uso") {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }, lp(top = dp(16)))
            }
            if (!accOk) {
                pc.addView(pbtn((if (usageOk) "1" else "2") + ". Ativar o bloqueio") {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }, lp(top = dp(12)))
                pc.addView(
                    tv("Se a opção estiver cinza (Android 13+): Informações do app > menu ⋮ > Permitir configurações restritas.", 13f, Ui.MUTE),
                    lp(top = dp(12))
                )
                pc.addView(btn("Informações do app") {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
                }, lp(top = dp(12)))
            }
            root.addView(pc, lp(bottom = dp(16)))
        }

        // ---- Cartão principal com o anel grande ----
        val frac = if (totalLim > 0L) totalUsed.toFloat() / totalLim.toFloat() else 0f
        val tone = Ui.tone(frac)
        val ring = RingView(this)
        ring.strokeDp = 10f
        ring.c1 = tone.first
        ring.c2 = tone.second
        ring.bigSp = 34f
        ring.big = if (usageOk) fmtMs(totalUsed) else "–"
        ring.small = if (totalLim > 0L) "de " + fmtMs(totalLim) else "sem limites"

        val blocked = lim.count { e -> (used[e.key] ?: 0L) >= e.value * 60_000L }
        val hero = card()
        hero.gravity = Gravity.CENTER_HORIZONTAL
        hero.addView(label("Uso hoje"))
        val ringLp = lp(w = dp(210), h = dp(210), top = dp(14))
        ringLp.gravity = Gravity.CENTER_HORIZONTAL
        hero.addView(ring, ringLp)
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER
        chips.addView(chip(lim.size.toString() + (if (lim.size == 1) " app" else " apps"), Ui.AC))
        val chipLp = lp(w = -2)
        chipLp.marginStart = dp(8)
        chips.addView(
            chip(if (blocked == 0) "nenhum bloqueado" else blocked.toString() + " bloqueado(s)", if (blocked == 0) Ui.OK else Ui.BAD),
            chipLp
        )
        hero.addView(chips, lp(top = dp(14)))
        root.addView(hero, lp(bottom = dp(14)))

        if (anim) {
            ring.animateTo(frac, 1000L)
            if (usageOk) {
                val va = ValueAnimator.ofFloat(0f, 1f)
                va.duration = 1000L
                va.interpolator = DecelerateInterpolator(2f)
                va.addUpdateListener {
                    val k = (it.animatedValue as Float).toDouble()
                    ring.big = fmtMs((totalUsed * k).toLong())
                    ring.invalidate()
                }
                va.start()
            }
        } else {
            ring.setNow(frac)
        }

        // ---- Lista de apps ----
        root.addView(label("Apps monitorados"), lp(top = dp(22), bottom = dp(12)))
        if (lim.isEmpty()) {
            val ec = card()
            ec.addView(tv("Nenhum app ainda. Toque em \"Adicionar apps\" para escolher quais limitar.", 14f, Ui.MUTE))
            root.addView(ec, lp(bottom = dp(12)))
        } else {
            val pm = packageManager
            val rows = lim.entries.map { e ->
                val nm = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(e.key, 0)).toString()
                } catch (ex: Exception) {
                    e.key
                }
                Triple(e.key, nm, e.value)
            }.sortedBy { it.second.lowercase() }
            rows.forEachIndexed { i, r ->
                val u: Long? = if (usageOk) (used[r.first] ?: 0L) else null
                root.addView(appRow(r.first, r.second, r.third, u, anim, i), lp(bottom = dp(12)))
            }
        }

        root.addView(pbtn("Adicionar apps") {
            startActivity(Intent(this, PickerActivity::class.java))
        }, lp(top = dp(8)))

        root.addView(
            tv("O tempo zera à meia-noite. Quando um app chega ao limite, ele é fechado e só volta no dia seguinte.", 13f, Ui.MUTE),
            lp(top = dp(22))
        )
        val tools = LinearLayout(this)
        tools.orientation = LinearLayout.HORIZONTAL
        val l1 = LinearLayout.LayoutParams(0, -2, 1f)
        l1.marginEnd = dp(6)
        val l2 = LinearLayout.LayoutParams(0, -2, 1f)
        l2.marginStart = dp(6)
        tools.addView(btn("Info do app") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
        }, l1)
        tools.addView(btn("Bateria") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, l2)
        root.addView(tools, lp(top = dp(18)))

        if (anim) {
            for (i in 0 until root.childCount) root.getChildAt(i).rise(60L * i)
        }
    }

    private fun appRow(pkg: String, name: String, limit: Int, usedMs: Long?, anim: Boolean, idx: Int): View {
        val limMs = limit * 60_000L
        val frac = if (usedMs != null) usedMs.toFloat() / limMs.toFloat() else 0f
        val over = usedMs != null && usedMs >= limMs
        val tone = Ui.tone(frac)
        val icon = try {
            packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }

        val ring = RingView(this)
        ring.strokeDp = 3.5f
        ring.c1 = tone.first
        ring.c2 = tone.second
        if (anim) ring.animateTo(frac, 900L, 150L + idx * 90L) else ring.setNow(frac)

        val iconView = ImageView(this)
        iconView.setImageDrawable(icon)
        val badge = FrameLayout(this)
        badge.addView(ring, FrameLayout.LayoutParams(dp(58), dp(58)))
        badge.addView(iconView, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))

        val remain = if (usedMs != null) ((limMs - usedMs).coerceAtLeast(0L) / 60000L).toInt() else limit
        val sub = when {
            usedMs == null -> "limite " + fmtMin(limit)
            over -> "usou " + fmtMs(usedMs) + " · limite " + fmtMin(limit)
            else -> "usou " + fmtMs(usedMs) + " de " + fmtMin(limit)
        }
        val status = when {
            usedMs == null -> chip("sem leitura", Ui.MUTE)
            over -> chip("bloqueado", Ui.BAD)
            frac >= 0.7f -> chip("restam " + fmtMin(remain), Ui.WARN)
            else -> chip("restam " + fmtMin(remain), Ui.OK)
        }

        val textCol = LinearLayout(this)
        textCol.orientation = LinearLayout.VERTICAL
        textCol.addView(tv(name, 17f))
        textCol.addView(tv(sub, 13f, if (over) Ui.BAD else Ui.MUTE), lp(top = dp(2)))
        textCol.addView(status, lp(w = -2, top = dp(8)))

        val del = tv("×", 26f, Ui.MUTE, "sans-serif-thin")
        del.setPadding(dp(12), dp(4), dp(4), dp(4))
        del.setOnClickListener {
            AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Parar de limitar " + name + "?")
                .setPositiveButton("Remover") { _, _ ->
                    Store.remove(this@MainActivity, pkg)
                    render(true)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        val rowCard = card()
        rowCard.orientation = LinearLayout.HORIZONTAL
        rowCard.gravity = Gravity.CENTER_VERTICAL
        rowCard.setPadding(dp(14), dp(14), dp(10), dp(14))
        val badgeLp = lp(w = dp(58), h = dp(58))
        badgeLp.marginEnd = dp(14)
        rowCard.addView(badge, badgeLp)
        rowCard.addView(textCol, LinearLayout.LayoutParams(0, -2, 1f))
        rowCard.addView(del)
        rowCard.setOnClickListener {
            limitDialog(name, limit) { m ->
                Store.set(this@MainActivity, pkg, m)
                render(true)
            }
        }
        rowCard.press()
        return rowCard
    }
}
