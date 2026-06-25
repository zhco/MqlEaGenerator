package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

object Mql5Generator {

    fun tfConst(tf: String) = when (tf) {
        "M1" -> "PERIOD_M1"; "M5" -> "PERIOD_M5"; "M15" -> "PERIOD_M15"
        "M30" -> "PERIOD_M30"; "H1" -> "PERIOD_H1"; "H4" -> "PERIOD_H4"
        "D1" -> "PERIOD_D1"; "W1" -> "PERIOD_W1"; "MN" -> "PERIOD_MN"
        else -> "PERIOD_CURRENT"
    }

    fun generate(config: StrategyConfig): String = buildString {
        appendHeader(config)
        appendInputs(config)
        appendGlobals(config)
        appendOnInit(config)
        appendOnDeinit(config)
        appendOnTick(config)
        appendHelpers(config)
        appendTradeFuncs(config)
        appendMoneyMgmt(config)
        appendSignalFunc(config)
        appendExitFunc(config)
        appendPanel(config)
        appendStats(config)
        appendFooter()
    }

    private fun StringBuilder.appendHeader(c: StrategyConfig) {
        append("""//+------------------------------------------------------------------+
//|                                        ${c.strategyName}.mq5       |
//|                                        MQL5 Expert Advisor v3      |
//|                                        Multi-Symbol Professional   |
//+------------------------------------------------------------------+
#property copyright   "${c.strategyName}"
#property version     "3.0"
#property description "专业级多币种策略 - ${c.entries.size}入场 + ${c.exits.size}出场"
""")
        if (c.multiSymbol) append("// 多币种模式: ${c.symbols.joinToString(", ")}\n")
        if (c.enableHedging) append("// 需要对冲账户\n")
        if (c.newsFilter.enabled) append("#property indicator_chart_window\n#include <Trade\\Trade.mqh>\n")
        append("\n")
    }

