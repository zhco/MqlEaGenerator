package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON序列化/反序列化工具
 * 策略配置导入导出
 */
object JsonSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun serialize(config: StrategyConfig): String {
        val dto = toDTO(config)
        return json.encodeToString(dto)
    }

    fun deserialize(jsonStr: String): StrategyConfig {
        val dto = json.decodeFromString<ConfigDTO>(jsonStr)
        return fromDTO(dto)
    }

    fun templateToConfig(template: StrategyTemplate): StrategyConfig {
        return deserialize(template.config).copy(strategyName = template.name)
    }

    // ── DTO (Serializable) ──
    @Serializable
    data class ConfigDTO(
        val mqlVersion: String = "MQL5",
        val strategyName: String = "AutoEA",
        val magicNumber: Int = 20250101,
        val slippage: Int = 3,
        val entries: List<EntryDTO> = listOf(EntryDTO()),
        val exits: List<ExitDTO> = listOf(ExitDTO()),
        val money: MoneyDTO = MoneyDTO(),
        val filter: FilterDTO = FilterDTO(),
        val enableHedging: Boolean = false,
        val multiSymbol: Boolean = false,
        val symbols: List<String> = listOf("EURUSD"),
        val sessionFilter: SessionDTO = SessionDTO(),
        val newsFilter: NewsDTO = NewsDTO(),
        val equityProtection: EquityDTO = EquityDTO(),
        val notifications: NotifyDTO = NotifyDTO(),
        val signalFusion: FusionDTO = FusionDTO(),
        val correlationFilter: CorrDTO = CorrDTO(),
        val mql5CloudReady: Boolean = false,
        val showPanel: Boolean = true,
        val showStats: Boolean = true,
        val panelBg: String = "C'18,18,24'",
        val panelText: String = "clrWhite",
        val buyColor: String = "C'0,200,83'",
        val sellColor: String = "C'255,82,82'",
    )

    @Serializable data class EntryDTO(
        val indicator: String = "MA_CROSS", val logicOp: String = "AND",
        val timeframe: String = "CURRENT", val entryType: String = "MARKET",
        val fastPeriod: Int = 5, val slowPeriod: Int = 20, val midPeriod: Int = 50,
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
        val candlePattern: String = "Engulfing", val volumeThreshold: Double = 1.5,
        val divLookback: Int = 20, val divMinStrength: Int = 3,
        val customExpression: String = "", val customIndiName: String = "",
        val customIndiParams: String = "", val customIndiBuffer: Int = 0,
        val customIndiSignal: Double = 0.0, val direction: String = "Both",
        val multiTF: MultiTfDTO = MultiTfDTO(),
    )

    @Serializable data class MultiTfDTO(
        val enabled: Boolean = false, val confirmTF: String = "H1",
        val confirmBars: Int = 3, val requireBoth: Boolean = true,
    )
    @Serializable data class ExitDTO(
        val exitType: String = "FIXED_SLTP", val slPoints: Int = 300, val tpPoints: Int = 600,
        val trailingStart: Int = 200, val trailingStep: Int = 50,
        val atrMultSL: Double = 2.0, val atrPeriodSL: Int = 14,
        val breakevenPips: Int = 100, val breakevenLock: Int = 10,
        val exitMinutes: Int = 240,
        val tpTrailingStart: Int = 300, val tpTrailingStep: Int = 100,
        val maExitPeriod: Int = 20,
        val exitIndicator: String = "RSI", val exitIndicatorPeriod: Int = 14,
        val exitObLevel: Double = 70.0, val exitOsLevel: Double = 30.0,
        val partialTPs: List<PartialTpDTO> = emptyList(),
    )
    @Serializable data class PartialTpDTO(val tpPoints: Int = 300, val closePercent: Double = 50.0, val moveSLToBE: Boolean = true)
    @Serializable data class MoneyDTO(
        val mmType: String = "FIXED_LOT", val fixedLot: Double = 0.01, val riskPercent: Double = 2.0,
        val maxLot: Double = 1.0, val martinMultiplier: Double = 2.0, val martinMaxSteps: Int = 5,
        val martinStepPips: Int = 200, val gridLevels: Int = 5, val gridSpacing: Int = 200,
        val gridLot: Double = 0.01, val pyramidLotAdd: Double = 0.01, val pyramidProfitPips: Int = 100,
        val atrLotPeriod: Int = 14, val atrRiskPerN: Double = 0.02,
        val kellyWinRate: Double = 0.5, val kellyWinLossRatio: Double = 1.5,
    )
    @Serializable data class FilterDTO(
        val useTimeFilter: Boolean = false, val startHour: Int = 8, val endHour: Int = 22,
        val maxSpread: Int = 0, val maxPositions: Int = 3,
        val maxDailyTrades: Int = 0, val cooldownBars: Int = 0,
    )
    @Serializable data class SessionDTO(
        val enabled: Boolean = false,
        val asian: Boolean = false, val asianStart: Int = 0, val asianEnd: Int = 9,
        val london: Boolean = true, val londonStart: Int = 8, val londonEnd: Int = 17,
        val ny: Boolean = false, val nyStart: Int = 13, val nyEnd: Int = 22,
        val monday: Boolean = true, val tuesday: Boolean = true, val wednesday: Boolean = true,
        val thursday: Boolean = true, val friday: Boolean = true,
    )
    @Serializable data class NewsDTO(
        val enabled: Boolean = false, val importance: String = "HIGH",
        val beforeMinutes: Int = 60, val afterMinutes: Int = 30, val onlyRedEvents: Boolean = true,
    )
    @Serializable data class EquityDTO(
        val enabled: Boolean = false, val maxDrawdownPct: Double = 20.0,
        val dailyLossLimit: Double = 5.0, val dailyProfitTarget: Double = 0.0,
        val consecutiveLosses: Int = 0, val stopAction: String = "CloseAll",
    )
    @Serializable data class NotifyDTO(
        val enabled: Boolean = false, val onTrade: Boolean = true, val onSignal: Boolean = false,
        val onError: Boolean = true, val onDaily: Boolean = true,
        val telegram: TelegramDTO = TelegramDTO(), val email: EmailDTO = EmailDTO(),
    )
    @Serializable data class TelegramDTO(
        val enabled: Boolean = false, val botToken: String = "", val chatId: String = ""
    )
    @Serializable data class EmailDTO(
        val enabled: Boolean = false, val smtpServer: String = "smtp.gmail.com",
        val smtpPort: Int = 587, val username: String = "", val password: String = "",
        val toEmail: String = "",
    )
    @Serializable data class FusionDTO(
        val enabled: Boolean = false,
        val weights: List<WeightDTO> = listOf(WeightDTO("MA_CROSS", 1.0), WeightDTO("RSI", 0.5)),
        val buyThreshold: Double = 1.5, val sellThreshold: Double = 1.5,
        val useNormalization: Boolean = true,
    )
    @Serializable data class WeightDTO(
        val indicator: String = "MA_CROSS", val weight: Double = 1.0, val minStrength: Double = 0.0,
    )
    @Serializable data class CorrDTO(
        val enabled: Boolean = false, val maxCorrelation: Double = 0.8,
        val lookbackBars: Int = 100, val excludeList: List<String> = emptyList(),
    )

    // ── Mapper ──
    private fun toDTO(c: StrategyConfig): ConfigDTO = ConfigDTO(
        mqlVersion = c.mqlVersion, strategyName = c.strategyName,
        magicNumber = c.magicNumber, slippage = c.slippage,
        entries = c.entries.map { toEntryDTO(it) },
        exits = c.exits.map { toExitDTO(it) },
        money = with(c.money) { MoneyDTO(mmType.name, fixedLot, riskPercent, maxLot, martinMultiplier, martinMaxSteps, martinStepPips, gridLevels, gridSpacing, gridLot, pyramidLotAdd, pyramidProfitPips, atrLotPeriod, atrRiskPerN, kellyWinRate, kellyWinLossRatio) },
        filter = with(c.filter) { FilterDTO(useTimeFilter, startHour, endHour, maxSpread, maxPositions, maxDailyTrades, cooldownBars) },
        enableHedging = c.enableHedging, multiSymbol = c.multiSymbol, symbols = c.symbols,
        sessionFilter = with(c.sessionFilter) { SessionDTO(enabled, asian, asianStart, asianEnd, london, londonStart, londonEnd, ny, nyStart, nyEnd, monday, tuesday, wednesday, thursday, friday) },
        newsFilter = with(c.newsFilter) { NewsDTO(enabled, importance.name, beforeMinutes, afterMinutes, onlyRedEvents) },
        equityProtection = with(c.equityProtection) { EquityDTO(enabled, maxDrawdownPct, dailyLossLimit, dailyProfitTarget, consecutiveLosses, stopAction) },
        notifications = with(c.notifications) { NotifyDTO(enabled, onTrade, onSignal, onError, onDaily, TelegramDTO(telegram.enabled, telegram.botToken, telegram.chatId), EmailDTO(email.enabled, email.smtpServer, email.smtpPort, email.username, email.password, email.toEmail)) },
        signalFusion = with(c.signalFusion) { FusionDTO(enabled, weights.map { WeightDTO(it.indicator.name, it.weight, it.minStrength) }, buyThreshold, sellThreshold, useNormalization) },
        correlationFilter = with(c.correlationFilter) { CorrDTO(enabled, maxCorrelation, lookbackBars, excludeList) },
        mql5CloudReady = c.mql5CloudReady,
        showPanel = c.showPanel, showStats = c.showStats,
        panelBg = c.panelBg, panelText = c.panelText, buyColor = c.buyColor, sellColor = c.sellColor,
    )

    private fun toEntryDTO(e: EntryCondition) = EntryDTO(
        indicator = e.indicator.name, logicOp = e.logicOp.symbol,
        timeframe = e.timeframe, entryType = e.entryType.name,
        fastPeriod = e.fastPeriod, slowPeriod = e.slowPeriod, midPeriod = e.midPeriod,
        maMethod = e.maMethod, appliedPrice = e.appliedPrice,
        obLevel = e.obLevel, osLevel = e.osLevel, period = e.period,
        kPeriod = e.kPeriod, dPeriod = e.dPeriod, slowing = e.slowing,
        bbPeriod = e.bbPeriod, bbDeviation = e.bbDeviation,
        adxLevel = e.adxLevel, sarStep = e.sarStep, sarMax = e.sarMax,
        tenkan = e.tenkan, kijun = e.kijun, senkou = e.senkou,
        jawPeriod = e.jawPeriod, teethPeriod = e.teethPeriod, lipsPeriod = e.lipsPeriod,
        atrPeriod = e.atrPeriod, atrMultiplier = e.atrMultiplier,
        lookbackBars = e.lookbackBars, candlePattern = e.candlePattern,
        volumeThreshold = e.volumeThreshold, divLookback = e.divLookback,
        divMinStrength = e.divMinStrength, customExpression = e.customExpression,
        customIndiName = e.customIndiName, customIndiParams = e.customIndiParams,
        customIndiBuffer = e.customIndiBuffer, customIndiSignal = e.customIndiSignal,
        direction = e.direction,
        multiTF = MultiTfDTO(e.multiTF.enabled, e.multiTF.confirmTF, e.multiTF.confirmBars, e.multiTF.requireBoth),
    )

    private fun toExitDTO(e: ExitRule) = ExitDTO(
        exitType = e.exitType.name,
        slPoints = e.slPoints, tpPoints = e.tpPoints,
        trailingStart = e.trailingStart, trailingStep = e.trailingStep,
        atrMultSL = e.atrMultSL, atrPeriodSL = e.atrPeriodSL,
        breakevenPips = e.breakevenPips, breakevenLock = e.breakevenLock,
        exitMinutes = e.exitMinutes,
        tpTrailingStart = e.tpTrailingStart, tpTrailingStep = e.tpTrailingStep,
        maExitPeriod = e.maExitPeriod,
        exitIndicator = e.exitIndicator.name, exitIndicatorPeriod = e.exitIndicatorPeriod,
        exitObLevel = e.exitObLevel, exitOsLevel = e.exitOsLevel,
        partialTPs = e.partialTPs.map { PartialTpDTO(it.tpPoints, it.closePercent, it.moveSLToBE) },
    )

    private fun fromDTO(d: ConfigDTO): StrategyConfig = StrategyConfig(
        mqlVersion = d.mqlVersion, strategyName = d.strategyName,
        magicNumber = d.magicNumber, slippage = d.slippage,
        entries = d.entries.map { fromEntryDTO(it) },
        exits = d.exits.map { fromExitDTO(it) },
        money = d.money.let { MoneyRule(tryEnum(it.mmType), it.fixedLot, it.riskPercent, it.maxLot, it.martinMultiplier, it.martinMaxSteps, it.martinStepPips, it.gridLevels, it.gridSpacing, it.gridLot, it.pyramidLotAdd, it.pyramidProfitPips, it.atrLotPeriod, it.atrRiskPerN, it.kellyWinRate, it.kellyWinLossRatio) },
        filter = d.filter.let { FilterRule(it.useTimeFilter, it.startHour, it.endHour, it.maxSpread, it.maxPositions, it.maxDailyTrades, it.cooldownBars) },
        enableHedging = d.enableHedging, multiSymbol = d.multiSymbol, symbols = d.symbols,
        sessionFilter = d.sessionFilter.let { SessionFilter(it.enabled, it.asian, it.asianStart, it.asianEnd, it.london, it.londonStart, it.londonEnd, it.ny, it.nyStart, it.nyEnd, it.monday, it.tuesday, it.wednesday, it.thursday, it.friday) },
        newsFilter = d.newsFilter.let { NewsFilter(it.enabled, tryEnum(it.importance), it.beforeMinutes, it.afterMinutes, it.onlyRedEvents) },
        equityProtection = d.equityProtection.let { EquityProtection(it.enabled, it.maxDrawdownPct, it.dailyLossLimit, it.dailyProfitTarget, it.consecutiveLosses, it.stopAction) },
        notifications = d.notifications.let { Notifications(it.enabled, it.onTrade, it.onSignal, it.onError, it.onDaily, TelegramConfig(it.telegram.enabled, it.telegram.botToken, it.telegram.chatId), EmailConfig(it.email.enabled, it.email.smtpServer, it.email.smtpPort, it.email.username, it.email.password, it.email.toEmail)) },
        signalFusion = d.signalFusion.let { SignalFusion(it.enabled, it.weights.map { w -> SignalWeight(tryEnum(w.indicator), w.weight, w.minStrength) }, it.buyThreshold, it.sellThreshold, it.useNormalization) },
        correlationFilter = d.correlationFilter.let { CorrelationFilter(it.enabled, it.maxCorrelation, it.lookbackBars, it.excludeList) },
        mql5CloudReady = d.mql5CloudReady,
        showPanel = d.showPanel, showStats = d.showStats,
        panelBg = d.panelBg, panelText = d.panelText, buyColor = d.buyColor, sellColor = d.sellColor,
    )

    private fun fromEntryDTO(d: EntryDTO) = EntryCondition(
        indicator = tryEnum(d.indicator), logicOp = if (d.logicOp == "||") LogicOp.OR else LogicOp.AND,
        timeframe = d.timeframe, entryType = tryEnum(d.entryType),
        fastPeriod = d.fastPeriod, slowPeriod = d.slowPeriod, midPeriod = d.midPeriod,
        maMethod = d.maMethod, appliedPrice = d.appliedPrice,
        obLevel = d.obLevel, osLevel = d.osLevel, period = d.period,
        kPeriod = d.kPeriod, dPeriod = d.dPeriod, slowing = d.slowing,
        bbPeriod = d.bbPeriod, bbDeviation = d.bbDeviation,
        adxLevel = d.adxLevel, sarStep = d.sarStep, sarMax = d.sarMax,
        tenkan = d.tenkan, kijun = d.kijun, senkou = d.senkou,
        jawPeriod = d.jawPeriod, teethPeriod = d.teethPeriod, lipsPeriod = d.lipsPeriod,
        atrPeriod = d.atrPeriod, atrMultiplier = d.atrMultiplier,
        lookbackBars = d.lookbackBars, candlePattern = d.candlePattern,
        volumeThreshold = d.volumeThreshold, divLookback = d.divLookback,
        divMinStrength = d.divMinStrength, customExpression = d.customExpression,
        customIndiName = d.customIndiName, customIndiParams = d.customIndiParams,
        customIndiBuffer = d.customIndiBuffer, customIndiSignal = d.customIndiSignal,
        direction = d.direction,
        multiTF = MultiTFConfig(d.multiTF.enabled, d.multiTF.confirmTF, d.multiTF.confirmBars, d.multiTF.requireBoth),
    )

    private fun fromExitDTO(d: ExitDTO) = ExitRule(
        exitType = tryEnum(d.exitType),
        slPoints = d.slPoints, tpPoints = d.tpPoints,
        trailingStart = d.trailingStart, trailingStep = d.trailingStep,
        atrMultSL = d.atrMultSL, atrPeriodSL = d.atrPeriodSL,
        breakevenPips = d.breakevenPips, breakevenLock = d.breakevenLock,
        exitMinutes = d.exitMinutes,
        tpTrailingStart = d.tpTrailingStart, tpTrailingStep = d.tpTrailingStep,
        maExitPeriod = d.maExitPeriod,
        exitIndicator = tryEnum(d.exitIndicator), exitIndicatorPeriod = d.exitIndicatorPeriod,
        exitObLevel = d.exitObLevel, exitOsLevel = d.exitOsLevel,
        partialTPs = d.partialTPs.map { PartialTP(it.tpPoints, it.closePercent, it.moveSLToBE) },
    )

    private inline fun <reified T : Enum<T>> tryEnum(name: String): T = enumValues<T>().firstOrNull { it.name == name } ?: enumValues<T>().first()

    fun validateJson(jsonStr: String): Result<String> {
        return try {
            json.decodeFromString<ConfigDTO>(jsonStr)
            Result.success("JSON验证通过")
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("JSON解析失败: ${e.message}"))
        }
    }
}