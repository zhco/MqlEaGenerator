package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

object Mql4Generator {

    fun tfConst(tf: String) = when (tf) {
        "M1" -> "1"; "M5" -> "5"; "M15" -> "15"; "M30" -> "30"
        "H1" -> "60"; "H4" -> "240"; "D1" -> "1440"; "W1" -> "10080"; "MN" -> "43200"
        else -> "0"
    }

    fun generate(config: StrategyConfig): String {
        val sb = StringBuilder()
        appendHeader(sb, config)
        appendInputs(sb, config)
        appendGlobals(sb, config)
        appendInit(sb, config)
        appendDeinit(sb, config)
        appendStart(sb, config)
        appendHelpers(sb, config)
        appendTradeFuncs(sb, config)
        appendMoneyMgmt(sb, config)
        appendSignalFunc(sb, config)
        appendExitFunc(sb, config)
        appendPanel(sb, config)
        appendStats(sb, config)
        appendFooter(sb)
        return sb.toString()
    }

    private fun appendHeader(sb: StringBuilder, c: StrategyConfig) {
        sb.append("""//+------------------------------------------------------------------+
//|                                        ${c.strategyName}.mq4       |
//|                                        MQL4 Expert Advisor v3      |
//+------------------------------------------------------------------+
#property copyright   "${c.strategyName}"
#property version     "3.0"
#property strict
""")
        if (c.multiSymbol) sb.append("// 多币种模式: ${c.symbols.joinToString(", ")}\n")
        sb.append("\n")
    }

