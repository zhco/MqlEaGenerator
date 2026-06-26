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
    // v6 fields
    val bestParams: Map<String, Double> = emptyMap(),
    val bestFitness: Double = 0.0,
    val target: String = "Sharpe",
    val results: List<OptResult> = emptyList(),
    val generationCount: Int = 0,
)

// ─── v6: 遗传算法模型 ───
data class GAConfig(
    val populationSize: Int = 50, val generations: Int = 20,
    val eliteCount: Int = 3,
    val crossoverRate: Double = 0.7, val mutationRate: Double = 0.1,
    val earlyStopGen: Int = 5, val targetMetric: String = "Sharpe",
)
data class ParamGene(
    val name: String, val min: Double, val max: Double, val step: Double,
    val isInt: Boolean = false,
)
data class GAIndividual(
    val genes: List<Double>, val fitness: Double = 0.0,
)
data class GAResult(
    val bestIndividual: GAIndividual,
    val generationHistory: List<Double> = emptyList(),
    val convergenceGen: Int = 0,
    val elapsedMs: Long = 0,
)

// ─── v6: 蒙特卡洛模型 ───
data class MCConfig(
    val simulations: Int = 200, val shuffleType: String = "returns",
    val confidenceLevel: Double = 95.0,
)
data class MCResult(
    val originalSharpe: Double = 0.0, val meanSharpe: Double = 0.0,
    val stdSharpe: Double = 0.0,
    val minSharpe: Double = 0.0, val maxSharpe: Double = 0.0,
    val confidenceInterval: Pair<Double, Double> = 0.0 to 0.0,
    val robustnessScore: Double = 0.0,
    val distribution: List<Double> = emptyList(),
)

// ─── v6: 前进式优化模型 ───
data class WFConfig(
    val windows: Int = 5, val inSampleBars: Int = 1000, val outSampleBars: Int = 500,
)
data class WFWindow(
    val index: Int = 0,
    val inStart: Int = 0, val inEnd: Int = 0,
    val outStart: Int = 0, val outEnd: Int = 0,
    val inSampleSharpe: Double = 0.0, val outSampleSharpe: Double = 0.0,
    val inSampleProfit: Double = 0.0, val outSampleProfit: Double = 0.0,
    val params: Map<String, Double> = emptyMap(),
)
data class WFResult(
    val windows: List<WFWindow> = emptyList(),
    val avgInSharpe: Double = 0.0, val avgOutSharpe: Double = 0.0,
    val stability: Double = 0.0, val robustness: Double = 0.0,
    val recommendedParams: Map<String, Double> = emptyMap(),
)

// ─── v6: 策略对比模型 ───
data class CompareEntry(
    val name: String = "",
    val netProfit: Double = 0.0, val winRate: Double = 0.0,
    val profitFactor: Double = 0.0, val sharpeRatio: Double = 0.0,
    val maxDrawdownPct: Double = 0.0, val recoveryFactor: Double = 0.0,
)
