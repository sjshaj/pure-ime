package com.pureime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 80, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "纯音输入法"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text = "极简 · 中英文 · 物理断网 · 0延迟"
            setTextColor(Color.parseColor("#888888"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }

        val btnEnable = createStyledButton("第一步：在系统设置中启用输入法") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        val btnSelect = createStyledButton("第二步：选择纯音输入法为当前输入法") {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        val testHint = TextView(this).apply {
            text = "在此测试输入："
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 14f
            setPadding(0, 64, 0, 16)
        }

        val testInput = EditText(this).apply {
            hint = "点我调起纯音输入法测试打字..."
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(32, 32, 32, 32)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E1E"))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#333333"))
            }
            background = bg
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(btnEnable)
        root.addView(createSpacer(24))
        root.addView(btnSelect)
        root.addView(testHint)
        root.addView(testInput)

        setContentView(root)
    }

    private fun createStyledButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.BLACK)
            textSize = 15f
            isAllCaps = false
            val shape = GradientDrawable().apply {
                setColor(Color.parseColor("#03DAC6"))
                cornerRadius = 24f
            }
            background = shape
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createSpacer(dp: Int): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (dp * resources.displayMetrics.density).toInt()
            )
        }
    }
}