    private fun appendInputs(sb: StringBuilder, c: StrategyConfig) {
        sb.append("//──── 输入参数 ────\n")
        sb.append("extern int     InpMagic = ${c.magicNumber};     // 魔术号\n")
        sb.append("extern int     InpSlippage = ${c.slippage};     // 滑点\n")
        if (c.multiSymbol) {
            sb.append("extern bool    InpMultiSym = true;              // 多币种\n")
            sb.append("extern string  InpSymbols = \"${c.symbols.joinToString(",")}\"; // 币种列表\n")
        }
        sb.append("\n")

        c.entries.forEachIndexed { i, e ->
            val n = i + 1
            sb.append("//── 入场#$n: ${e.indicator.label}\n")
            sb.append("extern int     InpE${n}_Dir = 0;  // 0=双向 1=多 2=空\n")
            if (e.entryType == EntryOrderType.LIMIT) sb.append("extern int  InpE${n}_Lim = ${e.limitOffset};\n")
            if (e.entryType == EntryOrderType.STOP) sb.append("extern int  InpE${n}_Stp = ${e.stopOffset};\n")
            if (e.timeframe != "CURRENT" && e.indicator != IndicatorType.CUSTOM_EXPRESSION)
                sb.append("extern int  InpE${n}_TF = ${tfConst(e.timeframe)};\n")

            when (e.indicator) {
                IndicatorType.ICUSTOM -> {
                    sb.append("extern string InpE${n}_Indi = \"${e.customIndiName}\";\n")
                    sb.append("extern string InpE${n}_Param = \"${e.customIndiParams}\";\n")
                    sb.append("extern int    InpE${n}_Buf = ${e.customIndiBuffer};\n")
                    sb.append("extern double InpE${n}_Sig = ${e.customIndiSignal};\n")
                }
                IndicatorType.CUSTOM_EXPRESSION -> sb.append("extern string InpE${n}_Expr = \"\";\n")
                IndicatorType.MA, IndicatorType.MA -> {
                    sb.append("extern int  InpE${n}_F = ${e.fastPeriod}; extern int InpE${n}_S = ${e.slowPeriod};\n")
                    sb.append("extern int  InpE${n}_MAM = MODE_${e.maMethod.uppercase()};\n")
                }
                IndicatorType.PRICE -> {
                    sb.append("extern int  InpE${n}_F = ${e.fastPeriod}; extern int InpE${n}_MAM = MODE_${e.maMethod.uppercase()};\n")
                }
                IndicatorType.RSI -> sb.append("extern int  InpE${n}_P = ${e.period}; extern double InpE${n}_OB = ${e.obLevel}; extern double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.STOCH -> sb.append("extern int  InpE${n}_K = ${e.kPeriod}; extern int InpE${n}_D = ${e.dPeriod}; extern int InpE${n}_Sl = ${e.slowing}; extern double InpE${n}_OB = ${e.obLevel}; extern double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.MACD -> sb.append("extern int  InpE${n}_F = ${e.fastPeriod}; extern int InpE${n}_S = ${e.slowPeriod}; extern int InpE${n}_Sig = ${e.period};\n")
                IndicatorType.BOLLINGER -> sb.append("extern int  InpE${n}_P = ${e.bbPeriod}; extern double InpE${n}_Dv = ${e.bbDeviation};\n")
                IndicatorType.ADX -> sb.append("extern int  InpE${n}_P = ${e.period}; extern double InpE${n}_Lv = ${e.adxLevel};\n")
                IndicatorType.SAR -> sb.append("extern double InpE${n}_St = ${e.sarStep}; extern double InpE${n}_Mx = ${e.sarMax};\n")
                IndicatorType.CCI -> sb.append("extern int  InpE${n}_P = ${e.period}; extern double InpE${n}_OB = ${e.obLevel}; extern double InpE${n}_OS = ${e.osLevel};\n")
                IndicatorType.ATR -> sb.append("extern int  InpE${n}_P = ${e.atrPeriod}; extern double InpE${n}_M = ${e.atrMultiplier};\n")
                IndicatorType.PRICE -> sb.append("extern int  InpE${n}_LB = ${e.lookbackBars};\n")
                IndicatorType.ICHIMOKU -> sb.append("extern int  InpE${n}_T = ${e.tenkan}; extern int InpE${n}_K = ${e.kijun}; extern int InpE${n}_S = ${e.senkou};\n")
                IndicatorType.ALLIGATOR -> sb.append("extern int  InpE${n}_J = ${e.jawPeriod}; extern int InpE${n}_T = ${e.teethPeriod}; extern int InpE${n}_L = ${e.lipsPeriod};\n")
                IndicatorType.CANDLE_PATTERN -> sb.append("extern string InpE${n}_CP = \"${e.candlePattern}\";\n")
                IndicatorType.VOLUME -> sb.append("extern double InpE${n}_VT = ${e.volumeThreshold};\n")
                else -> sb.append("extern int  InpE${n}_P = ${e.period};\n")
            }
            sb.append("\n")
        }

        c.exits.forEachIndexed { i, ex ->
            val n = i + 1
            sb.append("//── 出场#$n: ${ex.exitType.label}\n")
            when (ex.exitType) {
                ExitType.FIXED_SLTP -> sb.append("extern int InpX${n}_SL = ${ex.slPoints}; extern int InpX${n}_TP = ${ex.tpPoints};\n")
                ExitType.TRAILING -> sb.append("extern int InpX${n}_TS = ${ex.trailingStart}; extern int InpX${n}_Tp = ${ex.trailingStep};\n")
                ExitType.ATR_SL -> sb.append("extern int InpX${n}_AP = ${ex.atrPeriodSL}; extern double InpX${n}_AM = ${ex.atrMultSL};\n")
                ExitType.BREAKEVEN -> sb.append("extern int InpX${n}_BP = ${ex.breakevenPips}; extern int InpX${n}_BL = ${ex.breakevenLock};\n")
                ExitType.TIME_EXIT -> sb.append("extern int InpX${n}_Min = ${ex.exitMinutes};\n")
                ExitType.TRAILING_PROFIT -> sb.append("extern int InpX${n}_PTS = ${ex.tpTrailingStart}; extern int InpX${n}_PTP = ${ex.tpTrailingStep};\n")
                ExitType.MA_SL -> sb.append("extern int InpX${n}_MP = ${ex.maExitPeriod};\n")
                ExitType.PARTIAL_CLOSE -> {
                    ex.partialTPs.forEachIndexed { j, tp ->
                        sb.append("extern int InpX${n}_TP${j+1} = ${tp.tpPoints}; extern double InpX${n}_PC${j+1} = ${tp.closePercent}; extern bool InpX${n}_BE${j+1} = ${tp.moveSLToBE};\n")
                    }
                }
                else -> {}
            }
            sb.append("\n")
        }

        sb.append("//── 资金管理\n")
        sb.append("extern double InpMM_MaxLot = ${c.money.maxLot};\n")
        when (c.money.mmType) {
            MoneyManagement.FIXED_LOT -> sb.append("extern double InpMM_Lot = ${c.money.fixedLot};\n")
            MoneyManagement.RISK_PERCENT -> sb.append("extern double InpMM_Risk = ${c.money.riskPercent};\n")
            MoneyManagement.MARTINGALE -> sb.append("extern double InpMM_Base = ${c.money.fixedLot}; extern double InpMM_Mx = ${c.money.martinMultiplier}; extern int InpMM_Ms = ${c.money.martinMaxSteps};\n")
            MoneyManagement.GRID -> sb.append("extern int    InpMM_Lv = ${c.money.gridLevels}; extern int InpMM_Sp = ${c.money.gridSpacing}; extern double InpMM_GLot = ${c.money.gridLot};\n")
            MoneyManagement.PYRAMID -> sb.append("extern double InpMM_Add = ${c.money.pyramidLotAdd}; extern int InpMM_PP = ${c.money.pyramidProfitPips};\n")
            MoneyManagement.ATR_BASED -> sb.append("extern int    InpMM_AP = ${c.money.atrLotPeriod}; extern double InpMM_AR = ${c.money.atrRiskPerN};\n")
            MoneyManagement.KELLY -> sb.append("extern double InpMM_KW = ${c.money.kellyWinRate}; extern double InpMM_KR = ${c.money.kellyWinLossRatio};\n")
        }

        // v6: Signal Fusion
        if (c.signalFusion.enabled) {
            sb.append("\n//── v6: 信号融合\n")
            sb.append("extern bool InpFusion = true;\n")
            c.signalFusion.weights.forEachIndexed { j, w ->
                sb.append("extern double InpF_W${j+1} = ${w.weight}; // ${w.indicator.label}权重\n")
            }
            sb.append("extern double InpF_BuyTh = ${c.signalFusion.buyThreshold}; extern double InpF_SellTh = ${c.signalFusion.sellThreshold};\n")
        }

        // v6: Multi-TF confirmation
        c.entries.filter { it.multiTF.enabled }.forEach { e ->
            val n = e.id
            sb.append("//── v6: 入场#$n ${e.indicator.label} TF确认\n")
            sb.append("extern int InpE${n}_MTF = ${tfConst(e.multiTF.confirmTF)}; // 确认TF(分钟)\n")
            sb.append("extern int InpE${n}_MTF_Bars = ${e.multiTF.confirmBars}; // 确认K线数\n")
        }

        sb.append("\n//── 风控\n")
        sb.append("extern int    InpMaxPos = ${c.filter.maxPositions};\n")
        if (c.filter.useTimeFilter) sb.append("extern int   InpSH = ${c.filter.startHour}; extern int InpEH = ${c.filter.endHour};\n")
        if (c.filter.maxSpread > 0) sb.append("extern int   InpMaxSpread = ${c.filter.maxSpread};\n")
        if (c.filter.maxDailyTrades > 0) sb.append("extern int  InpMaxDaily = ${c.filter.maxDailyTrades};\n")

        // v6: News filter
        if (c.newsFilter.enabled) {
            sb.append("//── v6: 新闻过滤\n")
            sb.append("extern bool InpNewsF = true;\n")
            sb.append("extern int InpNewsBef = ${c.newsFilter.beforeMinutes}; extern int InpNewsAft = ${c.newsFilter.afterMinutes};\n")
        }

        // v6: Correlation filter
        if (c.correlationFilter.enabled) {
            sb.append("//── v6: 相关性过滤\n")
            sb.append("extern double InpMaxCorr = ${c.correlationFilter.maxCorrelation};\n")
            sb.append("extern int InpCorrLB = ${c.correlationFilter.lookbackBars};\n")
        }

        if (c.sessionFilter.enabled) {
            sb.append("//── 交易时段\n")
            sb.append("extern bool InpSessF = true;\n")
            if (c.sessionFilter.asian) sb.append("extern bool InpS_Asian = true; extern int InpS_AS = ${c.sessionFilter.asianStart}; extern int InpS_AE = ${c.sessionFilter.asianEnd};\n")
            if (c.sessionFilter.london) sb.append("extern bool InpS_London = true; extern int InpS_LS = ${c.sessionFilter.londonStart}; extern int InpS_LE = ${c.sessionFilter.londonEnd};\n")
            if (c.sessionFilter.ny) sb.append("extern bool InpS_NY = true; extern int InpS_NS = ${c.sessionFilter.nyStart}; extern int InpS_NE = ${c.sessionFilter.nyEnd};\n")
            sb.append("extern bool InpMon=${c.sessionFilter.monday.toString().lowercase()}; extern bool InpTue=${c.sessionFilter.tuesday.toString().lowercase()}; extern bool InpWed=${c.sessionFilter.wednesday.toString().lowercase()}; extern bool InpThu=${c.sessionFilter.thursday.toString().lowercase()}; extern bool InpFri=${c.sessionFilter.friday.toString().lowercase()};\n")
        }

        if (c.equityProtection.enabled) {
            sb.append("//── 资金保护\n")
            sb.append("extern double InpMaxDD = ${c.equityProtection.maxDrawdownPct};\n")
            if (c.equityProtection.dailyLossLimit > 0) sb.append("extern double InpDayLoss = ${c.equityProtection.dailyLossLimit};\n")
            if (c.equityProtection.consecutiveLosses > 0) sb.append("extern int InpConsL = ${c.equityProtection.consecutiveLosses};\n")
        }

        // v6: Notifications
        if (c.notifications.telegram.enabled || c.notifications.email.enabled) {
            sb.append("\n//── v6: 通知\n")
            if (c.notifications.telegram.enabled) {
                sb.append("extern string InpTG_Token = \"${c.notifications.telegram.botToken}\"; // TG Bot Token\n")
                sb.append("extern string InpTG_ChatId = \"${c.notifications.telegram.chatId}\"; // TG Chat ID\n")
            }
        }

        // v6: Cloud Ready
        if (c.mql5CloudReady) {
            sb.append("//── v6: 云端优化\n")
            sb.append("extern bool InpCloudReady = true; // MQL5云端优化就绪\n")
        }

        sb.append("\n//── 面板\n")
        sb.append("extern bool   InpPanel = ${c.showPanel.toString().lowercase()};\n")
        sb.append("extern bool   InpStats = ${c.showStats.toString().lowercase()};\n")
        sb.append("extern color  InpBg = ${c.panelBg}; extern color InpTxt = ${c.panelText};\n\n")
    }

    private fun appendGlobals(sb: StringBuilder, c: StrategyConfig) {
        sb.append("//──── 全局变量\n")
        val refs = StrategyEngine.analyzeIndicators(c, false)
        for (r in refs) sb.append("int ${r.name} = 0;\n")
        if (c.multiSymbol) sb.append("string g_syms[]; int g_symCnt = 0; datetime g_lb[];\n")
        else sb.append("datetime g_lastBar = 0;\n")
        sb.append("int g_todayTrades = 0; datetime g_today = 0;\n")
        sb.append("int g_consLoss = 0; double g_startEq = 0, g_maxEq = 0, g_dailyPL = 0;\n")
        sb.append("int g_trades = 0, g_wins = 0; double g_profit = 0;\n\n")
    }

    private fun appendInit(sb: StringBuilder, c: StrategyConfig) {
        sb.append("int init() {\n")
        sb.append("   g_startEq = AccountEquity(); g_maxEq = g_startEq;\n")
        sb.append("   g_today = iTime(_Symbol, PERIOD_D1, 0);\n")
        if (c.multiSymbol) {
            sb.append("   if (InpMultiSym) { StringSplit(InpSymbols, ',', g_syms); g_symCnt = ArraySize(g_syms); }\n")
            sb.append("   ArrayResize(g_lb, MathMax(1, g_symCnt));\n")
        }
        val refs = StrategyEngine.analyzeIndicators(c, false)
        for (r in refs) sb.append("   ${r.name} = ${r.initCode.trim()};\n")
        sb.append("   return 0;\n}\n")
    }

    private fun appendDeinit(sb: StringBuilder, c: StrategyConfig) {
        sb.append("void deinit() {\n")
        val refs = StrategyEngine.analyzeIndicators(c, false)
        for (r in refs) if (r.releaseCode.isNotBlank()) sb.append("   ${r.releaseCode.trim()}\n")
        sb.append("   ObjectsDeleteAll(0, \"EA_\"); Comment(\"\");\n}\n")
    }

    private fun appendStart(sb: StringBuilder, c: StrategyConfig) {
        sb.append("void start() {\n")
        if (c.multiSymbol) {
            sb.append("   for (int si = 0; si < MathMax(1, g_symCnt); si++) {\n")
            sb.append("      string s = InpMultiSym && g_symCnt > 0 ? g_syms[si] : _Symbol;\n")
            sb.append("      datetime cur = iTime(s, 0, 0); if (g_lb[si] >= cur) continue; g_lb[si] = cur;\n")
        } else {
            sb.append("   datetime cur = iTime(_Symbol, 0, 0); if (g_lastBar >= cur) return; g_lastBar = cur;\n")
            sb.append("   string s = _Symbol;\n")
        }
        if (c.equityProtection.enabled) sb.append("   if (!CheckEquity()) return;\n")
        sb.append("   if (g_today != iTime(s, PERIOD_D1, 0)) { g_todayTrades = 0; g_dailyPL = 0; g_today = iTime(s, PERIOD_D1, 0); }\n")
        if (c.filter.useTimeFilter) sb.append("   if (!IsTradeTime()) return;\n")
        if (c.sessionFilter.enabled) sb.append("   if (InpSessF && !IsSession()) return;\n")
        if (c.newsFilter.enabled) sb.append("   if (InpNewsF && IsNewsTime()) return;\n")
        if (c.filter.maxSpread > 0) sb.append("   if (!CheckSpread(s)) return;\n")
        if (c.filter.maxDailyTrades > 0) sb.append("   if (g_todayTrades >= InpMaxDaily) return;\n")
        if (c.correlationFilter.enabled) sb.append("   if (!CheckCorrelation(s)) return;\n")
        sb.append("   ManageExits(s);\n")
        if (c.money.mmType == MoneyManagement.GRID) {
            sb.append("   GridManage(s);\n")
        }
        if (c.money.mmType == MoneyManagement.PYRAMID) {
            sb.append("   PyramidManage(s);\n")
        }
        if (c.money.mmType != MoneyManagement.GRID) {
        sb.append("   if (InpPanel) UpdatePanel(s);\n")
        sb.append("   int sig = GetSignal(s);\n")
        sb.append("   int total = CountPositions(s);\n")
        if (c.enableHedging) {
            sb.append("   int buys = CountDir(s, OP_BUY), sells = CountDir(s, OP_SELL);\n")
            sb.append("   if (sig > 0 && buys < InpMaxPos) OpenOrder(s, OP_BUY);\n")
            sb.append("   if (sig < 0 && sells < InpMaxPos) OpenOrder(s, OP_SELL);\n")
        } else {
            sb.append("   if (total >= InpMaxPos) return;\n")
            sb.append("   if (sig > 0) OpenOrder(s, OP_BUY);\n")
            sb.append("   if (sig < 0) OpenOrder(s, OP_SELL);\n")
        }
        if (c.money.mmType == MoneyManagement.GRID) sb.append("   }\n")
        if (c.multiSymbol) sb.append("   }\n")
        sb.append("}\n")
    }
    }

    fun appendHelpers(sb: StringBuilder, c: StrategyConfig) {
        sb.append("bool IsTradeTime() { int h = Hour(); return (h >= InpSH && h < InpEH); }\n")
        if (c.sessionFilter.enabled) {
            sb.append("bool IsSession() { int h = Hour(), d = DayOfWeek();\n")
            sb.append("   if (d == 0 || d == 6) return false;\n")
            sb.append("   if (!InpMon && d == 1) return false; if (!InpTue && d == 2) return false; if (!InpWed && d == 3) return false; if (!InpThu && d == 4) return false; if (!InpFri && d == 5) return false;\n")
            val parts = mutableListOf<String>()
            if (c.sessionFilter.asian) parts.add("(InpS_Asian && h >= InpS_AS && h < InpS_AE)")
            if (c.sessionFilter.london) parts.add("(InpS_London && h >= InpS_LS && h < InpS_LE)")
            if (c.sessionFilter.ny) parts.add("(InpS_NY && h >= InpS_NS && h < InpS_NE)")
            sb.append("   return (${if (parts.isEmpty()) "true" else parts.joinToString(" || ")});\n}\n")
        }
        if (c.filter.maxSpread > 0) sb.append("bool CheckSpread(string sym) { return (MarketInfo(sym, MODE_ASK) - MarketInfo(sym, MODE_BID)) / Point <= InpMaxSpread; }\n")
        // v6: News filter stub
        if (c.newsFilter.enabled) sb.append("bool IsNewsTime() { return false; /* 需外部经济日历数据源 */ }\n")
        // v6: Correlation filter
        if (c.correlationFilter.enabled) {
            sb.append("bool CheckCorrelation(string sym) {\n")
            sb.append("   int bars = MathMin(InpCorrLB, Bars - 1);\n")
            sb.append("   double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;\n")
            sb.append("   for (int i = 1; i <= bars; i++) {\n")
            sb.append("      double x = (iClose(sym, 0, i) - iClose(sym, 0, i+1)) / iClose(sym, 0, i+1);\n")
            sb.append("      double y = (iClose(_Symbol, 0, i) - iClose(_Symbol, 0, i+1)) / iClose(_Symbol, 0, i+1);\n")
            sb.append("      sumX += x; sumY += y; sumXY += x*y; sumX2 += x*x; sumY2 += y*y;\n")
            sb.append("   }\n")
            sb.append("   double denom = MathSqrt((bars*sumX2 - sumX*sumX) * (bars*sumY2 - sumY*sumY));\n")
            sb.append("   if (denom == 0) return true;\n")
            sb.append("   double r = (bars*sumXY - sumX*sumY) / denom;\n")
            sb.append("   if (sym == _Symbol) return true;\n")
            sb.append("   return (MathAbs(r) < InpMaxCorr);\n}\n")
        }
        sb.append("int CountPositions(string sym) { int c = 0; for (int i = 0; i < OrdersTotal(); i++) { if (OrderSelect(i, SELECT_BY_POS) && OrderSymbol() == sym && OrderMagicNumber() == InpMagic) c++; } return c; }\n")
        if (c.enableHedging) sb.append("int CountDir(string sym, int tp) { int c = 0; for (int i = 0; i < OrdersTotal(); i++) { if (OrderSelect(i, SELECT_BY_POS) && OrderSymbol() == sym && OrderMagicNumber() == InpMagic && OrderType() == tp) c++; } return c; }\n")
        if (c.equityProtection.enabled) {
            sb.append("bool CheckEquity() { double eq = AccountEquity(); if (eq > g_maxEq) g_maxEq = eq;\n")
            sb.append("   double dd = (g_maxEq - eq) / g_maxEq * 100; if (dd > InpMaxDD) { CloseAll(); return false; }\n")
            if (c.equityProtection.dailyLossLimit > 0) sb.append("   if (g_dailyPL < -InpDayLoss * g_startEq / 100) return false;\n")
            if (c.equityProtection.consecutiveLosses > 0) sb.append("   if (g_consLoss >= InpConsL) return false;\n")
            sb.append("   return true;\n}\n")
            sb.append("void CloseAll() { for (int i = OrdersTotal() - 1; i >= 0; i--) { if (OrderSelect(i, SELECT_BY_POS) && OrderMagicNumber() == InpMagic) OrderClose(OrderTicket(), OrderLots(), OrderType() == OP_BUY ? Bid : Ask, 3); } }\n")
        }
        if (c.money.mmType == MoneyManagement.GRID) {
            sb.append("void GridManage(string sym) {\n")
            sb.append("   double price = Bid;\n")
            sb.append("   double spacing = InpMM_Sp * Point;\n")
            sb.append("   double lot = MathMin(InpMM_GLot, InpMM_MaxLot);\n")
            sb.append("   double sl = 0, tp = 0;\n")
            sb.append("   for (int i = 1; i <= InpMM_Lv; i++) {\n")
            sb.append("      double bp = price - i * spacing; double sp = price + i * spacing;\n")
            sb.append("      bool hb = false, hs = false;\n")
            sb.append("      for (int j = OrdersTotal() - 1; j >= 0; j--) {\n")
            sb.append("         if (OrderSelect(j, SELECT_BY_POS) && OrderSymbol() == sym && OrderMagicNumber() == InpMagic) {\n")
            sb.append("            double op = OrderOpenPrice();\n")
            sb.append("            if (MathAbs(op - bp) < spacing * 0.1) hb = true;\n")
            sb.append("            if (MathAbs(op - sp) < spacing * 0.1) hs = true; }}\n")
            sb.append("      CalcSLTP(true, bp, sl, tp);\n")
            sb.append("      if (!hb) OrderSend(sym, OP_BUYLIMIT, lot, bp, InpSlippage, sl, tp, \"Grid\", InpMagic, 0);\n")
            sb.append("      CalcSLTP(false, sp, sl, tp);\n")
            sb.append("      if (!hs) OrderSend(sym, OP_SELLLIMIT, lot, sp, InpSlippage, sl, tp, \"Grid\", InpMagic, 0);\n")
            sb.append("   }\n}\n")
        }
        if (c.money.mmType == MoneyManagement.PYRAMID) {
            sb.append("void PyramidManage(string sym) {\n")
            sb.append("   int cnt = CountPositions(sym);\n")
            sb.append("   if (cnt == 0 || cnt >= InpMaxPos) return;\n")
            sb.append("   for (int i = OrdersTotal() - 1; i >= 0; i--) {\n")
            sb.append("      if (OrderSelect(i, SELECT_BY_POS) && OrderSymbol() == sym && OrderMagicNumber() == InpMagic) {\n")
            sb.append("         if (OrderType() != OP_BUY) continue;\n")
            sb.append("         if (Bid - OrderOpenPrice() > InpMM_PP * Point) {\n")
            sb.append("            double lot = MathMin(OrderLots() + InpMM_Add, InpMM_MaxLot);\n")
            sb.append("            double sl = 0, tp = 0;\n")
            sb.append("            CalcSLTP(true, Ask, sl, tp);\n")
            sb.append("            OrderSend(sym, OP_BUY, lot, Ask, InpSlippage, sl, tp, \"Pyramid\", InpMagic, 0);\n")
            sb.append("            return;\n")
            sb.append("   } } }\n}\n")
        }
    }

    fun appendTradeFuncs(sb: StringBuilder, c: StrategyConfig) {
        sb.append("void OpenOrder(string sym, int dir) {\n")
        sb.append("   double lot = CalcLot(sym); double sl = 0, tp = 0;\n")
        sb.append("   double ep = dir == OP_BUY ? MarketInfo(sym, MODE_ASK) : MarketInfo(sym, MODE_BID);\n")
        sb.append("   CalcSLTP(dir == OP_BUY, ep, sl, tp);\n")

        // Pending orders
        c.entries.forEach { e ->
            val n = e.id
            if (e.entryType == EntryOrderType.LIMIT) {
                sb.append("   if (InpE${n}_Dir == 1) { ep = MarketInfo(sym, MODE_ASK) - InpE${n}_Lim * Point; int t = OrderSend(sym, OP_BUYLIMIT, lot, ep, InpSlippage, sl, tp, \"${c.strategyName}\", InpMagic, 0); g_todayTrades++; return; }\n")
                sb.append("   if (InpE${n}_Dir == 2) { ep = MarketInfo(sym, MODE_BID) + InpE${n}_Lim * Point; int t = OrderSend(sym, OP_SELLLIMIT, lot, ep, InpSlippage, sl, tp, \"${c.strategyName}\", InpMagic, 0); g_todayTrades++; return; }\n")
            }
            if (e.entryType == EntryOrderType.STOP) {
                sb.append("   if (InpE${n}_Dir == 1) { ep = MarketInfo(sym, MODE_ASK) + InpE${n}_Stp * Point; int t = OrderSend(sym, OP_BUYSTOP, lot, ep, InpSlippage, sl, tp, \"${c.strategyName}\", InpMagic, 0); g_todayTrades++; return; }\n")
                sb.append("   if (InpE${n}_Dir == 2) { ep = MarketInfo(sym, MODE_BID) - InpE${n}_Stp * Point; int t = OrderSend(sym, OP_SELLSTOP, lot, ep, InpSlippage, sl, tp, \"${c.strategyName}\", InpMagic, 0); g_todayTrades++; return; }\n")
            }
        }
        sb.append("   int t = OrderSend(sym, dir, lot, ep, InpSlippage, sl, tp, \"${c.strategyName}\", InpMagic, 0, dir == OP_BUY ? Green : Red);\n")
        sb.append("   if (t > 0) { g_todayTrades++; g_trades++; ")
        if (c.notifications.telegram.enabled) {
            sb.append("if (StringLen(InpTG_Token) > 0) SendNotification(sym + \" \" + (dir == OP_BUY ? \"BUY\" : \"SELL\") + \" \" + DoubleToStr(lot, 2) + \" Lot\"); ")
        }
        sb.append("}\n}\n")
    }

    fun appendMoneyMgmt(sb: StringBuilder, c: StrategyConfig) {
        val m = c.money
        sb.append("double CalcLot(string sym) {\n")
        when (m.mmType) {
            MoneyManagement.FIXED_LOT -> sb.append("   return MathMin(InpMM_Lot, InpMM_MaxLot);\n")
            MoneyManagement.RISK_PERCENT -> {
                sb.append("   double risk = AccountEquity() * InpMM_Risk / 100;\n")
                sb.append("   double tv = MarketInfo(sym, MODE_TICKVALUE);\n")
                sb.append("   double sl = InpX1_SL > 0 ? InpX1_SL : 100;\n")
                sb.append("   double lot = NormalizeDouble(risk / (sl * tv), 2);\n")
                sb.append("   return MathMin(lot, InpMM_MaxLot);\n")
            }
            MoneyManagement.MARTINGALE -> {
                sb.append("   int ls = 0;\n")
                sb.append("   for (int i = OrdersHistoryTotal() - 1; i >= 0; i--) { if (OrderSelect(i, SELECT_BY_POS, MODE_HISTORY)) { if (OrderSymbol() != sym || OrderMagicNumber() != InpMagic) continue; if (OrderProfit() >= 0) break; ls++; } }\n")
                sb.append("   return MathMin(InpMM_Base * MathPow(InpMM_Mx, MathMin(ls, InpMM_Ms)), InpMM_MaxLot);\n")
            }
            MoneyManagement.ATR_BASED -> {
                sb.append("   double atr = iATR(sym, 0, InpMM_AP, 0);\n")
                sb.append("   double riskAmt = AccountEquity() * InpMM_AR;\n")
                sb.append("   double lot = NormalizeDouble(riskAmt / (atr * MarketInfo(sym, MODE_TICKVALUE) * 10), 2);\n")
                sb.append("   return MathMin(lot, InpMM_MaxLot);\n")
            }
            MoneyManagement.GRID -> sb.append("   return MathMin(InpMM_GLot, InpMM_MaxLot);\n")
            MoneyManagement.PYRAMID -> {
                sb.append("   double base = MarketInfo(sym, MODE_MINLOT);\n")
                sb.append("   for (int i = OrdersTotal() - 1; i >= 0; i--) { if (OrderSelect(i, SELECT_BY_POS) && OrderSymbol() == sym && OrderMagicNumber() == InpMagic && OrderType() <= OP_SELL) { base = OrderLots() + InpMM_Add; break; } }\n")
                sb.append("   return MathMin(base, InpMM_MaxLot);\n")
            }
            MoneyManagement.KELLY -> {
                sb.append("   double f = InpMM_KW - (1.0 - InpMM_KW) / InpMM_KR;\n")
                sb.append("   if (f <= 0) return MarketInfo(sym, MODE_MINLOT);\n")
                sb.append("   double lot = AccountEquity() * f / 10000; return MathMin(NormalizeDouble(lot, 2), InpMM_MaxLot);\n")
            }
        }
        sb.append("}\n")
    }

    fun appendSignalFunc(sb: StringBuilder, c: StrategyConfig) {
        sb.append("int GetSignal(string sym) {\n")
        if (c.entries.isEmpty()) { sb.append("   return 0;\n}\n\n"); return }

        // v6: Signal Fusion mode
        if (c.signalFusion.enabled) {
            sb.append("   if (InpFusion) {\n")
            sb.append("      double score = 0.0, totalW = 0.0;\n")
            c.entries.forEachIndexed { j, e ->
                val w = c.signalFusion.weights.getOrNull(j)?.weight ?: 1.0
                val n = e.id
                when (e.indicator) {
                    IndicatorType.ICUSTOM -> sb.append("      if (iCustom${e.id}(sym)) { score += $w; totalW += $w; }\n")
                    else -> sb.append("      if (EvalEntry${e.id}(sym)) { score += $w; totalW += $w; }\n")
                }
            }
            sb.append("      if (totalW == 0) return 0;\n")
            sb.append("      double norm = score / totalW;\n")
            sb.append("      if (InpF_BuyTh + InpF_SellTh == 0) return 0;\n")
            sb.append("      double buyRatio = InpF_BuyTh / (InpF_BuyTh + InpF_SellTh);\n")
            sb.append("      double sellRatio = InpF_SellTh / (InpF_BuyTh + InpF_SellTh);\n")
            sb.append("      if (norm >= buyRatio) return 1;\n")
            sb.append("      if (norm <= 1.0 - sellRatio) return -1;\n")
            sb.append("      return 0;\n   }\n\n")
        }

        var condIdx = 0
        for (e in c.entries) {
            condIdx++
            when (e.indicator) {
                IndicatorType.ICUSTOM -> sb.append("   bool c$condIdx = iCustom${e.id}(sym);\n")
                else -> sb.append("   bool c$condIdx = EvalEntry${e.id}(sym);\n")
            }
        }
        condIdx = 0
        for (e in c.entries) {
            condIdx++
            if (condIdx == 1) {
                when (e.direction) { "BuyOnly" -> sb.append("   bool buyOk = c$condIdx, sellOk = false;\n"); "SellOnly" -> sb.append("   bool buyOk = false, sellOk = c$condIdx;\n"); else -> sb.append("   bool buyOk = c$condIdx, sellOk = c$condIdx;\n") }
            } else {
                when (e.direction) { "BuyOnly" -> sb.append("   buyOk = buyOk ${e.logicOp.symbol} c$condIdx;\n"); "SellOnly" -> sb.append("   sellOk = sellOk ${e.logicOp.symbol} c$condIdx;\n"); else -> sb.append("   buyOk = buyOk ${e.logicOp.symbol} c$condIdx; sellOk = sellOk ${e.logicOp.symbol} c$condIdx;\n") }
            }
        }
        sb.append("   if (buyOk && !sellOk) return 1;\n   if (sellOk && !buyOk) return -1;\n   return 0;\n}\n\n")

        c.entries.forEach { e ->
            val n = e.id
            when (e.indicator) {
                IndicatorType.ICUSTOM -> {
                    sb.append("bool iCustom${e.id}(string sym) { double v = iCustom(sym, 0, InpE${n}_Indi${if (e.customIndiParams.isNotBlank()) ", ${e.customIndiParams}" else ""}, InpE${n}_Buf, 0); return v > InpE${n}_Sig; }\n\n")
                }
                else -> {
                    sb.append("bool EvalEntry${e.id}(string sym) {\n")
                    when (e.indicator) {
                        IndicatorType.MA -> {
                            sb.append("   double f1 = iMA(sym, 0, InpE${n}_F, 0, InpE${n}_MAM, PRICE_CLOSE, 1);\n   double s1 = iMA(sym, 0, InpE${n}_S, 0, InpE${n}_MAM, PRICE_CLOSE, 1);\n   double f0 = iMA(sym, 0, InpE${n}_F, 0, InpE${n}_MAM, PRICE_CLOSE, 0);\n   double s0 = iMA(sym, 0, InpE${n}_S, 0, InpE${n}_MAM, PRICE_CLOSE, 0);\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (f1 <= s1 && f0 > s0);\n"); "SellOnly" -> sb.append("   return (f1 >= s1 && f0 < s0);\n"); else -> sb.append("   return (f1 <= s1 && f0 > s0) || (f1 >= s1 && f0 < s0);\n") }
                        }
                        IndicatorType.PRICE -> {
                            sb.append("   double ma = iMA(sym, 0, InpE${n}_F, 0, InpE${n}_MAM, PRICE_CLOSE, 0);\n")
                            sb.append("   double p1 = iClose(sym, 0, 1); double p0 = iClose(sym, 0, 0);\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (p1 < ma && p0 > ma);\n"); "SellOnly" -> sb.append("   return (p1 > ma && p0 < ma);\n"); else -> sb.append("   return (p1 < ma && p0 > ma) || (p1 > ma && p0 < ma);\n") }
                        }
                        IndicatorType.RSI -> {
                            sb.append("   double r = iRSI(sym, 0, InpE${n}_P, PRICE_CLOSE, 0);\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (r < InpE${n}_OS);\n"); "SellOnly" -> sb.append("   return (r > InpE${n}_OB);\n"); else -> sb.append("   return (r < InpE${n}_OS) || (r > InpE${n}_OB);\n") }
                        }
                        IndicatorType.RSI -> {
                            sb.append("   double r0 = iRSI(sym, 0, InpE${n}_P, PRICE_CLOSE, 0);\n   double rL = iRSI(sym, 0, InpE${n}_P, PRICE_CLOSE, InpE${n}_DivLB);\n")
                            sb.append("   double hh = High[iHighest(sym, 0, MODE_HIGH, InpE${n}_DivLB, 1)]; double ll = Low[iLowest(sym, 0, MODE_LOW, InpE${n}_DivLB, 1)];\n")
                            sb.append("   return (Ask > hh && r0 < rL) || (Bid < ll && r0 > rL);\n")
                        }
                        IndicatorType.MACD -> {
                            sb.append("   double m1 = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_MAIN, 1);\n   double g1 = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_SIGNAL, 1);\n   double m0 = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_MAIN, 0);\n   double g0 = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_SIGNAL, 0);\n   return (m1 <= g1 && m0 > g0) || (m1 >= g1 && m0 < g0);\n")
                        }
                        IndicatorType.MACD -> {
                            sb.append("   double m0 = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_MAIN, 0);\n   double mL = iMACD(sym, 0, InpE${n}_F, InpE${n}_S, InpE${n}_Sig, PRICE_CLOSE, MODE_MAIN, InpE${n}_DivLB);\n")
                            sb.append("   double hh = High[iHighest(sym, 0, MODE_HIGH, InpE${n}_DivLB, 1)]; double ll = Low[iLowest(sym, 0, MODE_LOW, InpE${n}_DivLB, 1)];\n")
                            sb.append("   return (Ask > hh && m0 < mL) || (Bid < ll && m0 > mL);\n")
                        }
                        IndicatorType.BOLLINGER -> {
                            sb.append("   double u = iBands(sym, 0, InpE${n}_P, InpE${n}_Dv, 0, PRICE_CLOSE, MODE_UPPER, 0);\n   double l = iBands(sym, 0, InpE${n}_P, InpE${n}_Dv, 0, PRICE_CLOSE, MODE_LOWER, 0);\n   return (Close[0] <= l) || (Close[0] >= u);\n")
                        }
                        IndicatorType.PRICE -> {
                            sb.append("   double hh = High[iHighest(sym, 0, MODE_HIGH, InpE${n}_LB, 1)]; double ll = Low[iLowest(sym, 0, MODE_LOW, InpE${n}_LB, 1)];\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (Ask > hh);\n"); "SellOnly" -> sb.append("   return (Bid < ll);\n"); else -> sb.append("   return (Ask > hh) || (Bid < ll);\n") }
                        }
                        IndicatorType.ICHIMOKU -> {
                            sb.append("   double t1 = iIchimoku(sym, 0, InpE${n}_T, InpE${n}_K, InpE${n}_S, MODE_TENKANSEN, 1);\n")
                            sb.append("   double k1 = iIchimoku(sym, 0, InpE${n}_T, InpE${n}_K, InpE${n}_S, MODE_KIJUNSEN, 1);\n")
                            sb.append("   double t0 = iIchimoku(sym, 0, InpE${n}_T, InpE${n}_K, InpE${n}_S, MODE_TENKANSEN, 0);\n")
                            sb.append("   double k0 = iIchimoku(sym, 0, InpE${n}_T, InpE${n}_K, InpE${n}_S, MODE_KIJUNSEN, 0);\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (t1 <= k1 && t0 > k0);\n"); "SellOnly" -> sb.append("   return (t1 >= k1 && t0 < k0);\n"); else -> sb.append("   return (t1 <= k1 && t0 > k0) || (t1 >= k1 && t0 < k0);\n") }
                        }
                        IndicatorType.ALLIGATOR -> {
                            sb.append("   double j0 = iAlligator(sym, 0, InpE${n}_J, 13, InpE${n}_T, 8, InpE${n}_L, 5, MODE_GATORJAW, MODE_SSMA, PRICE_MEDIAN, 0);\n")
                            sb.append("   double t0 = iAlligator(sym, 0, InpE${n}_J, 13, InpE${n}_T, 8, InpE${n}_L, 5, MODE_GATORTEETH, MODE_SSMA, PRICE_MEDIAN, 0);\n")
                            sb.append("   double l0 = iAlligator(sym, 0, InpE${n}_J, 13, InpE${n}_T, 8, InpE${n}_L, 5, MODE_GATORLIPS, MODE_SSMA, PRICE_MEDIAN, 0);\n")
                            sb.append("   double p0 = iClose(sym, 0, 0);\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (p0 > j0 && p0 > t0 && p0 > l0);\n"); "SellOnly" -> sb.append("   return (p0 < j0 && p0 < t0 && p0 < l0);\n"); else -> sb.append("   return (p0 > j0 && p0 > t0 && p0 > l0) || (p0 < j0 && p0 < t0 && p0 < l0);\n") }
                        }
                        IndicatorType.CANDLE_PATTERN -> {
                            sb.append("   double o = iOpen(sym, 0, 0); double h = iHigh(sym, 0, 0); double l = iLow(sym, 0, 0); double c = iClose(sym, 0, 0);\n")
                            sb.append("   double range = MathMax(h - l, Point); double body = MathAbs(c - o);\n")
                            when (e.candlePattern.lowercase()) {
                                "doji" -> sb.append("   return (body < range * 0.1);\n")
                                "hammer" -> sb.append("   double upS = h - MathMax(o,c), loS = MathMin(o,c) - l; return (loS > body * 2 && upS < body * 0.3);\n")
                                "shooting_star" -> sb.append("   double upS = h - MathMax(o,c), loS = MathMin(o,c) - l; return (upS > body * 2 && loS < body * 0.3);\n")
                                "engulfing" -> sb.append("   double o1 = iOpen(sym, 0, 1); double c1 = iClose(sym, 0, 1); return ((c1 < o1 && c > o && o < c1 && c > o1) || (c1 > o1 && c < o && o > c1 && c < o1));\n")
                                "marubozu" -> sb.append("   return (body > range * 0.9);\n")
                                else -> sb.append("   return false;\n")
                            }
                        }
                        IndicatorType.VOLUME -> {
                            sb.append("   double v = iVolume(sym, 0, 0);\n")
                            sb.append("   double vAvg = 0; for (int vi = 1; vi <= 20; vi++) vAvg += iVolume(sym, 0, vi); vAvg /= 20;\n")
                            when (e.direction) { "BuyOnly" -> sb.append("   return (v > vAvg * InpE${n}_VT);\n"); "SellOnly" -> sb.append("   return (v > vAvg * InpE${n}_VT);\n"); else -> sb.append("   return (v > vAvg * InpE${n}_VT);\n") }
                        }
                        else -> sb.append("   return false;\n")
                    }
                    sb.append("}\n\n")
                }
            }
        }
    }

    fun appendExitFunc(sb: StringBuilder, c: StrategyConfig) {
        sb.append("void ManageExits(string sym) {\n")
        sb.append("   for (int i = OrdersTotal() - 1; i >= 0; i--) {\n")
        sb.append("      if (!OrderSelect(i, SELECT_BY_POS)) continue;\n")
        sb.append("      if (OrderSymbol() != sym || OrderMagicNumber() != InpMagic) continue;\n")
        sb.append("      int dir = OrderType(); double op = OrderOpenPrice();\n")
        sb.append("      double cSL = OrderStopLoss(), cTP = OrderTakeProfit();\n")
        sb.append("      int tkt = OrderTicket();\n")
        c.exits.forEach { ex ->
            val n = ex.id
            when (ex.exitType) {
                ExitType.TRAILING -> {
                    sb.append("      if (dir == OP_BUY && Bid - op > InpX${n}_TS * Point) { double ns = Bid - InpX${n}_Tp * Point; if (ns > cSL || cSL == 0) OrderModify(tkt, op, ns, cTP, 0); }\n")
                    sb.append("      if (dir == OP_SELL && op - Ask > InpX${n}_TS * Point) { double ns = Ask + InpX${n}_Tp * Point; if (ns < cSL || cSL == 0) OrderModify(tkt, op, ns, cTP, 0); }\n")
                }
                ExitType.BREAKEVEN -> {
                    sb.append("      if (dir == OP_BUY && Bid - op > InpX${n}_BP * Point) { double ns = op + InpX${n}_BL * Point; if (ns > cSL || cSL == 0) OrderModify(tkt, op, ns, cTP, 0); }\n")
                    sb.append("      if (dir == OP_SELL && op - Ask > InpX${n}_BP * Point) { double ns = op - InpX${n}_BL * Point; if (ns < cSL || cSL == 0) OrderModify(tkt, op, ns, cTP, 0); }\n")
                }
                ExitType.TIME_EXIT -> sb.append("      if (TimeCurrent() - OrderOpenTime() > InpX${n}_Min * 60) OrderClose(tkt, OrderLots(), dir == OP_BUY ? Bid : Ask, InpSlippage);\n")
                ExitType.PARTIAL_CLOSE -> {
                    ex.partialTPs.forEachIndexed { j, pTP ->
                        sb.append("      static bool p${n}_${j}[500];\n")
                        sb.append("      if (dir == OP_BUY && Bid - op > InpX${n}_TP${j+1} * Point && !p${n}_${j}[i]) { double v = NormalizeDouble(OrderLots() * InpX${n}_PC${j+1} / 100, 2); if (v >= MarketInfo(sym, MODE_MINLOT)) OrderClose(tkt, v, Bid, InpSlippage); p${n}_${j}[i] = true; if (InpX${n}_BE${j+1}) OrderModify(tkt, op, op, cTP, 0); }\n")
                        sb.append("      if (dir == OP_SELL && op - Ask > InpX${n}_TP${j+1} * Point && !p${n}_${j}[i]) { double v = NormalizeDouble(OrderLots() * InpX${n}_PC${j+1} / 100, 2); if (v >= MarketInfo(sym, MODE_MINLOT)) OrderClose(tkt, v, Ask, InpSlippage); p${n}_${j}[i] = true; if (InpX${n}_BE${j+1}) OrderModify(tkt, op, op, cTP, 0); }\n")
                    }
                }
                ExitType.TRAILING_PROFIT -> {
                    sb.append("      if (dir == OP_BUY && Bid - op > InpX${n}_PTS * Point) { double tpSl = Bid - InpX${n}_PTP * Point; if (tpSl > cSL || cSL == 0) OrderModify(tkt, op, tpSl, cTP, 0); }\n")
                    sb.append("      if (dir == OP_SELL && op - Ask > InpX${n}_PTS * Point) { double tpSl = Ask + InpX${n}_PTP * Point; if (tpSl < cSL || cSL == 0) OrderModify(tkt, op, tpSl, cTP, 0); }\n")
                }
                ExitType.MA_SL -> {
                    sb.append("      double ma = iMA(sym, 0, InpX${n}_MP, 0, MODE_SMA, PRICE_CLOSE, 0);\n")
                    sb.append("      if (dir == OP_BUY && Bid < ma) OrderClose(tkt, OrderLots(), Bid, InpSlippage);\n")
                    sb.append("      if (dir == OP_SELL && Ask > ma) OrderClose(tkt, OrderLots(), Ask, InpSlippage);\n")
                }
                else -> {}
            }
        }
        sb.append("   }\n}\n\n")
        sb.append("void CalcSLTP(bool isBuy, double ep, double &sl, double &tp) {\n")
        c.exits.forEach { ex ->
            when (ex.exitType) { ExitType.FIXED_SLTP -> sb.append("   sl = InpX${ex.id}_SL > 0 ? (isBuy ? ep - InpX${ex.id}_SL * Point : ep + InpX${ex.id}_SL * Point) : 0;\n   tp = InpX${ex.id}_TP > 0 ? (isBuy ? ep + InpX${ex.id}_TP * Point : ep - InpX${ex.id}_TP * Point) : 0;\n") else -> {} }
        }
        sb.append("}\n")
    }

    fun appendPanel(sb: StringBuilder, c: StrategyConfig) {
        sb.append("void UpdatePanel(string sym) {\n")
        sb.append("   int n = CountPositions(sym);\n")
        if (c.showStats) {
            sb.append("   double wr = g_trades > 0 ? g_wins * 100.0 / g_trades : 0;\n")
            sb.append("   double dd = g_maxEq > 0 ? (g_maxEq - AccountEquity()) / g_maxEq * 100 : 0;\n")
            sb.append("   string s = \" 持仓:\" + IntegerToString(n) + \" 胜率:\" + DoubleToString(wr,0) + \"% 盈亏:\" + DoubleToString(g_profit,2) + \" DD:\" + DoubleToString(dd,1) + \"%\";\n")
            sb.append("   Comment(s);\n")
        } else {
            sb.append("   Comment(\"持仓:\" + IntegerToString(n));\n")
        }
        sb.append("}\n")
    }

    fun appendStats(sb: StringBuilder, c: StrategyConfig) {
        if (!c.showStats) return
        sb.append("void OnTrade() {\n")
        sb.append("   for (int i = OrdersHistoryTotal() - 1; i >= 0; i--) { if (OrderSelect(i, SELECT_BY_POS, MODE_HISTORY) && OrderMagicNumber() == InpMagic && OrderType() <= OP_SELL) { double p = OrderProfit(); g_profit += p; g_dailyPL += p; if (p > 0) { g_wins++; g_consLoss = 0; } else g_consLoss++; break; } }\n}\n")
    }

    fun appendFooter(sb: StringBuilder) { sb.append("//+------------------------------------------------------------------+\n") }
}