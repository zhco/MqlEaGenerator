package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

/**
 * 前进式优化引擎
 * 滑动窗口：样本内优化 → 样本外验证 → 合并评估参数稳定性
 */
object WalkForwardOptimizer {

    fun optimize(
        config: WFConfig,
        ohlc: List<DoubleArray>, // [open, high, low, close] per bar
        paramGenes: List<ParamGene>,
        fitnessFn: (List<Double>, Int, Int) -> Double, // (params, startBar, endBar) -> metric
    ): WFResult {
        val totalBars = ohlc.size
        val windows = mutableListOf<WFWindow>()
        val stepBars = (totalBars - config.inSampleBars - config.outSampleBars) / maxOf(config.windows - 1, 1)

        for (i in 0 until config.windows) {
            val inStart = i * stepBars
            val inEnd = inStart + config.inSampleBars
            val outStart = inEnd
            val outEnd = minOf(outStart + config.outSampleBars, totalBars)

            if (outEnd > totalBars || inEnd > totalBars) break

            // 样本内优化（简化版网格搜索）
            var bestParams = paramGenes.map { pg -> (pg.min + pg.max) / 2.0 }
            var bestInSharpe = Double.NEGATIVE_INFINITY

            // 对每个参数做粗网格搜索
            val candidates = generateGridCandidates(paramGenes, maxCombos = 200)
            for (cand in candidates) {
                val sharpe = fitnessFn(cand, inStart, inEnd)
                if (sharpe > bestInSharpe) {
                    bestInSharpe = sharpe
                    bestParams = cand
                }
            }

            // 样本外验证
            val outSharpe = fitnessFn(bestParams, outStart, outEnd)

            val paramMap = paramGenes.mapIndexed { idx, pg -> pg.name to bestParams[idx] }.toMap()

            windows.add(
                WFWindow(
                    index = i + 1,
                    inStart = inStart, inEnd = inEnd,
                    outStart = outStart, outEnd = outEnd,
                    inSampleSharpe = bestInSharpe,
                    outSampleSharpe = outSharpe,
                    inSampleProfit = bestInSharpe * 100,
                    outSampleProfit = outSharpe * 100,
                    params = paramMap,
                )
            )
        }

        val avgIn = windows.map { it.inSampleSharpe }.average()
        val avgOut = windows.map { it.outSampleSharpe }.average()

        // 稳定性 = 各窗口参数方差倒数归一化
        val stability = computeParamStability(windows, paramGenes)
        // 稳健性 = 样本外夏普 / 样本内夏普
        val robustness = if (avgIn > 1e-9) avgOut / avgIn else 0.0

        // 推荐参数：样本外表现最好的窗口参数
        val bestWindow = windows.maxByOrNull { it.outSampleSharpe }
        val recommendedParams = bestWindow?.params ?: emptyMap()

        return WFResult(
            windows = windows,
            avgInSharpe = avgIn,
            avgOutSharpe = avgOut,
            stability = stability,
            robustness = robustness,
            recommendedParams = recommendedParams,
        )
    }

    private fun generateGridCandidates(
        genes: List<ParamGene>, maxCombos: Int
    ): List<List<Double>> {
        val steps = mutableListOf<List<Double>>()
        var totalCombos = 1

        for (pg in genes) {
            val n = 5.coerceAtMost(
                ((pg.max - pg.min) / pg.step.coerceAtLeast(1.0)).toInt().coerceAtLeast(2)
            )
            val vals = (0 until n).map { pg.min + it * (pg.max - pg.min) / (n - 1) }
            steps.add(vals)
            totalCombos *= vals.size
            if (totalCombos > maxCombos * 10) break
        }

        if (totalCombos <= maxCombos) {
            return cartesianProduct(steps)
        }

        // 随机采样
        val result = mutableListOf<List<Double>>()
        val rng = kotlin.random.Random(42)
        repeat(maxCombos) {
            result.add(steps.map { it.random(rng) })
        }
        return result
    }

    private fun cartesianProduct(lists: List<List<Double>>): List<List<Double>> {
        var result = listOf(emptyList<Double>())
        for (list in lists) {
            result = result.flatMap { combo -> list.map { value -> combo + value } }
        }
        return result
    }

    private fun computeParamStability(windows: List<WFWindow>, genes: List<ParamGene>): Double {
        if (windows.isEmpty() || genes.isEmpty()) return 0.0
        var totalStability = 0.0
        for (pg in genes) {
            val vals = windows.mapNotNull { it.params[pg.name] }
            if (vals.size < 2) continue
            val range = pg.max - pg.min
            if (range < 1e-9) { totalStability += 1.0; continue }
            val mean = vals.average()
            val variance = vals.map { (it - mean).pow(2) }.average()
            val cv = kotlin.math.sqrt(variance) / range.coerceAtLeast(1e-9)
            totalStability += (1.0 - cv).coerceIn(0.0, 1.0)
        }
        return (totalStability / genes.size).coerceIn(0.0, 1.0)
    }

    private fun Double.pow(n: Int): Double {
        var r = 1.0
        repeat(n) { r *= this }
        return r
    }
}
