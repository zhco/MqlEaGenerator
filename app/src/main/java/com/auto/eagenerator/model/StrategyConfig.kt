package com.auto.eagenerator.model

enum class IndicatorType(val label: String) {
    MA("MA均线"),
    RSI("RSI"),
    STOCH("Stochastic"),
    MACD("MACD"),
    BOLLINGER("布林带"),
    ADX("ADX"),
    SAR("Parabolic SAR"),
    CCI("CCI"),
    ICHIMOKU("一目均衡"),
    ALLIGATOR("鳄鱼线"),
    ATR("ATR"),
    CANDLE_PATTERN("K线形态"),
    VOLUME("成交量"),
    PRICE("K线价格"),
    CUSTOM_EXPRESSION("自定义表达式"),
    ICUSTOM("自定义指标"),
}

enum class ComparisonOp(val symbol: String, val label: String, val isCross: Boolean = false) {
    GT(">", "大于"),
    LT("<", "小于"),
    GTE(">=", "大于等于"),
    LTE("<=", "小于等于"),
    EQ("==", "等于"),
    CROSS_ABOVE("crosses_above", "上穿", true),
    CROSS_BELOW("crosses_below", "下穿", true),
}

enum class TargetType(val label: String) {
    FIXED("固定值"),
    PRICE("K线价格"),
    INDICATOR("另一指标"),
}

enum class ExitType(val label: String) {
    FIXED_SLTP("固定SL/TP"),
    TRAILING("移动止损"),
    ATR_SL("ATR止损"),
    MA_SL("均线止损"),
    INDICATOR_EXIT("指标出场"),
    TIME_EXIT("时间出场"),
    BREAKEVEN("保本止损"),
    TRAILING_PROFIT("移动止盈"),
    PARTIAL_CLOSE("多级部分平仓"),
}

enum class MoneyManagement(val label: String) {
    FIXED_LOT("固定手数"),
    RISK_PERCENT("风险百分比"),
    MARTINGALE("马丁格尔"),
    GRID("网格交易"),
    PYRAMID("金字塔"),
    ATR_BASED("ATR仓位"),
    KELLY("凯利公式"),
}

enum class EntryOrderType(val label: String) { MARKET("市价"), LIMIT("限价"), STOP("突破") }
enum class SessionType(val label: String) { ASIAN("亚洲"), LONDON("伦敦"), NY("纽约") }
enum class NewsImportance(val label: String) { LOW("低"), MEDIUM("中"), HIGH("高") }

data class PartialTP(val tpPoints: Int = 300, val closePercent: Double = 50.0, val moveSLToBE: Boolean = true)

data class EntryCondition(
    val id: Int = 0,
    val indicator: IndicatorType = IndicatorType.MA,
    val comparison: ComparisonOp = ComparisonOp.CROSS_ABOVE,
    val srcBuffer: Int = 0,         // 信号源缓冲区 (0=主线/当前值)
    val targetType: TargetType = TargetType.FIXED,
    val targetPrice: String = "Close",  // 目标K线价格(Close/Open/High/Low)
    val targetIndicator: IndicatorType = IndicatorType.MA,  // 目标指标
    val targetBuffer: Int = 0,      // 目标指标缓冲区
    val targetFixed: Double = 0.0,  // 目标固定值
    val logicOp: LogicOp = LogicOp.AND,
    val timeframe: String = "CURRENT",
    val entryType: EntryOrderType = EntryOrderType.MARKET,
    val limitOffset: Int = 50, val stopOffset: Int = 50,
    val fastPeriod: Int = 5, val slowPeriod: Int = 20,
    val maMethod: String = "SMA", val appliedPrice: String = "Close",
    val obLevel: Double = 70.0, val osLevel: Double = 30.0, val period: Int = 14,
    val kPeriod: Int = 5, val dPeriod: Int = 3, val slowing: Int = 3,
    val bbPeriod: Int = 20, val bbDeviation: Double = 2.0,
    val adxLevel: Double = 25.0,
    val sarStep: Double = 0.02, val sarMax: Double = 0.2,
    val tenkan: Int = 9, val kijun: Int = 26, val senkou: Int = 52,
    val jawPeriod: Int = 13, val teethPeriod: Int = 8, val lipsPeriod: Int = 5,
    val atrPeriod: Int = 14, val atrMultiplier: Double = 1.5,
    val lookbackBars: Int = 20,
    val candlePattern: String = "Engulfing",
    val volumeThreshold: Double = 1.5,
    val customExpression: String = "",
    val customIndiName: String = "", val customIndiParams: String = "",
    val customIndiBuffer: Int = 0, val customIndiSignal: Double = 0.0,
    val direction: String = "Both",
    val multiTF: MultiTFConfig = MultiTFConfig(),
)

