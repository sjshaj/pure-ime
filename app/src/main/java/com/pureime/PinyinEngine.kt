package com.pureime

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object PinyinEngine {
    private val dict = mutableMapOf<String, List<String>>()
    private var isLoaded = false

    fun init(context: Context) {
        if (isLoaded) return
        try {
            context.assets.open("pinyin_dict.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || !trimmed.contains("=")) continue
                        val parts = trimmed.split("=", limit = 2)
                        val pinyin = parts[0].trim()
                        val words = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        dict[pinyin] = words
                    }
                }
            }
            isLoaded = true
        } catch (_: Exception) {}
    }

    // 根据输入的拼音获取候选词
    fun getCandidates(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val key = input.lowercase().trim()
        val results = mutableListOf<String>()

        // 1. 精确匹配
        dict[key]?.let { results.addAll(it) }

        // 2. 前缀匹配（当用户还在输入时，如输入 zhon 时联想 zhong, zhongguo）
        if (results.size < 10) {
            dict.keys.filter { it.startsWith(key) && it != key }
                .sortedBy { it.length }
                .take(5)
                .forEach { matchedKey ->
                    dict[matchedKey]?.firstOrNull()?.let {
                        if (!results.contains(it)) results.add(it)
                    }
                }
        }

        // 3. 兜底切分（简单两音节切分）
        if (results.isEmpty() && key.length >= 3) {
            for (i in 2..key.length - 2) {
                val part1 = key.substring(0, i)
                val part2 = key.substring(i)
                val cand1 = dict[part1]?.firstOrNull()
                val cand2 = dict[part2]?.firstOrNull()
                if (cand1 != null && cand2 != null) {
                    val combined = cand1 + cand2
                    if (!results.contains(combined)) results.add(combined)
                    break
                }
            }
        }

        return results
    }
}
