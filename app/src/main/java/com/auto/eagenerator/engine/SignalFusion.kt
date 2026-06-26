package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

/**
 * AI信号融合引擎 - 多指标加权打分
 */
object SignalFusionEngine {

    fun evaluate(config: StrategyConfig, signals: Map<IndicatorType, Double>): FusionResult {
        val fusion = config.signalFusion
        if (!fusion.enabled) {
            return FusionResult(score = 0.0, signals = signals.mapKeys { it.key.label }, action = "skip")
        }

        var totalScore = 0.0
        var totalWeight = 0.0
        val scored = mutableMapOf<String, Double>()

        for (sw in fusion.weights) {
            val raw = signals[sw.indicator] ?: 0.0
            if (kotlin.math.abs(raw) >= sw.minStrength) {
                val normalized = if (fusion.useNormalization) {
                    kotlin.math.tanh(raw) // tanh归一化到(-1,1)
                } else {
                    raw.coerceIn(-1.0, 1.0)
                }
                totalScore += normalized * sw.weight
                totalWeight += kotlin.math.abs(sw.weight)
                scored[sw.indicator.label] = normalized * sw.weight
            }
        }

        val score = if (totalWeight > 0) totalScore / totalWeight else 0.0

        val action = when {
            score >= fusion.buyThreshold -> "buy"
            score <= -fusion.sellThreshold -> "sell"
            else -> "hold"
        }

        return FusionResult(score = score, action = action, signals = scored)
    }

    data class FusionResult(
        val score: Double,
        val action: String,
        val signals: Map<String, Double> = emptyMap(),
    )
}

