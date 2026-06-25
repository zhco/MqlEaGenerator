package com.auto.eagenerator.model

// ═══════════════════════════════════════════
// v6 - 遗传算法
// ═══════════════════════════════════════════
data class ParamGene(
    val name: String,
    val min: Double,
    val max: Double,
    val step: Double = 1.0,
    val isInt: Boolean = true,
)

data class GAConfig(
    val populationSize: Int = 50,
    val generations: Int = 30,
    val mutationRate: Double = 0.1,
    val crossoverRate: Double = 0.8,
    val eliteCount: Int = 5,
    val targetMetric: String = "sharpe",  // sharpe/profit/drawdown/winrate
)

data class GAIndividual(
    val genes: List<Double>,
    val fitness: Double = 0.0,
    val metrics: Map<String, Double> = emptyMap(),
)

data class GAResult(
    val bestIndividual: GAIndividual,
    val generationHistory: List<Double> = emptyList(), // 每代最优fitness
    val convergenceGen: Int = 0,
    val elapsedMs: Long = 0,
)

// ═══════════════════════════════════════════
// v6 - 蒙特卡洛
// ═══════════════════════════════════════════
data class MCConfig(
    val simulations: Int = 200,
    val shuffleType: String = "trades",  // trades/returns/prices
    val confidenceLevel: Double = 0.95,
)

data class MCResult(
    val originalSharpe: Double = 0.0,
    val meanSharpe: Double = 0.0,
    val stdSharpe: Double = 0.0,
    val minSharpe: Double = 0.0,
    val maxSharpe: Double = 0.0,
    val confidenceInterval: Pair<Double, Double> = 0.0 to 0.0,
    val robustnessScore: Double = 0.0,  // 0~1，越高越稳健
    val distribution: List<Double> = emptyList(), // 模拟夏普分布
)

// ═══════════════════════════════════════════
// v6 - 前进式优化
// ═══════════════════════════════════════════
data class WFConfig(
    val inSampleBars: Int = 1000,   // 样本内K线数
    val outSampleBars: Int = 500,   // 样本外K线数
    val windows: Int = 4,           // 窗口数
    val stepBars: Int = 500,        // 步进
)

data class WFWindow(
    val index: Int,
    val inStart: Int, val inEnd: Int,
    val outStart: Int, val outEnd: Int,
    val inSampleSharpe: Double = 0.0,
    val outSampleSharpe: Double = 0.0,
    val inSampleProfit: Double = 0.0,
    val outSampleProfit: Double = 0.0,
    val params: Map<String, Double> = emptyMap(),
)

data class WFResult(
    val windows: List<WFWindow> = emptyList(),
    val avgInSharpe: Double = 0.0,
    val avgOutSharpe: Double = 0.0,
    val stability: Double = 0.0,     // 参数稳定性 0~1
    val robustness: Double = 0.0,    // 样本外稳健性 0~1
    val recommendedParams: Map<String, Double> = emptyMap(),
)

// ═══════════════════════════════════════════
// v6 - 策略对比
// ═══════════════════════════════════════════
data class CompareEntry(
    val name: String,
    val netProfit: Double = 0.0,
    val totalTrades: Int = 0,
    val winRate: Double = 0.0,
    val profitFactor: Double = 0.0,
    val sharpeRatio: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val avgTrade: Double = 0.0,
    val recoveryFactor: Double = 0.0,
    val equityCurve: List<Double> = emptyList(),
)
