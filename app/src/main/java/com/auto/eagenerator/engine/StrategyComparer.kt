package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlin.math.*

/**
 * 策略对比引擎 - 多策略横向比较
 */
object StrategyComparer {

    fun compare(entries: List<CompareEntry>): ComparisonReport {
        if (entries.isEmpty()) return ComparisonReport()

        val metrics = listOf("netProfit", "winRate", "profitFactor", "sharpeRatio", "maxDrawdownPct", "recoveryFactor")
        val rankings = mutableMapOf<String, List<RankItem>>()

        for (metric in metrics) {
            val sorted = when (metric) {
                "maxDrawdownPct" -> entries.sortedBy { getMetric(it, metric) } // 回撤越小越好
                else -> entries.sortedByDescending { getMetric(it, metric) }
            }
            rankings[metric] = sorted.mapIndexed { idx, e ->
                RankItem(e.name, getMetric(e, metric), idx + 1)
            }
        }

        // 综合排名 = 各指标排名均值
        val overallRank = entries.map { entry ->
            val totalRank = metrics.sumOf { m ->
                rankings[m]?.firstOrNull { it.name == entry.name }?.rank ?: entries.size
            }
            OverallRankItem(entry.name, totalRank.toDouble() / metrics.size)
        }.sortedBy { it.avgRank }

        return ComparisonReport(
            entries = entries,
            rankings = rankings,
            overallRank = overallRank,
        )
    }

    private fun getMetric(e: CompareEntry, metric: String): Double = when (metric) {
        "netProfit" -> e.netProfit
        "winRate" -> e.winRate
        "profitFactor" -> e.profitFactor
        "sharpeRatio" -> e.sharpeRatio
        "maxDrawdownPct" -> e.maxDrawdownPct
        "recoveryFactor" -> e.recoveryFactor
        else -> 0.0
    }

    data class RankItem(val name: String, val value: Double, val rank: Int)
    data class OverallRankItem(val name: String, val avgRank: Double)

    data class ComparisonReport(
        val entries: List<CompareEntry> = emptyList(),
        val rankings: Map<String, List<RankItem>> = emptyMap(),
        val overallRank: List<OverallRankItem> = emptyList(),
    )
}