enum class LogicOp(val symbol: String) { AND("&&"), OR("||") }

data class ExitRule(
    val id: Int = 0, val exitType: ExitType = ExitType.FIXED_SLTP,
    val slPoints: Int = 300, val tpPoints: Int = 600,
    val trailingStart: Int = 200, val trailingStep: Int = 50,
    val atrMultSL: Double = 2.0, val atrPeriodSL: Int = 14,
    val breakevenPips: Int = 100, val breakevenLock: Int = 10,
    val exitMinutes: Int = 240,
    val tpTrailingStart: Int = 300, val tpTrailingStep: Int = 100,
    val maExitPeriod: Int = 20,
    val exitIndicator: IndicatorType = IndicatorType.RSI,
    val exitIndicatorPeriod: Int = 14,
    val exitObLevel: Double = 70.0, val exitOsLevel: Double = 30.0,
    val partialTPs: List<PartialTP> = listOf(PartialTP(300, 50.0), PartialTP(600, 50.0)),
)

data class MoneyRule(
    val mmType: MoneyManagement = MoneyManagement.FIXED_LOT,
    val fixedLot: Double = 0.01, val riskPercent: Double = 2.0, val maxLot: Double = 1.0,
    val martinMultiplier: Double = 2.0, val martinMaxSteps: Int = 5, val martinStepPips: Int = 200,
    val gridLevels: Int = 5, val gridSpacing: Int = 200, val gridLot: Double = 0.01,
    val pyramidLotAdd: Double = 0.01, val pyramidProfitPips: Int = 100,
    val atrLotPeriod: Int = 14, val atrRiskPerN: Double = 0.02,
    val kellyWinRate: Double = 0.5, val kellyWinLossRatio: Double = 1.5,
)

data class SessionFilter(
    val enabled: Boolean = false,
    val asian: Boolean = false, val asianStart: Int = 0, val asianEnd: Int = 9,
    val london: Boolean = true, val londonStart: Int = 8, val londonEnd: Int = 17,
    val ny: Boolean = false, val nyStart: Int = 13, val nyEnd: Int = 22,
    val monday: Boolean = true, val tuesday: Boolean = true, val wednesday: Boolean = true,
    val thursday: Boolean = true, val friday: Boolean = true,
)

data class NewsFilter(
    val enabled: Boolean = false,
    val importance: NewsImportance = NewsImportance.HIGH,
    val beforeMinutes: Int = 60, val afterMinutes: Int = 30,
    val onlyRedEvents: Boolean = true,
)

data class EquityProtection(
    val enabled: Boolean = false,
    val maxDrawdownPct: Double = 20.0,
    val dailyLossLimit: Double = 5.0,
    val dailyProfitTarget: Double = 0.0,
    val consecutiveLosses: Int = 0,
    val stopAction: String = "CloseAll", // CloseAll / StopNew
)

data class Notifications(
    val enabled: Boolean = false,
    val onTrade: Boolean = true, val onSignal: Boolean = false,
    val onError: Boolean = true, val onDaily: Boolean = true,
    val telegram: TelegramConfig = TelegramConfig(),
    val email: EmailConfig = EmailConfig(),
)

data class FilterRule(
    val useTimeFilter: Boolean = false, val startHour: Int = 8, val endHour: Int = 22,
    val maxSpread: Int = 0, val maxPositions: Int = 3,
    val maxDailyTrades: Int = 0, val cooldownBars: Int = 0,
)

