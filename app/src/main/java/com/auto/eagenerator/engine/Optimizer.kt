package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlin.system.measureTimeMillis

object Optimizer {

    // ─── 生成参数网格 ───
    private fun generateGrid(ranges: List<ParamRange>): List<Map<String, Double>> {
        if (ranges.isEmpty()) return listOf(emptyMap())
        val grids = ranges.map { r ->
            generateSequence(r.min) { it + r.step }.takeWhile { it <= r.max + 1e-9 }.toList()
        }
        return cartesianProduct(grids).map { combo ->
            ranges.indices.associate { ranges[it].name to combo[it] }
        }
    }

    private fun cartesianProduct(lists: List<List<Double>>): List<List<Double>> {
        if (lists.isEmpty()) return listOf(emptyList())
        return lists.drop(1).fold(lists.first().map { listOf(it) }) { acc, list ->
            acc.flatMap { a -> list.map { b -> a + b } }
        }
    }

    // ─── 应用参数到配置 ───
    private fun applyParams(config: StrategyConfig, params: Map<String, Double>): StrategyConfig {
        return config.copy(
            entries = config.entries.map { e ->
                e.copy(
                    fastPeriod = params["${e.indicator.name}_fastPeriod"]?.toInt() ?: e.fastPeriod,
                    slowPeriod = params["${e.indicator.name}_slowPeriod"]?.toInt() ?: e.slowPeriod,
                    period = params["${e.indicator.name}_period"]?.toInt() ?: e.period,
                    obLevel = params["${e.indicator.name}_obLevel"] ?: e.obLevel,
                    osLevel = params["${e.indicator.name}_osLevel"] ?: e.osLevel,
                    adxLevel = params["${e.indicator.name}_adxLevel"] ?: e.adxLevel,
                )
            },
            exits = config.exits.map { ex ->
                ex.copy(
                    slPoints = params["slPoints"]?.toInt() ?: ex.slPoints,
                    tpPoints = params["tpPoints"]?.toInt() ?: ex.tpPoints,
                    trailingStart = params["trailingStart"]?.toInt() ?: ex.trailingStart,
                    trailingStep = params["trailingStep"]?.toInt() ?: ex.trailingStep,
                )
            }
        )
    }

    // ─── 运行优化 ───
    fun run(csv: String, config: StrategyConfig, ranges: List<ParamRange>): OptReport {
        val grid = generateGrid(ranges)
        val results = mutableListOf<OptResult>()
        val elapsed = measureTimeMillis {
            for ((idx, params) in grid.withIndex()) {
                val modCfg = applyParams(config, params)
                val report = BacktestEngine.run(csv, modCfg)
                results += OptResult(params, report.netProfit, report.winRate, report.sharpeRatio, report.maxDrawdownPct, report.totalTrades)
            }
        }
        val sorted = results.sortedByDescending { it.sharpe } // 以夏普比率为优化目标
        val best = sorted.firstOrNull() ?: OptResult(emptyMap(), 0.0, 0.0, 0.0, 0.0, 0)
        return OptReport(config.strategyName, ranges, grid.size, elapsed, best, sorted.take(10))
    }
}
