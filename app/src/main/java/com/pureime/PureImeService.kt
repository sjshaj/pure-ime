package com.pureime

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class PureImeService : InputMethodService() {

    private var isChinese = true
    private var isSymbols = false
    private var isCaps = false

    private val pinyinBuffer = StringBuilder()

    private lateinit var rootLayout: LinearLayout
    private lateinit var candidateScroll: HorizontalScrollView
    private lateinit var candidateBar: LinearLayout
    private lateinit var pinyinPreview: TextView
    private lateinit var keyboardContainer: LinearLayout

    override fun onCreate() {
        super.onCreate()
        PinyinEngine.init(this)
    }

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#181818"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // [候选词栏]
        val candidateHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#222222"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (44 * density).toInt()
            )
        }

        pinyinPreview = TextView(this).apply {
            setTextColor(Color.parseColor("#03DAC6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            visibility = View.GONE
            setPadding(0, 0, (12 * density).toInt(), 0)
        }

        candidateBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        candidateScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
            addView(candidateBar)
        }

        candidateHeader.addView(pinyinPreview)
        candidateHeader.addView(candidateScroll)

        // [键盘按键区]
        keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        rootLayout.addView(candidateHeader)
        rootLayout.addView(keyboardContainer)

        buildKeyboard()
        return rootLayout
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        clearBuffer()
        buildKeyboard()
    }

    private fun clearBuffer() {
        pinyinBuffer.clear()
        pinyinPreview.text = ""
        pinyinPreview.visibility = View.GONE
        candidateBar.removeAllViews()
    }

    private fun updateCandidates() {
        val density = resources.displayMetrics.density
        candidateBar.removeAllViews()

        if (pinyinBuffer.isEmpty()) {
            pinyinPreview.visibility = View.GONE
            return
        }

        pinyinPreview.text = "$pinyinBuffer'"
        pinyinPreview.visibility = View.VISIBLE

        val candidates = PinyinEngine.getCandidates(pinyinBuffer.toString())
        if (candidates.isEmpty()) {
            // 没有匹配时显示原始拼音作为候选
            val rawText = pinyinBuffer.toString()
            candidateBar.addView(createCandidateView(rawText, isFirst = true))
        } else {
            candidates.forEachIndexed { index, cand ->
                candidateBar.addView(createCandidateView(cand, isFirst = (index == 0)))
            }
        }
        candidateScroll.scrollTo(0, 0)
    }

    private fun createCandidateView(text: String, isFirst: Boolean): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(if (isFirst) Color.parseColor("#03DAC6") else Color.WHITE)
            typeface = if (isFirst) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.CENTER
            setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
            setOnClickListener {
                commitCandidate(text)
            }
        }
    }

    private fun commitCandidate(text: String) {
        currentInputConnection?.commitText(text, 1)
        clearBuffer()
    }

    private fun buildKeyboard() {
        keyboardContainer.removeAllViews()

        if (isSymbols) {
            buildSymbolsKeyboard()
        } else {
            buildLetterKeyboard()
        }
    }

    private fun buildLetterKeyboard() {
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

        keyboardContainer.addView(createKeyRow(row1))
        keyboardContainer.addView(createKeyRow(row2))

        // Row 3 (Shift + z..m + Delete)
        val density = resources.displayMetrics.density
        val r3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * density).toInt())
        }

        // Shift 键
        val shiftKey = createKey(if (isCaps) "⇪" else "⇧", weight = 1.4f, isSpecial = true) {
            isCaps = !isCaps
            buildKeyboard()
        }
        r3.addView(shiftKey)

        row3.forEach { key ->
            r3.addView(createKey(if (isCaps) key.uppercase() else key, weight = 1f) {
                handleKeyInput(key)
            })
        }

        // Delete 键
        val delKey = createKey("⌫", weight = 1.4f, isSpecial = true) {
            handleDelete()
        }
        r3.addView(delKey)
        keyboardContainer.addView(r3)

        // Row 4 (功能栏: 符号, 中/EN, 空格, 标点, 回车)
        val r4 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * density).toInt())
        }

        // ?123 切换
        r4.addView(createKey("?123", weight = 1.3f, isSpecial = true) {
            isSymbols = true
            clearBuffer()
            buildKeyboard()
        })

        // 中/EN 切换
        r4.addView(createKey(if (isChinese) "中" else "EN", weight = 1.2f, isSpecial = true) {
            isChinese = !isChinese
            clearBuffer()
            buildKeyboard()
        })

        // 逗号
        r4.addView(createKey(if (isChinese) "，" else ",", weight = 1f) {
            if (pinyinBuffer.isNotEmpty()) commitFirstCandidate()
            currentInputConnection?.commitText(if (isChinese) "，" else ",", 1)
        })

        // 空格（核心按键）
        val spaceLabel = if (isChinese) "空格" else "Space"
        r4.addView(createKey(spaceLabel, weight = 3.5f) {
            if (isChinese && pinyinBuffer.isNotEmpty()) {
                commitFirstCandidate()
            } else {
                currentInputConnection?.commitText(" ", 1)
            }
        })

        // 句号
        r4.addView(createKey(if (isChinese) "。" else ".", weight = 1f) {
            if (pinyinBuffer.isNotEmpty()) commitFirstCandidate()
            currentInputConnection?.commitText(if (isChinese) "。" else ".", 1)
        })

        // 回车 (↵)
        r4.addView(createKey("↵", weight = 1.5f, isAccent = true) {
            if (pinyinBuffer.isNotEmpty()) {
                currentInputConnection?.commitText(pinyinBuffer.toString(), 1)
                clearBuffer()
            } else {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        })

        keyboardContainer.addView(r4)
    }

    private fun buildSymbolsKeyboard() {
        val density = resources.displayMetrics.density
        val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row2 = listOf("！", "@", "#", "¥", "%", "&", "*", "（", "）", "／")
        val row3 = listOf("——", "+", "=", "：", "；", "“", "”", "‘", "’")

        keyboardContainer.addView(createKeyRow(row1))
        keyboardContainer.addView(createKeyRow(row2))

        val r3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * density).toInt())
        }
        row3.forEach { sym ->
            r3.addView(createKey(sym, weight = 1f) {
                currentInputConnection?.commitText(sym, 1)
            })
        }
        r3.addView(createKey("⌫", weight = 1.4f, isSpecial = true) {
            handleDelete()
        })
        keyboardContainer.addView(r3)

        // 底栏
        val r4 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * density).toInt())
        }

        r4.addView(createKey("ABC", weight = 1.5f, isSpecial = true) {
            isSymbols = false
            buildKeyboard()
        })
        r4.addView(createKey("，", weight = 1f) { currentInputConnection?.commitText("，", 1) })
        r4.addView(createKey("空格", weight = 3.5f) { currentInputConnection?.commitText(" ", 1) })
        r4.addView(createKey("。", weight = 1f) { currentInputConnection?.commitText("。", 1) })
        r4.addView(createKey("？", weight = 1f) { currentInputConnection?.commitText("？", 1) })
        r4.addView(createKey("↵", weight = 1.5f, isAccent = true) {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        })
        keyboardContainer.addView(r4)
    }

    private fun createKeyRow(keys: List<String>): LinearLayout {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (52 * density).toInt()
            )
        }
        keys.forEach { key ->
            val label = if (isCaps) key.uppercase() else key
            row.addView(createKey(label, weight = 1f) {
                handleKeyInput(key)
            })
        }
        return row
    }

    private fun handleKeyInput(key: String) {
        if (isChinese) {
            pinyinBuffer.append(key.lowercase())
            updateCandidates()
        } else {
            val charToCommit = if (isCaps) key.uppercase() else key.lowercase()
            currentInputConnection?.commitText(charToCommit, 1)
        }
    }

    private fun handleDelete() {
        if (pinyinBuffer.isNotEmpty()) {
            pinyinBuffer.deleteCharAt(pinyinBuffer.length - 1)
            updateCandidates()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun commitFirstCandidate() {
        val candidates = PinyinEngine.getCandidates(pinyinBuffer.toString())
        val textToCommit = if (candidates.isNotEmpty()) candidates[0] else pinyinBuffer.toString()
        commitCandidate(textToCommit)
    }

    private fun createKey(
        text: String,
        weight: Float,
        isSpecial: Boolean = false,
        isAccent: Boolean = false,
        onClick: () -> Unit
    ): View {
        val density = resources.displayMetrics.density
        val btn = TextView(this).apply {
            this.text = text
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (isAccent) Color.BLACK else Color.WHITE)

            val bgColor = when {
                isAccent -> Color.parseColor("#03DAC6")
                isSpecial -> Color.parseColor("#333333")
                else -> Color.parseColor("#262626")
            }

            val shape = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 10 * density
            }
            background = shape

            setOnClickListener { onClick() }
        }

        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
            setMargins((2.5f * density).toInt(), (3 * density).toInt(), (2.5f * density).toInt(), (3 * density).toInt())
        }
        btn.layoutParams = lp
        return btn
    }
}
