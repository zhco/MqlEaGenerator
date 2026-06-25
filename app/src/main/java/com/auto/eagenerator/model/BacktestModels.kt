package com.auto.eagenerator.model

// ─── 回测/优化相关模型 ───

data class Candle(val date: String, val open: Double, val high: Double, val low: Double, val close: Double, val volume: Long = 0)

data class BTTrade(
    val entryBar: Int, val entryTime: String, val entryPrice: Double,
    val exitBar: Int, val exitTime: String, val exitPrice: Double,
    val dir: Int, val profit: Double, val profitPct: Double
)

data class BTReport(
    val strategyName: String, val startDate: String, val endDate: String,
    val totalBars: Int, val totalTrades: Int, val wins: Int, val losses: Int,
    val winRate: Double, val grossProfit: Double, val grossLoss: Double,
    val netProfit: Double, val profitFactor: Double,
    val maxDrawdown: Double, val maxDrawdownPct: Double,
    val avgWin: Double, val avgLoss: Double, val expect: Double,
    val sharpeRatio: Double, val equityCurve: List<Double>,
    val trades: List<BTTrade>
)

// ─── 参数优化模型 ───

data class ParamRange(val name: String, val min: Double, val max: Double, val step: Double)

data class OptResult(
    val params: Map<String, Double>, val netProfit: Double,
    val winRate: Double, val sharpe: Double, val maxDD: Double, val trades: Int
)

data class OptReport(
    val strategyName: String = "", val paramRanges: List<ParamRange> = emptyList(),
    val totalRuns: Int = 0, val elapsedMs: Long = 0,
    val best: OptResult? = null, val top10: List<OptResult> = emptyList(),