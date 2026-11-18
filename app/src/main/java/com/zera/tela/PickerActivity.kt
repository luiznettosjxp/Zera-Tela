package com.zera.tela

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class PickerActivity : Activity() {
    private class Item(val pkg: String, val label: String, val icon: Drawable)

    private val all = ArrayList<Item>()
    private var shown = ArrayList<Item>()
    private val sel = HashSet<String>()
    private lateinit var countTv: TextView

    private val rowAdapter = object : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(i: Int) = shown[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, cv: View?, p: ViewGroup?): View {
            val item = shown[i]
            val row: LinearLayout
            if (cv is LinearLayout) {
                row = cv
            } else {
                row = LinearLayout(this@PickerActivity)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(8), dp(12), dp(8), dp(12))
                val ic = ImageView(this@PickerActivity)
                val icLp = LinearLayout.LayoutParams(dp(38), dp(38))
                icLp.marginEnd = dp(14)
                row.addView(ic, icLp)
                row.addView(tv("", 16f), LinearLayout.LayoutParams(0, -2, 1f))
                val chk = tv("", 14f, Ui.BG, "sans-serif-medium")
                chk.gravity = Gravity.CENTER
                row.addView(chk, LinearLayout.LayoutParams(dp(26), dp(26)))
            }
            (row.getChildAt(0) as ImageView).setImageDrawable(item.icon)
            (row.getChildAt(1) as TextView).text = item.label
            val on = item.pkg in sel
            val chkView = row.getChildAt(2) as TextView
            chkView.text = if (on) "✓" else ""
            val oval = GradientDrawable()
            oval.shape = GradientDrawable.OVAL
            if (on) {
                oval.setColor(Ui.AC)
            } else {
                oval.setColor(Color.TRANSPARENT)
                oval.setStroke(dp(1), Ui.LINE)
            }
            chkView.background = oval
            row.setBackgroundColor(if (on) Color.parseColor("#146CC4FF") else Color.TRANSPARENT)
            return row
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG2

        val tr = timeRow(60)
        countTv = label("Nenhum app selecionado")

        val search = EditText(this)
        search.hint = "Buscar app"
        search.setHintTextColor(Ui.MUTE)
        search.setTextColor(Ui.INK)
        search.textSize = 17f
        search.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        search.setSingleLine()
        search.background = rect(Ui.CARD, Ui.LINE, 14)
        search.setPadding(dp(14), dp(13), dp(14), dp(13))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString() ?: "")
            }
        })

        val list = ListView(this)
        list.adapter = rowAdapter
        list.divider = ColorDrawable(Ui.LINE)
        list.dividerHeight = 1
        list.selector = ColorDrawable(Color.TRANSPARENT)
        list.setOnItemClickListener { _, _, i, _ ->
            val pk = shown[i].pkg
            if (!sel.add(pk)) sel.remove(pk)
            rowAdapter.notifyDataSetChanged()
            countTv.text = (if (sel.isEmpty()) "Nenhum app selecionado" else sel.size.toString() + " selecionado(s)").uppercase()
        }

        val bottom = card()
        bottom.addView(countTv)
        bottom.addView(tr.view, lp(top = dp(12)))
        bottom.addView(pbtn("Adicionar") {
            val m = tr.minutes()
            if (sel.isEmpty()) {
                Toast.makeText(this, "Escolha pelo menos um app.", Toast.LENGTH_SHORT).show()
            } else if (m < 1) {
                Toast.makeText(this, "Defina pelo menos 1 minuto.", Toast.LENGTH_SHORT).show()
            } else {
                for (pk in sel) Store.set(this, pk, m)
                finish()
            }
        }, lp(top = dp(16)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(24), dp(20), dp(16))
        col.addView(tv("Escolher apps", 34f, Ui.INK, "sans-serif-thin"), lp(top = dp(8)))
        col.addView(tv("Marque os apps e defina o tempo máximo por dia.", 14f, Ui.MUTE), lp(top = dp(4), bottom = dp(16)))
        col.addView(search, lp(bottom = dp(8)))
        col.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(bottom, lp(top = dp(12)))

        val frame = FrameLayout(this)
        frame.addView(GlowBackground(this), FrameLayout.LayoutParams(-1, -1))
        frame.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)

        Thread {
            val pm = packageManager
            val taken = Store.all(this).keys
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val items = pm.queryIntentActivities(q, 0)
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != packageName && it.activityInfo.packageName !in taken }
                .map { Item(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                all.addAll(items)
                applyFilter("")
            }
        }.start()
    }

    private fun applyFilter(q: String) {
        val t = q.trim().lowercase()
        shown = ArrayList(if (t.isEmpty()) all else all.filter { it.label.lowercase().contains(t) })
        rowAdapter.notifyDataSetChanged()
    }
}
