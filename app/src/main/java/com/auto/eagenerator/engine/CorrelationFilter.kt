package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlin.math.*

/**
 * 品种相关性过滤 - 避免高度相关品种重复入场
 */
object CorrelationFilterEngine {

    fun check(cfg: CorrelationFilter, prices: Map<String, List<Double>>): CorrelationCheckResult {
        if (!cfg.enabled || prices.size < 2) {
            return CorrelationCheckResult(passed = true, correlation = 1.0, pairs = emptyMap())
        }

        val symbols = prices.keys.toList()
        val results = mutableMapOf<String, Double>()
        var maxCorr = 0.0

        for (i in symbols.indices) {
            for (j in i + 1 until symbols.size) {
                val corr = pearsonCorrelation(
                    prices[symbols[i]] ?: emptyList(),
                    prices[symbols[j]] ?: emptyList()
                )
                val pairKey = "${symbols[i]}-${symbols[j]}"
                results[pairKey] = corr
                maxCorr = maxOf(maxCorr, abs(corr))
            }
        }

        val passed = maxCorr <= cfg.maxCorrelation
        return CorrelationCheckResult(
            passed = passed,
            correlation = maxCorr,
            pairs = results
        )
    }

    private fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        val n = minOf(x.size, y.size)
        if (n < 2) return 1.0

        val mx = x.takeLast(n).average()
        val my = y.takeLast(n).average()

        var cov = 0.0
        var vx = 0.0
        var vy = 0.0

        for (i in 0 until n) {
            val dx = x[x.size - n + i] - mx
            val dy = y[y.size - n + i] - my
            cov += dx * dy
            vx += dx * dx
            vy += dy * dy
        }

        return if (vx > 0 && vy > 0) cov / sqrt(vx * vy) else 1.0
    }

    data class CorrelationCheckResult(
        val passed: Boolean,
        val correlation: Double,
        val pairs: Map<String, Double>,
    )
}