data class StrategyConfig(
    val mqlVersion: String = "MQL5", val strategyName: String = "AutoEA",
    val magicNumber: Int = 20250101, val slippage: Int = 3,
    val entries: List<EntryCondition> = listOf(EntryCondition(id = 1)),
    val exits: List<ExitRule> = listOf(ExitRule(id = 1)),
    val money: MoneyRule = MoneyRule(),
    val filter: FilterRule = FilterRule(),
    val enableHedging: Boolean = false,
    val multiSymbol: Boolean = false,
    val symbols: List<String> = listOf("EURUSD"),
    val sessionFilter: SessionFilter = SessionFilter(),
    val newsFilter: NewsFilter = NewsFilter(),
    val equityProtection: EquityProtection = EquityProtection(),
    val notifications: Notifications = Notifications(),
    // v6: 策略深度
    val signalFusion: SignalFusion = SignalFusion(),
    val correlationFilter: CorrelationFilter = CorrelationFilter(),
    // v6: MQL5云端优化就绪标志
    val mql5CloudReady: Boolean = false,
    val showPanel: Boolean = true,
    val showStats: Boolean = true,
    val panelBg: String = "C'18,18,24'", val panelText: String = "clrWhite",
    val buyColor: String = "C'0,200,83'", val sellColor: String = "C'255,82,82'",
)

// ─── v6: Multi-TF 确认 ───
data class MultiTFConfig(
    val enabled: Boolean = false,
    val confirmTF: String = "H4",       // 高时间框架
    val confirmBars: Int = 3,           // 确认K线数
    val requireBoth: Boolean = true,    // 要求双向确认
)

// ─── v6: AI信号融合 ───
data class SignalWeight(
    val indicator: IndicatorType = IndicatorType.MA,
    val weight: Double = 1.0,
    val minStrength: Double = 0.0,
)
data class SignalFusion(
    val enabled: Boolean = false,
    val weights: List<SignalWeight> = listOf(
        SignalWeight(IndicatorType.MA, 1.0),
        SignalWeight(IndicatorType.RSI, 1.0),
    ),
    val buyThreshold: Double = 1.5,
    val sellThreshold: Double = 1.5,
    val useNormalization: Boolean = true,
)

// ─── v6: 品种相关性过滤 ───
data class CorrelationFilter(
    val enabled: Boolean = false,
    val maxCorrelation: Double = 0.8,
    val lookbackBars: Int = 100,
    val excludeList: List<String> = emptyList(),
)

// ─── v6: 通知配置 ───
data class TelegramConfig(
    val enabled: Boolean = false,
    val botToken: String = "",
    val chatId: String = "",
)
data class EmailConfig(
    val enabled: Boolean = false,
    val smtpServer: String = "smtp.gmail.com",
    val smtpPort: Int = 587,
    val username: String = "", val password: String = "",
    val toEmail: String = "",
)

// ─── v6: 遗传算法优化 ───
data class GeneticOptConfig(
    val enabled: Boolean = false,
    val popSize: Int = 50, val generations: Int = 20,
    val mutationRate: Double = 0.1, val crossoverRate: Double = 0.7,
    val eliteCount: Int = 3, val tournamentSize: Int = 4,
    val target: String = "Sharpe",  // Sharpe / NetProfit / WinRate / ProfitFactor
)

// ─── v6: 蒙特卡洛 ───
data class MonteCarloConfig(
    val enabled: Boolean = false,
    val simulations: Int = 200,
    val resampleRatio: Double = 0.8,  // 每次采样比例
    val confidenceLevel: Double = 95.0, // 置信度
)

// ─── v6: 前进式优化 ───
data class WalkForwardConfig(
    val enabled: Boolean = false,
    val inSamplePct: Double = 60.0,  // 样本内比例
    val outSamplePct: Double = 40.0,  // 样本外比例
    val stepSize: Int = 500,           // 步进K线数
)

// ─── v6: 策略模板 ───
data class StrategyTemplate(
    val name: String, val description: String,
    val category: String = "",       // Trend/MeanReversion/Breakout/Scalping/MultiIndi
    val mqlVersion: String = "MQL5", val entries: List<EntryCondition> = emptyList(),
    val exits: List<ExitRule> = emptyList(), val money: MoneyRule = MoneyRule(),
    val config: String = "",          // JSON序列化的StrategyConfig
    val tags: List<String> = emptyList(),
    val difficulty: String = "中级",  // 初级/中级/高级
    val timeframes: List<String> = listOf("H1"),
    val bestMarket: String = "趋势市场",
)
