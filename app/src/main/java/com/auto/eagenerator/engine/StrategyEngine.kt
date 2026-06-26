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
        val tf = if (isMQL5) "PERIOD_CURRENT" else "0"
        val tfArg = if (isMQL5) "PERIOD_CURRENT," else "0,"

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
                IndicatorType.MA -> {
                    val code = if (isMQL5)
                        "iMA(_Symbol,PERIOD_CURRENT,${e.fastPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_${e.appliedPrice.uppercase()})"
                    else
                        "iMA(_Symbol,0,${e.fastPeriod},0,MODE_${e.maMethod.uppercase()},PRICE_${e.appliedPrice.uppercase()})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.RSI -> {
                    val code = if (isMQL5)
                        "iRSI(_Symbol,PERIOD_CURRENT,${e.period},PRICE_${e.appliedPrice.uppercase()})"
                    else
                        "iRSI(_Symbol,0,${e.period},PRICE_${e.appliedPrice.uppercase()})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.STOCH -> {
                    val code = if (isMQL5)
                        "iStochastic(_Symbol,PERIOD_CURRENT,${e.kPeriod},${e.dPeriod},${e.slowing},MODE_SMA,STO_LOWHIGH)"
                    else
                        "iStochastic(_Symbol,0,${e.kPeriod},${e.dPeriod},${e.slowing},MODE_SMA,0,STO_LOWHIGH)"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.MACD -> {
                    val code = if (isMQL5)
                        "iMACD(_Symbol,PERIOD_CURRENT,${e.fastPeriod},${e.slowPeriod},${e.period},PRICE_CLOSE)"
                    else
                        "iMACD(_Symbol,0,${e.fastPeriod},${e.slowPeriod},${e.period},PRICE_CLOSE)"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.BOLLINGER -> {
                    val code = if (isMQL5)
                        "iBands(_Symbol,PERIOD_CURRENT,${e.bbPeriod},0,${e.bbDeviation},PRICE_CLOSE)"
                    else
                        "iBands(_Symbol,0,${e.bbPeriod},${e.bbDeviation},0,PRICE_CLOSE)"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.ADX -> {
                    val code = if (isMQL5)
                        "iADX(_Symbol,PERIOD_CURRENT,${e.period})"
                    else
                        "iADX(_Symbol,0,${e.period},PRICE_CLOSE,MODE_MAIN)"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.SAR -> {
                    val code = if (isMQL5)
                        "iSAR(_Symbol,PERIOD_CURRENT,${e.sarStep},${e.sarMax})"
                    else
                        "iSAR(_Symbol,0,${e.sarStep},${e.sarMax})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.CCI -> {
                    val code = if (isMQL5)
                        "iCCI(_Symbol,PERIOD_CURRENT,${e.period},PRICE_${e.appliedPrice.uppercase()})"
                    else
                        "iCCI(_Symbol,0,${e.period},PRICE_${e.appliedPrice.uppercase()})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.ICHIMOKU -> {
                    val code = if (isMQL5)
                        "iIchimoku(_Symbol,PERIOD_CURRENT,${e.tenkan},${e.kijun},${e.senkou})"
                    else
                        "iIchimoku(_Symbol,0,${e.tenkan},${e.kijun},${e.senkou})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.ALLIGATOR -> {
                    val code = if (isMQL5)
                        "iAlligator(_Symbol,PERIOD_CURRENT,${e.jawPeriod},0,${e.teethPeriod},0,${e.lipsPeriod},0,MODE_SMMA,PRICE_MEDIAN)"
                    else
                        "iAlligator(_Symbol,0,${e.jawPeriod},${e.teethPeriod},${e.lipsPeriod},0,0,0,MODE_SMMA,PRICE_MEDIAN)"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.ATR -> {
                    val code = if (isMQL5)
                        "iATR(_Symbol,PERIOD_CURRENT,${e.atrPeriod})"
                    else
                        "iATR(_Symbol,0,${e.atrPeriod})"
                    refs += IndicatorRef("e${e.id}_h", code, "IndicatorRelease(e${e.id}_h);")
                }
                IndicatorType.CANDLE_PATTERN, IndicatorType.PRICE, IndicatorType.CUSTOM_EXPRESSION -> {
                    // No handle needed
                }
                IndicatorType.VOLUME -> {
                    val vcode = if (isMQL5) "iVolumes(_Symbol,PERIOD_CURRENT,VOLUME_TICK)" else "iVolumes(_Symbol,0,VOLUME_TICK)"
                    refs += IndicatorRef("e${e.id}_h", vcode, "IndicatorRelease(e${e.id}_h);")
                }
            }

            // Target indicator (if another indicator is the comparison target)
            if (e.targetType == TargetType.INDICATOR) {
                val ti = e.targetIndicator
                if (ti != IndicatorType.PRICE && ti != IndicatorType.CANDLE_PATTERN
                    && ti != IndicatorType.CUSTOM_EXPRESSION && ti != IndicatorType.ICUSTOM) {
                    val tName = "e${e.id}_tgt"
                    val tInit = when (ti) {
                        IndicatorType.MA -> if (isMQL5) "iMA(_Symbol,PERIOD_CURRENT,14,0,MODE_SMA,PRICE_CLOSE)" else "iMA(_Symbol,0,14,0,MODE_SMA,PRICE_CLOSE)"
                        IndicatorType.RSI -> if (isMQL5) "iRSI(_Symbol,PERIOD_CURRENT,14,PRICE_CLOSE)" else "iRSI(_Symbol,0,14,PRICE_CLOSE)"
                        IndicatorType.STOCH -> if (isMQL5) "iStochastic(_Symbol,PERIOD_CURRENT,5,3,3,MODE_SMA,STO_LOWHIGH)" else "iStochastic(_Symbol,0,5,3,3,MODE_SMA,0,STO_LOWHIGH)"
                        IndicatorType.MACD -> if (isMQL5) "iMACD(_Symbol,PERIOD_CURRENT,12,26,9,PRICE_CLOSE)" else "iMACD(_Symbol,0,12,26,9,PRICE_CLOSE)"
                        IndicatorType.BOLLINGER -> if (isMQL5) "iBands(_Symbol,PERIOD_CURRENT,20,0,2.0,PRICE_CLOSE)" else "iBands(_Symbol,0,20,2.0,0,PRICE_CLOSE)"
                        IndicatorType.ADX -> if (isMQL5) "iADX(_Symbol,PERIOD_CURRENT,14)" else "iADX(_Symbol,0,14,PRICE_CLOSE,MODE_MAIN)"
                        IndicatorType.SAR -> if (isMQL5) "iSAR(_Symbol,PERIOD_CURRENT,0.02,0.2)" else "iSAR(_Symbol,0,0.02,0.2)"
                        IndicatorType.CCI -> if (isMQL5) "iCCI(_Symbol,PERIOD_CURRENT,14,PRICE_TYPICAL)" else "iCCI(_Symbol,0,14,PRICE_TYPICAL)"
                        IndicatorType.ICHIMOKU -> if (isMQL5) "iIchimoku(_Symbol,PERIOD_CURRENT,9,26,52)" else "iIchimoku(_Symbol,0,9,26,52)"
                        IndicatorType.ALLIGATOR -> if (isMQL5) "iAlligator(_Symbol,PERIOD_CURRENT,13,0,8,0,5,0,MODE_SMMA,PRICE_MEDIAN)" else "iAlligator(_Symbol,0,13,8,5,0,0,0,MODE_SMMA,PRICE_MEDIAN)"
                        IndicatorType.ATR -> if (isMQL5) "iATR(_Symbol,PERIOD_CURRENT,14)" else "iATR(_Symbol,0,14)"
                        IndicatorType.VOLUME -> if (isMQL5) "iVolumes(_Symbol,PERIOD_CURRENT,VOLUME_TICK)" else "iVolumes(_Symbol,0,VOLUME_TICK)"
                        else -> null
                    }
                    if (tInit != null) refs += IndicatorRef(tName, tInit, "IndicatorRelease($tName);")
                }
            }
        }
        return refs.distinctBy { it.name }
    }
}
