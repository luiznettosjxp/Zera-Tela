package com.zera.tela

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/** Limites diários em minutos, por pacote. */
object Store {
    private fun sp(c: Context) = c.getSharedPreferences("zera_tela", Context.MODE_PRIVATE)

    fun all(c: Context): Map<String, Int> {
        val o = JSONObject(sp(c).getString("limits", "{}") ?: "{}")
        return o.keys().asSequence().associateWith { o.getInt(it) }
    }

    fun get(c: Context, pkg: String): Int? = all(c)[pkg]

    fun set(c: Context, pkg: String, min: Int) {
        val o = JSONObject(sp(c).getString("limits", "{}") ?: "{}")
        o.put(pkg, min)
        sp(c).edit().putString("limits", o.toString()).apply()
    }

    fun remove(c: Context, pkg: String) {
        val o = JSONObject(sp(c).getString("limits", "{}") ?: "{}")
        o.remove(pkg)
        sp(c).edit().putString("limits", o.toString()).apply()
    }

    fun lastPhrase(c: Context): Int = sp(c).getInt("last_phrase", -1)
    fun setLastPhrase(c: Context, i: Int) = sp(c).edit().putInt("last_phrase", i).apply()
}

/** Tempo em primeiro plano desde 00:00 de hoje, em ms, a partir do histórico de eventos do sistema. */
object Usage {
    private const val RESUMED = 1
    private const val PAUSED = 2
    private const val STOPPED = 23
    private const val SCREEN_OFF = 16
    private const val SHUTDOWN = 26

    fun today(ctx: Context, pkgs: Set<String>): Map<String, Long> {
        if (pkgs.isEmpty()) return emptyMap()
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(cal.timeInMillis, now)
        val e = UsageEvents.Event()
        val open = HashMap<String, MutableSet<String>>()
        val since = HashMap<String, Long>()
        val total = HashMap<String, Long>()

        fun close(p: String, t: Long) {
            val s = since.remove(p) ?: return
            open.remove(p)
            total[p] = (total[p] ?: 0L) + (t - s).coerceAtLeast(0L)
        }

        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val p = e.packageName ?: continue
            when (e.eventType) {
                RESUMED -> if (p in pkgs) {
                    val cls = e.className ?: p
                    val set = open.getOrPut(p) { HashSet() }
                    if (set.isEmpty()) since[p] = e.timeStamp
                    set.add(cls)
                }
                PAUSED, STOPPED -> if (p in pkgs) {
                    val set = open[p]
                    if (set != null) {
                        set.remove(e.className ?: p)
                        if (set.isEmpty()) close(p, e.timeStamp)
                    }
                }
                SCREEN_OFF, SHUTDOWN -> for (q in since.keys.toList()) close(q, e.timeStamp)
            }
        }
        for (q in since.keys.toList()) close(q, now)
        return total
    }
}
