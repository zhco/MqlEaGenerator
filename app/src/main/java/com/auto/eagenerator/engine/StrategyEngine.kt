package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

data class IndicatorRef(
    val name: String,
    val initCode: String,
    val releaseCode: String = ""
)

object StrategyEngine {

    fun analyzeIndicators(config: StrategyConfig, isMQL5: Boolean): List<IndicatorRef> {
        val refs = mutableListOf<IndicatorRef>()
        config.entries.forEach { e ->
            if (e.timeframe != "CURRENT") return@forEach
            when (e.indicator) {
                IndicatorType.ICUSTOM -> {
                    val params = e.customIndiParams.ifBlank { "" }
                    val init = if (isMQL5)
                        "iCustom(_Symbol, PERIOD_CURRENT, \"${e.customIndiName}\"${if (params.isNotBlank()) ", $params" else ""})"
                    else
                        "iCustom(_Symbol, 0, \"${e.customIndiName}\"${if (params.isNotBlank()) ", $params" else ""})"
                    refs += IndicatorRef("e${e.id}_ic", init)
                }
                IndicatorType.MA_CROSS, IndicatorType.MA_TREND, IndicatorType.MA_PRICE -> {
                    refs += IndicatorRef(
                        "e${e.id}_f",
                        if (isMQL5) "iMA(_Symbol,PERIOD_CURRENT,${e.fastPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                        else "iMA(_Symbol,0,${e.fastPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                    )
                    refs += IndicatorRef(
                        "e${e.id}_s",
                        if (isMQL5) "iMA(_Symbol,PERIOD_CURRENT,${e.slowPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                        else "iMA(_Symbol,0,${e.slowPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                    )
                    if (e.indicator == IndicatorType.MA_TREND) {
                        refs += IndicatorRef(
                            "e${e.id}_m",
                            if (isMQL5) "iMA(_Symbol,PERIOD_CURRENT,${e.midPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                            else "iMA(_Symbol,0,${e.midPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_CLOSE)"
                        )
                    }
                }
                IndicatorType.RSI, IndicatorType.RSI_DIVERGENCE -> {
                    refs += IndicatorRef(
                        "e${e.id}_rsi",
                        if (isMQL5) "iRSI(_Symbol,PERIOD_CURRENT,${e.period},PRICE_CLOSE)"
                        else "iRSI(_Symbol,0,${e.period},PRICE_CLOSE)"
                    )
                }
                IndicatorType.STOCH -> {
                    refs += IndicatorRef(
                        "e${e.id}_st",
                        if (isMQL5) "iStochastic(_Symbol,PERIOD_CURRENT,${e.kPeriod},${e.dPeriod},${e.slowing},MODE_SMA,STO_LOWHIGH)"
                        else "iStochastic(_Symbol,0,${e.kPeriod},${e.dPeriod},${e.slowing},MODE_SMA,0,STO_LOWHIGH)"
                    )
                }
                IndicatorType.MACD, IndicatorType.MACD_DIVERGENCE -> {
                    refs += IndicatorRef(
                        "e${e.id}_macd",
                        if (isMQL5) "iMACD(_Symbol,PERIOD_CURRENT,${e.fastPeriod},${e.slowPeriod},${e.period},PRICE_CLOSE)"
                        else "iMACD(_Symbol,0,${e.fastPeriod},${e.slowPeriod},${e.period},PRICE_CLOSE)"
                    )
                }
                IndicatorType.BOLLINGER -> {
                    refs += IndicatorRef(
                        "e${e.id}_bb",
                        if (isMQL5) "iBands(_Symbol,PERIOD_CURRENT,${e.bbPeriod},0,${e.bbDeviation},PRICE_CLOSE)"
                        else "iBands(_Symbol,0,${e.bbPeriod},${e.bbDeviation},0,PRICE_CLOSE)"
                    )
                }
                IndicatorType.ADX -> {
                    refs += IndicatorRef(
                        "e${e.id}_adx",
                        if (isMQL5) "iADX(_Symbol,PERIOD_CURRENT,${e.period})"
                        else "iADX(_Symbol,0,${e.period},PRICE_CLOSE,MODE_MAIN)"
                    )
                }
                IndicatorType.CCI -> {
                    refs += IndicatorRef(
                        "e${e.id}_cci",
                        if (isMQL5) "iCCI(_Symbol,PERIOD_CURRENT,${e.period},PRICE_TYPICAL)"
                        else "iCCI(_Symbol,0,${e.period},PRICE_TYPICAL)"
                    )
                }
                IndicatorType.ATR -> {
                    refs += IndicatorRef(
                        "e${e.id}_atr",
                        if (isMQL5) "iATR(_Symbol,PERIOD_CURRENT,${e.atrPeriod})"
                        else "iATR(_Symbol,0,${e.atrPeriod})"
                    )
                }
                else -> {}
            }
        }
        return refs.distinctBy { it.name }
    }
}