    private fun StringBuilder.appendInputs(c: StrategyConfig) {
        append("//──── 输入参数 ────\n")
        append("input group \"═══ 基础 ═══\"\n")
        append("input ulong InpMagic = ${c.magicNumber};  // 魔术号\n")
        append("input ulong InpDeviation = ${c.slippage}; // 滑点\n")
        if (c.enableHedging) append("input bool InpHedging = true; // 对冲模式\n")
        if (c.multiSymbol) {
            append("input bool InpMultiSym = true; // 多币种模式\n")
            append("input string InpSymbols = \"${c.symbols.joinToString(",")}\"; // 币种列表\n")
        }
        append("\n")

        // Entry inputs
        c.entries.forEachIndexed { i, e ->
            val n = i + 1
            append("input group \"═══ 入场#$n: ${e.indicator.label} ═══\"\n")
            append("input ENUM_ORDER_TYPE InpE${n}_Type = ORDER_TYPE_BUY; // 入场方向\n")
            append("input ENUM_ORDER_TYPE InpE${n}_Order = ORDER_TYPE_BUY; // 订单类型(市价/限价/突破)\n")
            if (e.entryType == EntryOrderType.LIMIT) append("input int InpE${n}_LmtOff = ${e.limitOffset}; // 限价偏移(点)\n")
            if (e.entryType == EntryOrderType.STOP) append("input int InpE${n}_StpOff = ${e.stopOffset}; // 突破偏移(点)\n")
            if (e.timeframe != "CURRENT" && e.indicator != IndicatorType.CUSTOM_EXPRESSION)
                append("input ENUM_TIMEFRAMES InpE${n}_TF = ${tfConst(e.timeframe)}; // 时间框架\n")

            when (e.indicator) {
                IndicatorType.ICUSTOM -> {
                    append("input string InpE${n}_Indi = \"${e.customIndiName}\"; // 指标名称\n")
                    append("input string InpE${n}_Params = \"${e.customIndiParams}\"; // 指标参数(逗号分隔)\n")
                    append("input int InpE${n}_Buf = ${e.customIndiBuffer}; // Buffer索引\n")
                    append("input double InpE${n}_SigVal = ${e.customIndiSignal}; // 信号阈值\n")
                }
                IndicatorType.CUSTOM_EXPRESSION -> append("input string InpE${n}_Expr = \"\"; // MQL条件表达式\n")
                IndicatorType.MA_CROSS, IndicatorType.MA_TREND, IndicatorType.MA_PRICE -> {
                    append("input int InpE${n}_F = ${e.fastPeriod}; input int InpE${n}_S = ${e.slowPeriod};\n")
                    if (e.indicator == IndicatorType.MA_TREND) append("input int InpE${n}_M = ${e.midPeriod};\n")
                    append("input ENUM_MA_METHOD InpE${n}_MAM = MODE_${e.maMethod.uppercase()};\n")
                }
                IndicatorType.RSI -> append("input int InpE${n}_P = ${e.period}; input double InpE${n}_OB = ${e.obLevel}; input double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.RSI_DIVERGENCE -> append("input int InpE${n}_P = ${e.period}; input int InpE${n}_DivLB = ${e.divLookback}; input int InpE${n}_DivMin = ${e.divMinStrength};\n")
                IndicatorType.STOCH -> append("input int InpE${n}_K = ${e.kPeriod}; input int InpE${n}_D = ${e.dPeriod}; input int InpE${n}_Sl = ${e.slowing}; input double InpE${n}_OB = ${e.obLevel}; input double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.MACD -> append("input int InpE${n}_F = ${e.fastPeriod}; input int InpE${n}_S = ${e.slowPeriod}; input int InpE${n}_Sig = ${e.period};\n")
                IndicatorType.MACD_DIVERGENCE -> append("input int InpE${n}_F = ${e.fastPeriod}; input int InpE${n}_S = ${e.slowPeriod}; input int InpE${n}_Sig = ${e.period}; input int InpE${n}_DivLB = ${e.divLookback}; input int InpE${n}_DivMin = ${e.divMinStrength};\n")
                IndicatorType.BOLLINGER -> append("input int InpE${n}_P = ${e.bbPeriod}; input double InpE${n}_Dv = ${e.bbDeviation};\n")
                IndicatorType.ADX -> append("input int InpE${n}_P = ${e.period}; input double InpE${n}_Lv = ${e.adxLevel};\n")
                IndicatorType.SAR -> append("input double InpE${n}_St = ${e.sarStep}; input double InpE${n}_Mx = ${e.sarMax};\n")
                IndicatorType.CCI -> append("input int InpE${n}_P = ${e.period}; input double InpE${n}_OB = ${e.obLevel}; input double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.ATR -> append("input int InpE${n}_P = ${e.atrPeriod}; input double InpE${n}_M = ${e.atrMultiplier};\n")
                IndicatorType.PRICE_BREAK -> append("input int InpE${n}_LB = ${e.lookbackBars};\n")
                IndicatorType.ICHIMOKU -> append("input int InpE${n}_Tk = ${e.tenkan}; input int InpE${n}_Kj = ${e.kijun}; input int InpE${n}_Sk = ${e.senkou};\n")
                IndicatorType.ALLIGATOR -> append("input int InpE${n}_J = ${e.jawPeriod}; input int InpE${n}_T = ${e.teethPeriod}; input int InpE${n}_L = ${e.lipsPeriod};\n")
                IndicatorType.CANDLE_PATTERN -> append("input string InpE${n}_Pat = \"${e.candlePattern}\";\n")
                IndicatorType.VOLUME -> append("input double InpE${n}_VTh = ${e.volumeThreshold}; input int InpE${n}_VP = ${e.period};\n")
                else -> append("input int InpE${n}_P = ${e.period};\n")
            }
            append("\n")
        }

        // Exit inputs
        c.exits.forEachIndexed { i, ex ->
            val n = i + 1
            append("input group \"═══ 出场#$n: ${ex.exitType.label} ═══\"\n")
            when (ex.exitType) {
                ExitType.FIXED_SLTP -> append("input int InpX${n}_SL = ${ex.slPoints}; input int InpX${n}_TP = ${ex.tpPoints};\n")
                ExitType.TRAILING -> append("input int InpX${n}_TS = ${ex.trailingStart}; input int InpX${n}_Tp = ${ex.trailingStep};\n")
                ExitType.ATR_SL -> append("input int InpX${n}_AP = ${ex.atrPeriodSL}; input double InpX${n}_AM = ${ex.atrMultSL};\n")
                ExitType.BREAKEVEN -> append("input int InpX${n}_BP = ${ex.breakevenPips}; input int InpX${n}_BL = ${ex.breakevenLock};\n")
                ExitType.TIME_EXIT -> append("input int InpX${n}_Min = ${ex.exitMinutes};\n")
                ExitType.TRAILING_PROFIT -> append("input int InpX${n}_PTS = ${ex.tpTrailingStart}; input int InpX${n}_PTP = ${ex.tpTrailingStep};\n")
                ExitType.MA_SL -> append("input int InpX${n}_MP = ${ex.maExitPeriod};\n")
                ExitType.PARTIAL_CLOSE -> {
                    ex.partialTPs.forEachIndexed { j, tp ->
                        append("input int InpX${n}_TP${j+1} = ${tp.tpPoints}; input double InpX${n}_PC${j+1} = ${tp.closePercent}; input bool InpX${n}_BE${j+1} = ${tp.moveSLToBE};\n")
                    }
                }
                else -> {}
            }
            append("\n")
        }

        // Money management
        append("input group \"═══ 资金管理 ═══\"\n")
        append("input double InpMM_MaxLot = ${c.money.maxLot};\n")
        when (c.money.mmType) {
            MoneyManagement.FIXED_LOT -> append("input double InpMM_Lot = ${c.money.fixedLot};\n")
            MoneyManagement.RISK_PERCENT -> {
                append("input double InpMM_Risk = ${c.money.riskPercent}; input double InpMM_Lot = ${c.money.fixedLot};\n")
            }
            MoneyManagement.MARTINGALE -> {
                append("input double InpMM_Base = ${c.money.fixedLot}; input double InpMM_Mx = ${c.money.martinMultiplier}; input int InpMM_Ms = ${c.money.martinMaxSteps}; input int InpMM_Step = ${c.money.martinStepPips};\n")
            }
            MoneyManagement.GRID -> {
                append("input int InpMM_Lv = ${c.money.gridLevels}; input int InpMM_Sp = ${c.money.gridSpacing}; input double InpMM_GLot = ${c.money.gridLot};\n")
            }
            MoneyManagement.PYRAMID -> {
                append("input double InpMM_Add = ${c.money.pyramidLotAdd}; input int InpMM_PP = ${c.money.pyramidProfitPips};\n")
            }
            MoneyManagement.ATR_BASED -> {
                append("input int InpMM_AP = ${c.money.atrLotPeriod}; input double InpMM_AR = ${c.money.atrRiskPerN};\n")
            }
            MoneyManagement.KELLY -> {
                append("input double InpMM_KW = ${c.money.kellyWinRate}; input double InpMM_KR = ${c.money.kellyWinLossRatio};\n")
            }
        }

        // Filters
        append("\ninput group \"═══ 风控 ═══\"\n")
        append("input int InpMaxPos = ${c.filter.maxPositions};\n")
        if (c.filter.useTimeFilter) append("input int InpSH = ${c.filter.startHour}; input int InpEH = ${c.filter.endHour};\n")
        if (c.filter.maxSpread > 0) append("input int InpMaxSpread = ${c.filter.maxSpread};\n")
        if (c.filter.maxDailyTrades > 0) append("input int InpMaxDaily = ${c.filter.maxDailyTrades};\n")
        if (c.filter.cooldownBars > 0) append("input int InpCooldown = ${c.filter.cooldownBars};\n")

        // Session filter
        if (c.sessionFilter.enabled) {
            append("\ninput group \"═══ 交易时段 ═══\"\n")
            append("input bool InpSessF = true;\n")
            if (c.sessionFilter.asian) append("input bool InpS_Asian = true; input int InpS_AS = ${c.sessionFilter.asianStart}; input int InpS_AE = ${c.sessionFilter.asianEnd};\n")
            if (c.sessionFilter.london) append("input bool InpS_London = true; input int InpS_LS = ${c.sessionFilter.londonStart}; input int InpS_LE = ${c.sessionFilter.londonEnd};\n")
            if (c.sessionFilter.ny) append("input bool InpS_NY = true; input int InpS_NS = ${c.sessionFilter.nyStart}; input int InpS_NE = ${c.sessionFilter.nyEnd};\n")
            append("input bool InpMon = ${c.sessionFilter.monday.toString().lowercase()}; input bool InpTue = ${c.sessionFilter.tuesday.toString().lowercase()}; input bool InpWed = ${c.sessionFilter.wednesday.toString().lowercase()}; input bool InpThu = ${c.sessionFilter.thursday.toString().lowercase()}; input bool InpFri = ${c.sessionFilter.friday.toString().lowercase()};\n")
        }

        // News filter
        if (c.newsFilter.enabled) {
            append("\ninput group \"═══ 新闻过滤 ═══\"\n")
            append("input bool InpNewsF = true;\n")
            append("input int InpNewsBef = ${c.newsFilter.beforeMinutes}; input int InpNewsAft = ${c.newsFilter.afterMinutes};\n")
        }

        // Equity protection
        if (c.equityProtection.enabled) {
            append("\ninput group \"═══ 资金保护 ═══\"\n")
            append("input double InpMaxDD = ${c.equityProtection.maxDrawdownPct};\n")
            if (c.equityProtection.dailyLossLimit > 0) append("input double InpDayLoss = ${c.equityProtection.dailyLossLimit};\n")
            if (c.equityProtection.consecutiveLosses > 0) append("input int InpConsL = ${c.equityProtection.consecutiveLosses};\n")
        }

        // v6: Signal Fusion
        if (c.signalFusion.enabled) {
            append("\ninput group \"═══ v6 信号融合 ═══\"\n")
            append("input bool InpFusion = true;\n")
            c.signalFusion.weights.forEach { sw ->
                append("input double InpFW_${sw.indicator.name} = ${sw.weight}; // ${sw.indicator.label} 权重\n")
            }
            append("input double InpF_BuyTh = ${c.signalFusion.buyThreshold}; input double InpF_SellTh = ${c.signalFusion.sellThreshold};\n")
        }

        // v6: Multi-TF confirmation
        c.entries.filter { it.multiTF.enabled }.forEach { e ->
            val n = e.id
            append("input group \"═══ 入场#$n: ${e.indicator.label} TF确认 ═══\"\n")
            append("input ENUM_TIMEFRAMES InpE${n}_MTF = ${tfConst(e.multiTF.confirmTF)}; // 确认TF\n")
            append("input int InpE${n}_MTF_Bars = ${e.multiTF.confirmBars}; // 确认K线数\n")
        }

        // v6: Telegram/Email
        if (c.notifications.telegram.enabled || c.notifications.email.enabled) {
            append("\ninput group \"═══ v6 通知 ═══\"\n")
            if (c.notifications.telegram.enabled) {
                append("input string InpTG_Token = \"${c.notifications.telegram.botToken}\"; // TG Bot Token\n")
                append("input string InpTG_ChatId = \"${c.notifications.telegram.chatId}\"; // TG Chat ID\n")
            }
        }

        // v6: MQL5 Cloud Ready flag
        if (c.mql5CloudReady) {
            append("\ninput group \"═══ v6 云端优化 ═══\"\n")
            append("input bool InpCloudReady = true; // MQL5云端优化就绪\n")
        }

        // Panel
        append("\ninput group \"═══ 面板 ═══\"\n")
        append("input bool InpPanel = ${c.showPanel.toString().lowercase()};\n")
        append("input bool InpStats = ${c.showStats.toString().lowercase()};\n")
        append("input color InpBg = ${c.panelBg}; input color InpTxt = ${c.panelText};\n")
        append("input color InpBuyC = ${c.buyColor}; input color InpSellC = ${c.sellColor};\n\n")
    }

    private fun StringBuilder.appendGlobals(c: StrategyConfig) {
        append("//──── 全局变量 ────\n")
        append("CTrade g_trade;\n")
        val refs = StrategyEngine.analyzeIndicators(c, true)
        for (r in refs) append("int ${r.name} = INVALID_HANDLE;\n")
        c.entries.filter { it.timeframe != "CURRENT" && it.indicator != IndicatorType.CUSTOM_EXPRESSION && it.indicator != IndicatorType.ICUSTOM }.forEach { e ->
            when (e.indicator) {
                IndicatorType.MA_CROSS, IndicatorType.MA_TREND, IndicatorType.MA_PRICE -> {
                    append("int e${e.id}_f = INVALID_HANDLE; int e${e.id}_s = INVALID_HANDLE;\n")
                }
                IndicatorType.RSI, IndicatorType.RSI_DIVERGENCE -> append("int e${e.id}_rsi = INVALID_HANDLE;\n")
                IndicatorType.MACD, IndicatorType.MACD_DIVERGENCE -> append("int e${e.id}_macd = INVALID_HANDLE;\n")
                IndicatorType.STOCH -> append("int e${e.id}_st = INVALID_HANDLE;\n")
                IndicatorType.BOLLINGER -> append("int e${e.id}_bb = INVALID_HANDLE;\n")
                IndicatorType.ADX -> append("int e${e.id}_adx = INVALID_HANDLE;\n")
                IndicatorType.ATR -> append("int e${e.id}_atr = INVALID_HANDLE;\n")
                IndicatorType.CCI -> append("int e${e.id}_cci = INVALID_HANDLE;\n")
                IndicatorType.ICHIMOKU -> append("int e${e.id}_ich = INVALID_HANDLE;\n")
                IndicatorType.ALLIGATOR -> append("int e${e.id}_all = INVALID_HANDLE;\n")
                IndicatorType.VOLUME -> append("int e${e.id}_vol = INVALID_HANDLE; int e${e.id}_vma = INVALID_HANDLE;\n")
                else -> {}
            }
        }
        if (c.money.mmType == MoneyManagement.ATR_BASED)
            append("int g_atrLot = INVALID_HANDLE;\n")
        if (c.multiSymbol)
            append("string g_symbols[]; int g_symCnt = 0; datetime g_lastBars[100];\n")
        else
            append("datetime g_lastBar = 0;\n")
        append("int g_todayTrades = 0; datetime g_todayDate = 0;\n")
        append("int g_consLoss = 0; double g_startEquity = 0;\n")
        append("double g_maxEquity = 0, g_dailyPL = 0;\n")
        append("int g_totalTrades = 0, g_wins = 0; double g_totalProfit = 0;\n")
        // v6: Multi-TF confirm handles
        c.entries.filter { it.multiTF.enabled }.forEach { e ->
            append("int e${e.id}_mtf = INVALID_HANDLE;\n")
        }
        append("string g_panel = \"EA_Panel\";\n\n")
    }

    private fun StringBuilder.appendOnInit(c: StrategyConfig) {
        append("int OnInit() {\n")
        append("   g_trade.SetExpertMagicNumber(InpMagic);\n")
        append("   g_trade.SetDeviationInPoints((int)InpDeviation);\n")
        append("   g_startEquity = AccountInfoDouble(ACCOUNT_EQUITY);\n")
        append("   g_maxEquity = g_startEquity;\n")
        append("   g_todayDate = iTime(_Symbol, PERIOD_D1, 0);\n")

        // Multi-symbol setup
        if (c.multiSymbol) {
            append("   if (InpMultiSym) { StringSplit(InpSymbols, ',', g_symbols); g_symCnt = ArraySize(g_symbols); for (int i = 0; i < g_symCnt; i++) StringTrimLeft(StringTrimRight(g_symbols[i])); }\n")
            append("   ArrayResize(g_lastBars, MathMax(g_symCnt, 1));\n")
        }

        // Regular indicator init
        val refs = StrategyEngine.analyzeIndicators(c, true)
        for (r in refs) {
            if (r.name.contains("icustom")) {
                append("   ${r.name} = ${r.initCode.trim()}\n")
            } else {
                append("   ${r.name} = ${r.initCode.trim()}\n")
            }
            append("   if (${r.name} == INVALID_HANDLE) { Print(\"Init failed: ${r.name}\"); return INIT_FAILED; }\n")
        }

        // MTF indicator handles
        c.entries.filter { it.timeframe != "CURRENT" && it.indicator != IndicatorType.CUSTOM_EXPRESSION && it.indicator != IndicatorType.ICUSTOM }.forEach { e ->
            val tf = tfConst(e.timeframe)
            when (e.indicator) {
                IndicatorType.MA_CROSS, IndicatorType.MA_TREND, IndicatorType.MA_PRICE -> {
                    append("   e${e.id}_f = iMA(_Symbol, $tf, InpE${e.id}_F, 0, MODE_SMA, PRICE_CLOSE);\n")
                    append("   e${e.id}_s = iMA(_Symbol, $tf, InpE${e.id}_S, 0, MODE_SMA, PRICE_CLOSE);\n")
                }
                IndicatorType.RSI, IndicatorType.RSI_DIVERGENCE -> append("   e${e.id}_rsi = iRSI(_Symbol, $tf, InpE${e.id}_P, PRICE_CLOSE);\n")
                IndicatorType.MACD, IndicatorType.MACD_DIVERGENCE -> append("   e${e.id}_macd = iMACD(_Symbol, $tf, InpE${e.id}_F, InpE${e.id}_S, InpE${e.id}_Sig, PRICE_CLOSE);\n")
                IndicatorType.STOCH -> append("   e${e.id}_st = iStochastic(_Symbol, $tf, InpE${e.id}_K, InpE${e.id}_D, InpE${e.id}_Sl, MODE_SMA, STO_LOWHIGH);\n")
                IndicatorType.BOLLINGER -> append("   e${e.id}_bb = iBands(_Symbol, $tf, InpE${e.id}_P, 0, InpE${e.id}_Dv, PRICE_CLOSE);\n")
                IndicatorType.ADX -> append("   e${e.id}_adx = iADX(_Symbol, $tf, InpE${e.id}_P);\n")
                IndicatorType.ATR -> append("   e${e.id}_atr = iATR(_Symbol, $tf, InpE${e.id}_P);\n")
                IndicatorType.CCI -> append("   e${e.id}_cci = iCCI(_Symbol, $tf, InpE${e.id}_P, PRICE_TYPICAL);\n")
                IndicatorType.ICHIMOKU -> append("   e${e.id}_ich = iIchimoku(_Symbol, $tf, InpE${e.id}_Tk, InpE${e.id}_Kj, InpE${e.id}_Sk);\n")
                IndicatorType.ALLIGATOR -> append("   e${e.id}_all = iAlligator(_Symbol, $tf, InpE${e.id}_J, 0, InpE${e.id}_T, 0, InpE${e.id}_L, 0, MODE_SMMA, PRICE_MEDIAN);\n")
                IndicatorType.VOLUME -> append("   e${e.id}_vol = iVolumes(_Symbol, $tf, VOLUME_TICK);\n   e${e.id}_vma = iMA(_Symbol, $tf, InpE${e.id}_VP, 0, MODE_SMA, e${e.id}_vol);\n")
                else -> {}
            }
        }
        if (c.money.mmType == MoneyManagement.ATR_BASED)
            append("   g_atrLot = iATR(_Symbol, PERIOD_CURRENT, InpMM_AP);\n")
        // v6: Multi-TF confirm handles init
        c.entries.filter { it.multiTF.enabled }.forEach { e ->
            val n = e.id
            when (e.indicator) {
                IndicatorType.MA_CROSS, IndicatorType.MA_PRICE ->
                    append("   e${e.id}_mtf = iMA(_Symbol, InpE${n}_MTF, InpE${n}_F, 0, MODE_SMA, PRICE_CLOSE);\n")
                IndicatorType.RSI ->
                    append("   e${e.id}_mtf = iRSI(_Symbol, InpE${n}_MTF, InpE${n}_P, PRICE_CLOSE);\n")
                IndicatorType.MACD ->
                    append("   e${e.id}_mtf = iMACD(_Symbol, InpE${n}_MTF, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE);\n")
                else ->
                    append("   e${e.id}_mtf = iMA(_Symbol, InpE${n}_MTF, 14, 0, MODE_SMA, PRICE_CLOSE);\n")
            }
        }
        append("   if (InpPanel || InpStats) CreatePanel();\n")
        append("   return INIT_SUCCEEDED;\n}\n")
    }

    private fun StringBuilder.appendOnDeinit(c: StrategyConfig) {
        append("void OnDeinit(const int r) {\n")
        val refs = StrategyEngine.analyzeIndicators(c, true)
        for (r in refs) if (r.releaseCode.isNotBlank()) append("   ${r.releaseCode.trim()}\n")
        append("   ObjectsDeleteAll(0, g_panel); Comment(\"\");\n}\n")
    }

    private fun StringBuilder.appendOnTick(c: StrategyConfig) {
        val sym = if (c.multiSymbol) "g_symbols[si]" else "_Symbol"
        append("void OnTick() {\n")
        if (c.multiSymbol) {
            append("   for (int si = 0; si < MathMax(g_symCnt, 1); si++) {\n")
            append("      string _sym = InpMultiSym && g_symCnt > 0 ? g_symbols[si] : _Symbol;\n")
            append("      if (!SymbolSelect(_sym, true)) continue;\n")
            append("      datetime cur = iTime(_sym, PERIOD_CURRENT, 0);\n")
            append("      if (g_lastBars[si] >= cur) continue;\n")
            append("      g_lastBars[si] = cur;\n")
        } else {
            append("   datetime cur = iTime(_Symbol, PERIOD_CURRENT, 0);\n")
            append("   if (g_lastBar >= cur) return;\n")
            append("   g_lastBar = cur;\n")
            append("   string _sym = _Symbol;\n")
        }

        // Equity protection
        if (c.equityProtection.enabled) {
            append("   if (!CheckEquity(_sym)) return;\n")
        }

        // Daily reset
        append("   if (g_todayDate != iTime(_sym, PERIOD_D1, 0)) { g_todayTrades = 0; g_dailyPL = 0; g_todayDate = iTime(_sym, PERIOD_D1, 0); }\n")

        // Time filter
        if (c.filter.useTimeFilter) append("   if (!IsTradeTime()) return;\n")

        // Session filter
        if (c.sessionFilter.enabled) append("   if (!IsTradingSession()) return;\n")

        // News filter
        if (c.newsFilter.enabled) append("   if (InpNewsF && IsNewsTime()) return;\n")

        // Spread filter
        if (c.filter.maxSpread > 0) append("   if (!CheckSpread(_sym)) return;\n")

        // Daily trade limit
        if (c.filter.maxDailyTrades > 0) append("   if (g_todayTrades >= InpMaxDaily) return;\n")

        // Cooldown
        if (c.filter.cooldownBars > 0) append("   if (!Cooldown(_sym)) return;\n")

        append("   ManageExits(_sym);\n")
        if (c.money.mmType == MoneyManagement.GRID) {
            append("   GridManage(_sym);\n")
        }
        if (c.money.mmType == MoneyManagement.PYRAMID) {
            append("   PyramidManage(_sym);\n")
        }
        if (c.money.mmType != MoneyManagement.GRID) {
        append("   if (InpPanel) UpdatePanel(_sym);\n")
        append("   int signal = GetSignal(_sym);\n")
        append("   int total = CountPositions(_sym);\n")

        if (c.enableHedging) {
            append("   int buys = CountDir(_sym, POSITION_TYPE_BUY); int sells = CountDir(_sym, POSITION_TYPE_SELL);\n")
            append("   if (signal > 0 && buys < InpMaxPos) OpenOrder(_sym, ORDER_TYPE_BUY);\n")
            append("   if (signal < 0 && sells < InpMaxPos) OpenOrder(_sym, ORDER_TYPE_SELL);\n")
        } else {
            append("   if (total >= InpMaxPos) return;\n")
            append("   if (signal > 0) OpenOrder(_sym, ORDER_TYPE_BUY);\n")
            append("   if (signal < 0) OpenOrder(_sym, ORDER_TYPE_SELL);\n")
        }
        if (c.multiSymbol) append("   }\n")
        if (c.money.mmType == MoneyManagement.GRID) append("   }\n")
        append("}\n")
    }

    private fun StringBuilder.appendHelpers(c: StrategyConfig) {
        // Time filter
        append("bool IsTradeTime() { MqlDateTime dt; TimeToStruct(TimeCurrent(), dt); return (dt.hour >= InpSH && dt.hour < InpEH); }\n")

        // Session filter
        if (c.sessionFilter.enabled) {
            append("bool IsTradingSession() {\n")
            append("   MqlDateTime dt; TimeToStruct(TimeCurrent(), dt);\n")
            append("   int d = dt.day_of_week;\n")
            append("   if (!InpMon && d == 1) return false; if (!InpTue && d == 2) return false; if (!InpWed && d == 3) return false;\n")
            append("   if (!InpThu && d == 4) return false; if (!InpFri && d == 5) return false; if (d == 0 || d == 6) return false;\n")
            append("   int h = dt.hour;\n")
            val hasSessions = c.sessionFilter.asian || c.sessionFilter.london || c.sessionFilter.ny
            if (hasSessions) {
                val parts = mutableListOf<String>()
                if (c.sessionFilter.asian) parts.add("(InpS_Asian && h >= InpS_AS && h < InpS_AE)")
                if (c.sessionFilter.london) parts.add("(InpS_London && h >= InpS_LS && h < InpS_LE)")
                if (c.sessionFilter.ny) parts.add("(InpS_NY && h >= InpS_NS && h < InpS_NE)")
                append("   return (${parts.joinToString(" || ")})")
            } else {
                append("   return true;")
            }
            append(";\n}\n")
        }

        // Spread
        if (c.filter.maxSpread > 0)
            append("bool CheckSpread(string sym) { double a = SymbolInfoDouble(sym, SYMBOL_ASK); double b = SymbolInfoDouble(sym, SYMBOL_BID); return ((a - b) / SymbolInfoDouble(sym, SYMBOL_POINT) <= InpMaxSpread); }\n")

        // Position counting
        append("int CountPositions(string sym) { int c = 0; for (int i = PositionsTotal() - 1; i >= 0; i--) { if (PositionSelectByTicket(PositionGetTicket(i))) if (PositionGetString(POSITION_SYMBOL) == sym && PositionGetInteger(POSITION_MAGIC) == InpMagic) c++; } return c; }\n")

        if (c.enableHedging)
            append("int CountDir(string sym, ENUM_POSITION_TYPE tp) { int c = 0; for (int i = PositionsTotal() - 1; i >= 0; i--) { if (PositionSelectByTicket(PositionGetTicket(i))) if (PositionGetString(POSITION_SYMBOL) == sym && PositionGetInteger(POSITION_MAGIC) == InpMagic && PositionGetInteger(POSITION_TYPE) == tp) c++; } return c; }\n")

        if (c.filter.maxDailyTrades > 0)
            append("int DailyTrades(string sym) { return g_todayTrades; }\n")

        // Cooldown
        if (c.filter.cooldownBars > 0)
            append("bool Cooldown(string sym) { HistorySelect(0, TimeCurrent()); datetime lastClose = 0; for (int i = HistoryDealsTotal() - 1; i >= 0; i--) { ulong t = HistoryDealGetTicket(i); if (HistoryDealGetString(t, DEAL_SYMBOL) == sym && HistoryDealGetInteger(t, DEAL_MAGIC) == InpMagic && HistoryDealGetInteger(t, DEAL_ENTRY) == DEAL_ENTRY_OUT) { lastClose = (datetime)HistoryDealGetInteger(t, DEAL_TIME); break; } } return (lastClose == 0 || iTime(sym, PERIOD_CURRENT, 0) - lastClose >= InpCooldown * PeriodSeconds(PERIOD_CURRENT)); }\n")

        // News filter (stub - requires external data source)
        if (c.newsFilter.enabled) {
            append("bool IsNewsTime() { return false; /* 通过WebRequest读取经济日历 */ }\n")
        }

        // Equity protection
        if (c.equityProtection.enabled) {
            append("bool CheckEquity(string sym) {\n")
            append("   double eq = AccountInfoDouble(ACCOUNT_EQUITY);\n")
            append("   if (eq > g_maxEquity) g_maxEquity = eq;\n")
            append("   double dd = (g_maxEquity - eq) / g_maxEquity * 100;\n")
            append("   if (dd > InpMaxDD) { Print(\"MaxDD: \", dd, \"%\"); ")
            if (c.equityProtection.stopAction == "CloseAll") append("CloseAll(); ")
            append("return false; }\n")
            if (c.equityProtection.dailyLossLimit > 0)
                append("   if (g_dailyPL < -InpDayLoss * g_startEquity / 100) { Print(\"Daily loss limit\"); return false; }\n")
            if (c.equityProtection.consecutiveLosses > 0)
                append("   if (g_consLoss >= InpConsL) { Print(\"Consecutive loss limit\"); return false; }\n")
            append("   return true;\n}\n")
            append("void CloseAll() { for (int i = PositionsTotal() - 1; i >= 0; i--) { if (PositionSelectByTicket(PositionGetTicket(i))) if (PositionGetInteger(POSITION_MAGIC) == InpMagic) g_trade.PositionClose(PositionGetTicket(i)); } }\n")
        }
        // Grid management function
        if (c.money.mmType == MoneyManagement.GRID) {
            append("void GridManage(string sym) {\n")
            append("   int cnt = CountPositions(sym);\n")
            append("   if (cnt >= InpMM_Lv) return;\n")
            append("   double price = SymbolInfoDouble(sym, SYMBOL_BID);\n")
            append("   double spacing = InpMM_Sp * SymbolInfoDouble(sym, SYMBOL_POINT);\n")
            append("   double lot = MathMin(InpMM_GLot, InpMM_MaxLot);\n")
            append("   double sl = 0, tp = 0;\n")
            append("   for (int i = 1; i <= InpMM_Lv; i++) {\n")
            append("      double buyPrice = price - i * spacing;\n")
            append("      double sellPrice = price + i * spacing;\n")
            append("      bool hasBuy = false, hasSell = false;\n")
            append("      for (int j = PositionsTotal() - 1; j >= 0; j--) {\n")
            append("         if (PositionSelectByTicket(PositionGetTicket(j))) {\n")
            append("            if (PositionGetString(POSITION_SYMBOL) == sym && PositionGetInteger(POSITION_MAGIC) == InpMagic) {\n")
            append("               double op = PositionGetDouble(POSITION_PRICE_OPEN);\n")
            append("               if (MathAbs(op - buyPrice) < spacing * 0.1) hasBuy = true;\n")
            append("               if (MathAbs(op - sellPrice) < spacing * 0.1) hasSell = true;\n")
            append("         }}}\n")
            append("      CalcSLTP(true, buyPrice, sl, tp);\n")
            append("      if (!hasBuy) g_trade.BuyLimit(lot, buyPrice, sym, sl, tp);\n")
            append("      CalcSLTP(false, sellPrice, sl, tp);\n")
            append("      if (!hasSell) g_trade.SellLimit(lot, sellPrice, sym, sl, tp);\n")
            append("   }\n}\n")
        }
        // Pyramid management function
        if (c.money.mmType == MoneyManagement.PYRAMID) {
            append("void PyramidManage(string sym) {\n")
            append("   int cnt = CountPositions(sym);\n")
            append("   if (cnt == 0 || cnt >= InpMaxPos) return;\n")
            append("   for (int i = PositionsTotal() - 1; i >= 0; i--) {\n")
            append("      if (PositionSelectByTicket(PositionGetTicket(i))) {\n")
            append("         if (PositionGetString(POSITION_SYMBOL) != sym || PositionGetInteger(POSITION_MAGIC) != InpMagic) continue;\n")
            append("         double op = PositionGetDouble(POSITION_PRICE_OPEN);\n")
            append("         double bid = SymbolInfoDouble(sym, SYMBOL_BID);\n")
            append("         if (bid - op > InpMM_PP * SymbolInfoDouble(sym, SYMBOL_POINT)) {\n")
            append("            double lot = MathMin(InpMM_Lot + InpMM_Add, InpMM_MaxLot);\n")
            append("            double sl = 0, tp = 0, ep = SymbolInfoDouble(sym, SYMBOL_ASK);\n")
            append("            CalcSLTP(true, ep, sl, tp);\n")
            append("            g_trade.Buy(lot, sym, ep, sl, tp, \"Pyramid\");\n")
            append("            return;\n")
            append("   }}}\n}\n")
        }
    }

    private fun StringBuilder.appendTradeFuncs(c: StrategyConfig) {
        append("void OpenOrder(string sym, ENUM_ORDER_TYPE dir) {\n")
        append("   double lot = CalcLot(sym);\n")
        append("   double sl = 0, tp = 0;\n")
        append("   double ep = dir == ORDER_TYPE_BUY ? SymbolInfoDouble(sym, SYMBOL_ASK) : SymbolInfoDouble(sym, SYMBOL_BID);\n")
        append("   CalcSLTP(dir == ORDER_TYPE_BUY, ep, sl, tp);\n\n")

        // Pending orders check
        val hasLimit = c.entries.any { it.entryType == EntryOrderType.LIMIT }
        val hasStop = c.entries.any { it.entryType == EntryOrderType.STOP }
        if (hasLimit || hasStop) {
            append("   // 检查是否需要挂单入场\n")
            c.entries.forEachIndexed { i, e ->
                val n = i + 1
                if (e.entryType == EntryOrderType.LIMIT) {
                    append("   if (InpE${n}_Order == ORDER_TYPE_BUY && InpE${n}_Type == ORDER_TYPE_BUY) { ep = SymbolInfoDouble(sym, SYMBOL_ASK) - InpE${n}_LmtOff * SymbolInfoDouble(sym, SYMBOL_POINT); g_trade.BuyLimit(lot, ep, sym, sl, tp, 0, 0, \"${c.strategyName} L\"); g_todayTrades++; return; }\n")
                    append("   if (InpE${n}_Order == ORDER_TYPE_SELL && InpE${n}_Type == ORDER_TYPE_SELL) { ep = SymbolInfoDouble(sym, SYMBOL_BID) + InpE${n}_LmtOff * SymbolInfoDouble(sym, SYMBOL_POINT); g_trade.SellLimit(lot, ep, sym, sl, tp, 0, 0, \"${c.strategyName} L\"); g_todayTrades++; return; }\n")
                }
                if (e.entryType == EntryOrderType.STOP) {
                    append("   if (InpE${n}_Order == ORDER_TYPE_BUY && InpE${n}_Type == ORDER_TYPE_BUY) { ep = SymbolInfoDouble(sym, SYMBOL_ASK) + InpE${n}_StpOff * SymbolInfoDouble(sym, SYMBOL_POINT); g_trade.BuyStop(lot, ep, sym, sl, tp, 0, 0, \"${c.strategyName} S\"); g_todayTrades++; return; }\n")
                    append("   if (InpE${n}_Order == ORDER_TYPE_SELL && InpE${n}_Type == ORDER_TYPE_SELL) { ep = SymbolInfoDouble(sym, SYMBOL_BID) - InpE${n}_StpOff * SymbolInfoDouble(sym, SYMBOL_POINT); g_trade.SellStop(lot, ep, sym, sl, tp, 0, 0, \"${c.strategyName} S\"); g_todayTrades++; return; }\n")
                }
            }
        }

        append("   // 市价单\n")
        append("   if (dir == ORDER_TYPE_BUY) g_trade.Buy(lot, sym, 0, sl, tp, \"${c.strategyName}\");\n")
        append("   else g_trade.Sell(lot, sym, 0, sl, tp, \"${c.strategyName}\");\n")
        append("   g_todayTrades++; g_totalTrades++;\n")
        // v6: Notification
        if (c.notifications.telegram.enabled) {
            append("   if (StringLen(InpTG_Token) > 0 && StringLen(InpTG_ChatId) > 0) {\n")
            append("      string msg = StringFormat(\"${c.strategyName}: %s %.2f Lot\", sym, lot);\n")
            append("      SendNotification(msg);\n   }\n")
        }
        append("}\n")
    }

    private fun StringBuilder.appendMoneyMgmt(c: StrategyConfig) {
        val m = c.money
        append("double CalcLot(string sym) {\n")
        when (m.mmType) {
            MoneyManagement.FIXED_LOT -> append("   return MathMin(InpMM_Lot, InpMM_MaxLot);\n")
            MoneyManagement.RISK_PERCENT -> {
                append("   double bal = AccountInfoDouble(ACCOUNT_BALANCE);\n")
                append("   double risk = bal * InpMM_Risk / 100.0;\n")
                append("   double pnt = SymbolInfoDouble(sym, SYMBOL_POINT);\n")
                append("   double tv = SymbolInfoDouble(sym, SYMBOL_TRADE_TICK_VALUE);\n")
                append("   double sl = InpX1_SL > 0 ? InpX1_SL : 100;\n")
                append("   double lot = risk / (sl * tv);\n")
                append("   return MathMin(NormalizeDouble(lot, 2), InpMM_MaxLot);\n")
            }
            MoneyManagement.MARTINGALE -> {
                append("   int ls = 0; HistorySelect(0, TimeCurrent());\n")
                append("   for (int i = HistoryDealsTotal() - 1; i >= 0; i--) {\n")
                append("      ulong t = HistoryDealGetTicket(i);\n")
                append("      if (HistoryDealGetString(t, DEAL_SYMBOL) != sym || HistoryDealGetInteger(t, DEAL_MAGIC) != InpMagic) continue;\n")
                append("      if (HistoryDealGetInteger(t, DEAL_ENTRY) != DEAL_ENTRY_OUT) continue;\n")
                append("      if (HistoryDealGetDouble(t, DEAL_PROFIT) >= 0) break; ls++;\n   }\n")
                append("   return MathMin(InpMM_Base * MathPow(InpMM_Mx, MathMin(ls, InpMM_Ms)), InpMM_MaxLot);\n")
            }
            MoneyManagement.GRID -> {
                append("   return MathMin(InpMM_GLot, InpMM_MaxLot);\n")
            }
            MoneyManagement.ATR_BASED -> {
                append("   double atr[1]; CopyBuffer(g_atrLot, 0, 0, 1, atr);\n")
                append("   double riskAmt = AccountInfoDouble(ACCOUNT_BALANCE) * InpMM_AR;\n")
                append("   double lot = riskAmt / (atr[0] * SymbolInfoDouble(sym, SYMBOL_TRADE_TICK_VALUE) * 10);\n")
                append("   return MathMin(NormalizeDouble(lot, 2), InpMM_MaxLot);\n")
            }
            MoneyManagement.PYRAMID -> {
                append("   double lastProfit = 0; HistorySelect(0, TimeCurrent());\n")
                append("   for (int i = HistoryDealsTotal() - 1; i >= 0; i--) {\n")
                append("      ulong t = HistoryDealGetTicket(i);\n")
                append("      if (HistoryDealGetString(t, DEAL_SYMBOL) == sym && HistoryDealGetInteger(t, DEAL_MAGIC) == InpMagic && HistoryDealGetInteger(t, DEAL_ENTRY) == DEAL_ENTRY_OUT) {\n")
                append("         lastProfit = HistoryDealGetDouble(t, DEAL_PROFIT); break;\n   }}\n")
                append("   return MathMin(InpMM_Lot + (lastProfit > 0 ? InpMM_Add : 0), InpMM_MaxLot);\n")
            }
            MoneyManagement.KELLY -> {
                append("   double f = InpMM_KW - (1.0 - InpMM_KW) / InpMM_KR;\n")
                append("   if (f <= 0) return SymbolInfoDouble(sym, SYMBOL_VOLUME_MIN);\n")
                append("   double lot = AccountInfoDouble(ACCOUNT_BALANCE) * f / 10000.0;\n")
                append("   return MathMin(NormalizeDouble(lot, 2), InpMM_MaxLot);\n")
            }
        }
        append("}\n")
    }

    private fun StringBuilder.appendSignalFunc(c: StrategyConfig) {
        if (c.entries.isEmpty()) { append("int GetSignal(string sym) { return 0; }\n\n"); return }

        append("int GetSignal(string sym) {\n")

        // v6: Signal Fusion mode
        if (c.signalFusion.enabled) {
            append("   if (InpFusion) {\n")
            append("      double score = 0.0, totalW = 0.0;\n")
            c.entries.forEach { e ->
                val n = e.id
                val w = c.signalFusion.weights.firstOrNull { it.indicator == e.indicator }?.weight ?: 1.0
                when (e.indicator) {
                    IndicatorType.CUSTOM_EXPRESSION -> append("      if (EvalExpr${e.id}(sym)) { score += $w; totalW += $w; }\n")
                    IndicatorType.ICUSTOM -> append("      if (CheckiCustom${e.id}(sym)) { score += $w; totalW += $w; }\n")
                    else -> append("      if (EvalEntry${e.id}(sym)) { score += $w; totalW += $w; }\n")
                }
            }
            append("      if (totalW == 0) return 0;\n")
            append("      double norm = score / totalW;\n")
            append("      if (norm >= InpF_BuyTh / (InpF_BuyTh + InpF_SellTh)) return 1;\n")
            append("      if (norm <= -InpF_SellTh / (InpF_BuyTh + InpF_SellTh)) return -1;\n")
            append("      return 0;\n   }\n\n")
        }

        var condIdx = 0
        for (e in c.entries) {
            condIdx++
            when (e.indicator) {
                IndicatorType.CUSTOM_EXPRESSION -> append("   bool c$condIdx = EvalExpr${e.id}(sym);\n")
                IndicatorType.ICUSTOM -> append("   bool c$condIdx = CheckiCustom${e.id}(sym);\n")
                else -> append("   bool c$condIdx = EvalEntry${e.id}(sym);\n")
            }
        }

        condIdx = 0
        for (e in c.entries) {
            condIdx++
            if (condIdx == 1) {
                when (e.direction) {
                    "BuyOnly" -> append("   bool buyOk = c$condIdx, sellOk = false;\n")
                    "SellOnly" -> append("   bool buyOk = false, sellOk = c$condIdx;\n")
                    else -> append("   bool buyOk = c$condIdx, sellOk = c$condIdx;\n")
                }
            } else {
                when (e.direction) {
                    "BuyOnly" -> append("   buyOk = buyOk ${e.logicOp.symbol} c$condIdx;\n")
                    "SellOnly" -> append("   sellOk = sellOk ${e.logicOp.symbol} c$condIdx;\n")
                    else -> append("   buyOk = buyOk ${e.logicOp.symbol} c$condIdx; sellOk = sellOk ${e.logicOp.symbol} c$condIdx;\n")
                }
            }
        }
        append("   if (buyOk && !sellOk) return 1;\n   if (sellOk && !buyOk) return -1;\n   return 0;\n}\n\n")

        // Individual entry functions
        c.entries.forEach { e ->
            val n = e.id
            val mtf = e.timeframe != "CURRENT"
            when (e.indicator) {
                IndicatorType.CUSTOM_EXPRESSION -> {
                    append("bool EvalExpr${e.id}(string sym) { string expr = InpE${n}_Expr; if (expr == \"\") return false; return false; /* 运行时表达式 */ }\n\n")
                }
                IndicatorType.ICUSTOM -> {
                    append("bool CheckiCustom${e.id}(string sym) {\n")
                    append("   double val[1]; CopyBuffer(e${n}_ic, InpE${n}_Buf, 0, 1, val);\n")
                    append("   return val[0] > InpE${n}_SigVal;\n}\n\n")
                }
                else -> {
                    val h = if (mtf) "_T" else ""
                    append("bool EvalEntry${e.id}(string sym) {\n")
                    when (e.indicator) {
                        IndicatorType.MA_CROSS -> {
                            append("   double f[2], s[2]; CopyBuffer(e${e.id}${h}_f, 0, 0, 2, f); CopyBuffer(e${e.id}${h}_s, 0, 0, 2, s);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (f[1] <= s[1] && f[0] > s[0]);\n")
                                "SellOnly" -> append("   return (f[1] >= s[1] && f[0] < s[0]);\n")
                                else -> append("   return (f[1] <= s[1] && f[0] > s[0]) || (f[1] >= s[1] && f[0] < s[0]);\n")
                            }
                        }
                        IndicatorType.MA_TREND -> {
                            append("   double f[1], s[1], m[1]; CopyBuffer(e${e.id}${h}_f, 0, 0, 1, f); CopyBuffer(e${e.id}${h}_s, 0, 0, 1, s); CopyBuffer(e${e.id}${h}_m, 0, 0, 1, m);\n")
                            append("   return (f[0] > s[0] && s[0] > m[0]) || (f[0] < s[0] && s[0] < m[0]);\n")
                        }
                        IndicatorType.MA_PRICE -> {
                            append("   double ma[1]; CopyBuffer(e${e.id}${h}_f, 0, 0, 1, ma);\n")
                            append("   double p1 = iClose(sym, PERIOD_CURRENT, 1); double p0 = iClose(sym, PERIOD_CURRENT, 0);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (p1 < ma[0] && p0 > ma[0]);\n")
                                "SellOnly" -> append("   return (p1 > ma[0] && p0 < ma[0]);\n")
                                else -> append("   return (p1 < ma[0] && p0 > ma[0]) || (p1 > ma[0] && p0 < ma[0]);\n")
                            }
                        }
                        IndicatorType.RSI -> {
                            append("   double r[1]; CopyBuffer(e${e.id}${h}_rsi, 0, 0, 1, r);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (r[0] < InpE${n}_OS);\n")
                                "SellOnly" -> append("   return (r[0] > InpE${n}_OB);\n")
                                else -> append("   return (r[0] < InpE${n}_OS) || (r[0] > InpE${n}_OB);\n")
                            }
                        }
                        IndicatorType.RSI_DIVERGENCE -> {
                            append("   double rsi[100]; CopyBuffer(e${e.id}${h}_rsi, 0, 0, InpE${n}_DivLB + 1, rsi);\n")
                            append("   double hh = iHigh(sym, PERIOD_CURRENT, iHighest(sym, PERIOD_CURRENT, MODE_HIGH, InpE${n}_DivLB, 1));\n")
                            append("   double ll = iLow(sym, PERIOD_CURRENT, iLowest(sym, PERIOD_CURRENT, MODE_LOW, InpE${n}_DivLB, 1));\n")
                            append("   double curR = rsi[0], prevR = rsi[InpE${n}_DivLB];\n")
                            append("   return (SymbolInfoDouble(sym, SYMBOL_ASK) > hh && curR < prevR) || (SymbolInfoDouble(sym, SYMBOL_BID) < ll && curR > prevR);\n")
                        }
                        IndicatorType.MACD -> {
                            append("   double m[2], g[2]; CopyBuffer(e${e.id}${h}_macd, 0, 0, 2, m); CopyBuffer(e${e.id}${h}_macd, 1, 0, 2, g);\n")
                            append("   return (m[1] <= g[1] && m[0] > g[0]) || (m[1] >= g[1] && m[0] < g[0]);\n")
                        }
                        IndicatorType.MACD_DIVERGENCE -> {
                            append("   double macd[100]; CopyBuffer(e${e.id}${h}_macd, 0, 0, InpE${n}_DivLB + 1, macd);\n")
                            append("   double hh = iHigh(sym, PERIOD_CURRENT, iHighest(sym, PERIOD_CURRENT, MODE_HIGH, InpE${n}_DivLB, 1));\n")
                            append("   double ll = iLow(sym, PERIOD_CURRENT, iLowest(sym, PERIOD_CURRENT, MODE_LOW, InpE${n}_DivLB, 1));\n")
                            append("   double curM = macd[0], prevM = macd[InpE${n}_DivLB];\n")
                            append("   return (SymbolInfoDouble(sym, SYMBOL_ASK) > hh && curM < prevM) || (SymbolInfoDouble(sym, SYMBOL_BID) < ll && curM > prevM);\n")
                        }
                        IndicatorType.STOCH -> {
                            append("   double k[2], d[2]; CopyBuffer(e${e.id}${h}_st, 0, 0, 2, k); CopyBuffer(e${e.id}${h}_st, 1, 0, 2, d);\n")
                            append("   return (k[1] < d[1] && k[0] > d[0] && k[0] < InpE${n}_OS) || (k[1] > d[1] && k[0] < d[0] && k[0] > InpE${n}_OB);\n")
                        }
                        IndicatorType.BOLLINGER -> {
                            append("   double u[1], l[1]; CopyBuffer(e${e.id}${h}_bb, 1, 0, 1, u); CopyBuffer(e${e.id}${h}_bb, 2, 0, 1, l);\n")
                            append("   return (Ask <= l[0]) || (Bid >= u[0]);\n")
                        }
                        IndicatorType.ADX -> {
                            append("   double a[1]; CopyBuffer(e${e.id}${h}_adx, 0, 0, 1, a);\n   return (a[0] > InpE${n}_Lv);\n")
                        }
                        IndicatorType.CCI -> {
                            append("   double c[1]; CopyBuffer(e${e.id}${h}_cci, 0, 0, 1, c);\n   return (c[0] < -InpE${n}_OS || c[0] > InpE${n}_OB);\n")
                        }
                        IndicatorType.ATR -> {
                            append("   double a[1]; CopyBuffer(e${e.id}${h}_atr, 0, 0, 1, a);\n   double r = (iHigh(sym, PERIOD_CURRENT, 1) - iLow(sym, PERIOD_CURRENT, 1)) / _Point;\n   return (r > a[0] * InpE${n}_M / SymbolInfoDouble(sym, SYMBOL_POINT));\n")
                        }
                        IndicatorType.PRICE_BREAK -> {
                            append("   double hh = iHigh(sym, PERIOD_CURRENT, iHighest(sym, PERIOD_CURRENT, MODE_HIGH, InpE${n}_LB, 1)); double ll = iLow(sym, PERIOD_CURRENT, iLowest(sym, PERIOD_CURRENT, MODE_LOW, InpE${n}_LB, 1));\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (Ask > hh);\n")
                                "SellOnly" -> append("   return (Bid < ll);\n")
                                else -> append("   return (Ask > hh) || (Bid < ll);\n")
                            }
                        }
                        IndicatorType.ICHIMOKU -> {
                            append("   double tk[1], kj[1], sa[1], sb[1];\n")
                            append("   CopyBuffer(e${e.id}${h}_ich, 0, 0, 1, tk); CopyBuffer(e${e.id}${h}_ich, 1, 0, 1, kj);\n")
                            append("   CopyBuffer(e${e.id}${h}_ich, 2, 0, 1, sa); CopyBuffer(e${e.id}${h}_ich, 3, 0, 1, sb);\n")
                            append("   double cloudTop = MathMax(sa[0], sb[0]), cloudBot = MathMin(sa[0], sb[0]);\n")
                            append("   bool tkAboveKj = tk[0] > kj[0];\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (tkAboveKj && Bid > cloudTop);\n")
                                "SellOnly" -> append("   return (!tkAboveKj && Ask < cloudBot);\n")
                                else -> append("   return (tkAboveKj && Bid > cloudTop) || (!tkAboveKj && Ask < cloudBot);\n")
                            }
                        }
                        IndicatorType.ALLIGATOR -> {
                            append("   double jaw[1], teeth[1], lips[1];\n")
                            append("   CopyBuffer(e${e.id}${h}_all, 0, 0, 1, jaw); CopyBuffer(e${e.id}${h}_all, 1, 0, 1, teeth);\n")
                            append("   CopyBuffer(e${e.id}${h}_all, 2, 0, 1, lips);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   return (lips[0] > teeth[0] && teeth[0] > jaw[0]);\n")
                                "SellOnly" -> append("   return (lips[0] < teeth[0] && teeth[0] < jaw[0]);\n")
                                else -> append("   return (lips[0] > teeth[0] && teeth[0] > jaw[0]) || (lips[0] < teeth[0] && teeth[0] < jaw[0]);\n")
                            }
                        }
                        IndicatorType.CANDLE_PATTERN -> {
                            append("   double o1 = iOpen(sym, PERIOD_CURRENT, 1), c1 = iClose(sym, PERIOD_CURRENT, 1);\n")
                            append("   double o2 = iOpen(sym, PERIOD_CURRENT, 2), c2 = iClose(sym, PERIOD_CURRENT, 2);\n")
                            append("   double h1 = iHigh(sym, PERIOD_CURRENT, 1), l1 = iLow(sym, PERIOD_CURRENT, 1);\n")
                            append("   double body1 = MathAbs(c1 - o1), range1 = h1 - l1;\n")
                            append("   if (InpE${n}_Pat == \"Engulfing\") {\n")
                            when (e.direction) {
                                "BuyOnly" -> append("      return (c2 < o2 && c1 > o1 && c1 > o2 && o1 < c2);\n")
                                "SellOnly" -> append("      return (c2 > o2 && c1 < o1 && c1 < o2 && o1 > c2);\n")
                                else -> append("      return (c2 < o2 && c1 > o1 && c1 > o2 && o1 < c2) || (c2 > o2 && c1 < o1 && c1 < o2 && o1 > c2);\n")
                            }
                            append("   }\n   if (InpE${n}_Pat == \"Hammer\") {\n")
                            append("      bool isHammer = (body1 < range1 * 0.3 && (o1 + c1) / 2 > (h1 + l1) / 2 && l1 < (h1 + l1) / 2 - range1 * 0.3);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("      return isHammer;\n")
                                "SellOnly" -> append("      bool isStar = (body1 < range1 * 0.3 && (o1 + c1) / 2 < (h1 + l1) / 2 && h1 > (h1 + l1) / 2 + range1 * 0.3);\n      return isStar;\n")
                                else -> append("      bool isStar = (body1 < range1 * 0.3 && (o1 + c1) / 2 < (h1 + l1) / 2 && h1 > (h1 + l1) / 2 + range1 * 0.3);\n      return isHammer || isStar;\n")
                            }
                            append("   }\n   if (InpE${n}_Pat == \"Doji\") {\n")
                            append("      bool d = (body1 < range1 * 0.1);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("      return (d && c2 < o2);\n")
                                "SellOnly" -> append("      return (d && c2 > o2);\n")
                                else -> append("      return d;\n")
                            }
                            append("   }\n")
                            append("   return false;\n")
                        }
                        IndicatorType.VOLUME -> {
                            append("   double vol[1], vma[1];\n")
                            append("   CopyBuffer(e${e.id}${h}_vol, 0, 0, 1, vol); CopyBuffer(e${e.id}${h}_vma, 0, 0, 1, vma);\n")
                            when (e.direction) {
                                "BuyOnly" -> append("   double o = iOpen(sym, PERIOD_CURRENT, 1);\n   double c = iClose(sym, PERIOD_CURRENT, 1);\n   return (vol[0] > vma[0] * InpE${n}_VTh && c > o);\n")
                                "SellOnly" -> append("   double o = iOpen(sym, PERIOD_CURRENT, 1);\n   double c = iClose(sym, PERIOD_CURRENT, 1);\n   return (vol[0] > vma[0] * InpE${n}_VTh && c < o);\n")
                                else -> append("   return (vol[0] > vma[0] * InpE${n}_VTh);\n")
                            }
                        }
                        else -> append("   return false;\n")
                    }
                    append("}\n\n")
                }
            }
        }
    }

    private fun StringBuilder.appendExitFunc(c: StrategyConfig) {
        append("void ManageExits(string sym) {\n")
        append("   for (int i = PositionsTotal() - 1; i >= 0; i--) {\n")
        append("      if (!PositionSelectByTicket(PositionGetTicket(i))) continue;\n")
        append("      if (PositionGetString(POSITION_SYMBOL) != sym || PositionGetInteger(POSITION_MAGIC) != InpMagic) continue;\n")
        append("      ulong dir = PositionGetInteger(POSITION_TYPE);\n")
        append("      double op = PositionGetDouble(POSITION_PRICE_OPEN);\n")
        append("      double cSL = PositionGetDouble(POSITION_SL), cTP = PositionGetDouble(POSITION_TP);\n")
        append("      double a = SymbolInfoDouble(sym, SYMBOL_ASK), b = SymbolInfoDouble(sym, SYMBOL_BID);\n")
        append("      ulong tkt = PositionGetInteger(POSITION_TICKET);\n")
        c.exits.forEach { ex ->
            val n = ex.id
            when (ex.exitType) {
                ExitType.TRAILING -> {
                    append("      if (dir == POSITION_TYPE_BUY && b - op > InpX${n}_TS * _Point) { double ns = b - InpX${n}_Tp * _Point; if (ns > cSL || cSL == 0) g_trade.PositionModify(tkt, ns, cTP); }\n")
                    append("      if (dir == POSITION_TYPE_SELL && op - a > InpX${n}_TS * _Point) { double ns = a + InpX${n}_Tp * _Point; if (ns < cSL || cSL == 0) g_trade.PositionModify(tkt, ns, cTP); }\n")
                }
                ExitType.BREAKEVEN -> {
                    append("      if (dir == POSITION_TYPE_BUY && b - op > InpX${n}_BP * _Point) { double ns = op + InpX${n}_BL * _Point; if (ns > cSL || cSL == 0) g_trade.PositionModify(tkt, ns, cTP); }\n")
                    append("      if (dir == POSITION_TYPE_SELL && op - a > InpX${n}_BP * _Point) { double ns = op - InpX${n}_BL * _Point; if (ns < cSL || cSL == 0) g_trade.PositionModify(tkt, ns, cTP); }\n")
                }
                ExitType.TIME_EXIT -> append("      if (TimeCurrent() - PositionGetInteger(POSITION_TIME) > InpX${n}_Min * 60) g_trade.PositionClose(tkt);\n")
                ExitType.PARTIAL_CLOSE -> {
                    ex.partialTPs.forEachIndexed { j, pTP ->
                        append("      static bool p${n}_${j}[500];\n")
                        append("      if (dir == POSITION_TYPE_BUY && b - op > InpX${n}_TP${j+1} * _Point && !p${n}_${j}[i]) { double v = PositionGetDouble(POSITION_VOLUME) * InpX${n}_PC${j+1} / 100; g_trade.PositionClosePartial(tkt, v); p${n}_${j}[i] = true; if (InpX${n}_BE${j+1}) g_trade.PositionModify(tkt, op, cTP); }\n")
                        append("      if (dir == POSITION_TYPE_SELL && op - a > InpX${n}_TP${j+1} * _Point && !p${n}_${j}[i]) { double v = PositionGetDouble(POSITION_VOLUME) * InpX${n}_PC${j+1} / 100; g_trade.PositionClosePartial(tkt, v); p${n}_${j}[i] = true; if (InpX${n}_BE${j+1}) g_trade.PositionModify(tkt, op, cTP); }\n")
                    }
                }
                ExitType.TRAILING_PROFIT -> {
                    append("      if (dir == POSITION_TYPE_BUY && b - op > InpX${n}_PTS * _Point) { double tpSl = b - InpX${n}_PTP * _Point; if (tpSl > cSL || cSL == 0) g_trade.PositionModify(tkt, tpSl, cTP); }\n")
                    append("      if (dir == POSITION_TYPE_SELL && op - a > InpX${n}_PTS * _Point) { double tpSl = a + InpX${n}_PTP * _Point; if (tpSl < cSL || cSL == 0) g_trade.PositionModify(tkt, tpSl, cTP); }\n")
                }
                ExitType.MA_SL -> {
                    append("      double ma[1]; CopyBuffer(iMA(sym, PERIOD_CURRENT, InpX${n}_MP, 0, MODE_SMA, PRICE_CLOSE), 0, 0, 1, ma);\n")
                    append("      if (dir == POSITION_TYPE_BUY && b < ma[0]) g_trade.PositionClose(tkt);\n")
                    append("      if (dir == POSITION_TYPE_SELL && a > ma[0]) g_trade.PositionClose(tkt);\n")
                }
                else -> {}
            }
        }
        append("   }\n}\n\n")
        append("void CalcSLTP(bool isBuy, double ep, double &sl, double &tp) {\n")
        c.exits.forEach { ex ->
            when (ex.exitType) {
                ExitType.FIXED_SLTP -> append("   sl = InpX${ex.id}_SL > 0 ? (isBuy ? ep - InpX${ex.id}_SL * _Point : ep + InpX${ex.id}_SL * _Point) : 0;\n   tp = InpX${ex.id}_TP > 0 ? (isBuy ? ep + InpX${ex.id}_TP * _Point : ep - InpX${ex.id}_TP * _Point) : 0;\n")
                ExitType.ATR_SL -> {
                    append("   double a[1]; CopyBuffer(iATR(_Symbol, PERIOD_CURRENT, InpX${ex.id}_AP), 0, 0, 1, a);\n")
                    append("   double atrSL = a[0] * InpX${ex.id}_AM;\n   sl = isBuy ? ep - atrSL : ep + atrSL;\n")
                }
                else -> {}
            }
        }
        append("}\n")
    }

    private fun StringBuilder.appendPanel(c: StrategyConfig) {
        append("void CreatePanel() {\n")
        append("   ObjectCreate(0, g_panel + \"_bg\", OBJ_RECTANGLE_LABEL, 0, 0, 0); ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_CORNER, 1);\n")
        append("   ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_XDISTANCE, 10); ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_YDISTANCE, 20);\n")
        append("   ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_XSIZE, 280); ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_YSIZE, 150);\n")
        append("   ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_BGCOLOR, ${c.panelBg}); ObjectSetInteger(0, g_panel + \"_bg\", OBJPROP_SELECTABLE, false);\n")

        append("   ObjectCreate(0, g_panel + \"_title\", OBJ_LABEL, 0, 0, 0); ObjectSetInteger(0, g_panel + \"_title\", OBJPROP_CORNER, 1);\n")
        append("   ObjectSetInteger(0, g_panel + \"_title\", OBJPROP_XDISTANCE, 20); ObjectSetInteger(0, g_panel + \"_title\", OBJPROP_YDISTANCE, 25);\n")
        append("   ObjectSetString(0, g_panel + \"_title\", OBJPROP_TEXT, \"${c.strategyName} v3.0\");\n")
        append("   ObjectSetInteger(0, g_panel + \"_title\", OBJPROP_COLOR, ${c.buyColor}); ObjectSetInteger(0, g_panel + \"_title\", OBJPROP_FONTSIZE, 11);\n")

        if (c.showStats) {
            append("   ObjectCreate(0, g_panel + \"_stats\", OBJ_LABEL, 0, 0, 0); ObjectSetInteger(0, g_panel + \"_stats\", OBJPROP_CORNER, 1);\n")
            append("   ObjectSetInteger(0, g_panel + \"_stats\", OBJPROP_XDISTANCE, 20); ObjectSetInteger(0, g_panel + \"_stats\", OBJPROP_YDISTANCE, 50);\n")
            append("   ObjectSetInteger(0, g_panel + \"_stats\", OBJPROP_COLOR, ${c.panelText}); ObjectSetInteger(0, g_panel + \"_stats\", OBJPROP_FONTSIZE, 8);\n")
        }
        append("   ChartRedraw();\n}\n")

        append("void UpdatePanel(string sym) {\n")
        append("   int n = CountPositions(sym);\n")
        if (c.showStats) {
            append("   double wr = g_totalTrades > 0 ? g_wins * 100.0 / g_totalTrades : 0;\n")
            append("   string s = StringFormat(\"持仓:%d | " + (if (c.multiSymbol) "币种:%s | " else "") + "胜率:%.0f%% | 盈亏:%.2f | DD:%.1f%%\", n, "
                + (if (c.multiSymbol) "sym, " else "") + "wr, g_totalProfit, (g_maxEquity > 0 ? (g_maxEquity - AccountInfoDouble(ACCOUNT_EQUITY)) / g_maxEquity * 100 : 0));\n")
            append("   ObjectSetString(0, g_panel + \"_stats\", OBJPROP_TEXT, s);\n")
        }
        append("   ChartRedraw();\n}\n")
    }

    private fun StringBuilder.appendStats(c: StrategyConfig) {
        if (!c.showStats) return
        append("void OnTrade() {\n")
        append("   HistorySelect(0, TimeCurrent());\n")
        append("   ulong lastDeal = 0; datetime lastTime = 0;\n")
        append("   for (int i = HistoryDealsTotal() - 1; i >= 0; i--) {\n")
        append("      ulong t = HistoryDealGetTicket(i);\n")
        append("      if (HistoryDealGetInteger(t, DEAL_MAGIC) != InpMagic) continue;\n")
        append("      if (HistoryDealGetInteger(t, DEAL_ENTRY) != DEAL_ENTRY_OUT) continue;\n")
        append("      datetime dt = (datetime)HistoryDealGetInteger(t, DEAL_TIME);\n")
        append("      if (dt > lastTime) { lastTime = dt; lastDeal = t; }\n")
        append("   }\n")
        append("   if (lastDeal != 0) {\n")
        append("      double profit = HistoryDealGetDouble(lastDeal, DEAL_PROFIT);\n")
        append("      g_totalProfit += profit; g_dailyPL += profit;\n")
        append("      if (profit > 0) { g_wins++; g_consLoss = 0; } else g_consLoss++;\n")
        append("   }\n}\n")
    }

    private fun StringBuilder.appendFooter() { append("//+------------------------------------------------------------------+\n") }
}
