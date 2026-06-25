package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 蒙特卡洛稳健性分析
 * 通过随机重排交易序列，评估策略对交易顺序的敏感性
 */
object MonteCarloAnalyzer {

    private val rng = Random(System.currentTimeMillis())

    fun analyze(
        config: MCConfig,
        trades: List<BacktestTrade>,
        originalMetrics: Map<String, Double>,
    ): MCResult {
        val originalSharpe = originalMetrics["sharpe"] ?: 0.0
        val simulations = config.simulations
        val distributions = mutableListOf<Double>()

        // 预计算交易收益率
        val tradeReturns = trades.map { it.profit / it.risk.coerceAtLeast(1.0) }

        repeat(simulations) {
            val shuffled = when (config.shuffleType) {
                "returns" -> tradeReturns.shuffled(rng)
                else -> tradeReturns.shuffled(rng) // 默认重排收益率
            }
            val sharpe = computeSharpe(shuffled)
            distributions.add(sharpe)
        }

        val mean = distributions.average()
        val std = distributions.sorted().let { sorted ->
            val v = sorted.map { (it - mean).pow(2) }.average()
            sqrt(v)
        }

        val sortedDist = distributions.sorted()
        val alpha = (1.0 - config.confidenceLevel) / 2.0
        val lowerIdx = (alpha * simulations).toInt().coerceIn(0, simulations - 1)
        val upperIdx = ((1.0 - alpha) * simulations).toInt().coerceIn(0, simulations - 1)
        val ci = sortedDist[lowerIdx] to sortedDist[upperIdx]

        // 稳健性评分：原始夏普在模拟分布中的分位数
        val robustnessScore = sortedDist.count { it <= originalSharpe }.toDouble() / simulations

        return MCResult(
            originalSharpe = originalSharpe,
            meanSharpe = mean,
            stdSharpe = std,
            minSharpe = sortedDist.first(),
            maxSharpe = sortedDist.last(),
            confidenceInterval = ci,
            robustnessScore = robustnessScore,
            distribution = distributions,
        )
    }

    private fun computeSharpe(returns: List<Double>): Double {
        if (returns.size < 2) return 0.0
        val mean = returns.average()
        val variance = returns.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance.coerceAtLeast(1e-10))
        return (mean / stdDev) * sqrt(252.0) // 年化
    }

    data class BacktestTrade(
        val profit: Double,
        val risk: Double,
        val timestamp: Long = 0,
    )
}
