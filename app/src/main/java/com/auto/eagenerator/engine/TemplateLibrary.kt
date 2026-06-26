package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*

/**
 * 策略模板库 - 预设经典策略
 */
object TemplateLibrary {

    val builtInTemplates: List<StrategyTemplate> = listOf(
        // ═══ 趋势类 ═══
        StrategyTemplate(
            name = "均线金叉死叉",
            category = "Trend",
            description = "双均线交叉经典趋势跟踪策略。快线上穿慢线做多，下穿做空。",
            tags = listOf("MA", "入门", "趋势"),
            difficulty = "初级",
            timeframes = listOf("H1", "H4"),
            bestMarket = "趋势市场",
            config = defaultMaCrossConfig(),
        ),
        StrategyTemplate(
            name = "三重EMA趋势",
            category = "Trend",
            description = "三根EMA排列确认趋势方向，结合ADX过滤震荡行情。",
            tags = listOf("EMA", "ADX", "趋势"),
            difficulty = "中级",
            timeframes = listOf("H4", "D1"),
            bestMarket = "强趋势市场",
            config = defaultTripleEmaConfig(),
        ),
        StrategyTemplate(
            name = "一目均衡(ICHIMOKU)",
            category = "Trend",
            description = "基于一目均衡表的云层突破策略。价格突破云层且转换线在基准线上方做多。",
            tags = listOf("Ichimoku", "云层", "趋势"),
            difficulty = "中级",
            timeframes = listOf("H1", "H4", "D1"),
            bestMarket = "趋势市场",
            config = defaultIchimokuConfig(),
        ),
        StrategyTemplate(
            name = "布林带突破",
            category = "Breakout",
            description = "价格突破布林带上轨做多/下轨做空，配合成交量确认。",
            tags = listOf("Bollinger", "突破", "波动率"),
            difficulty = "初级",
            timeframes = listOf("M15", "H1"),
            bestMarket = "波动市场",
            config = defaultBbConfig(),
        ),

        // ═══ 均值回归 ═══
        StrategyTemplate(
            name = "RSI超买超卖",
            category = "MeanReversion",
            description = "RSI进入超卖区做多，超买区做空，配合趋势过滤。",
            tags = listOf("RSI", "反转", "入门"),
            difficulty = "初级",
            timeframes = listOf("H1", "H4"),
            bestMarket = "震荡市场",
            config = defaultRsiConfig(),
        ),
        StrategyTemplate(
            name = "RSI背离策略",
            category = "MeanReversion",
            description = "检测RSI与价格的顶底背离信号，反转概率高。",
            tags = listOf("RSI", "背离", "反转"),
            difficulty = "高级",
            timeframes = listOf("H1", "H4", "D1"),
            bestMarket = "趋势反转点",
            config = defaultRsiDivConfig(),
        ),
        StrategyTemplate(
            name = "Stochastic超买超卖",
            category = "MeanReversion",
            description = "随机指标K/D线进入超卖区金叉做多，超买区死叉做空。",
            tags = listOf("Stochastic", "振荡器", "反转"),
            difficulty = "初级",
            timeframes = listOf("M30", "H1"),
            bestMarket = "区间震荡",
            config = defaultStochConfig(),
        ),

        // ═══ 突破类 ═══
        StrategyTemplate(
            name = "前高前低突破",
            category = "Breakout",
            description = "价格突破N周期最高价做多，跌破最低价做空。",
            tags = listOf("突破", "价格行为", "经典"),
            difficulty = "初级",
            timeframes = listOf("H1", "H4"),
            bestMarket = "突破行情",
            config = defaultBreakoutConfig(),
        ),
        StrategyTemplate(
            name = "鳄鱼线+分形",
            category = "Breakout",
            description = "鳄鱼线发散时跟随分形突破方向入场。",
            tags = listOf("Alligator", "分形", "突破"),
            difficulty = "中级",
            timeframes = listOf("H1", "H4"),
            bestMarket = "趋势启动",
            config = defaultAlligatorConfig(),
        ),

        // ═══ 多指标融合 ═══
        StrategyTemplate(
            name = "MACD+RSI+均线融合",
            category = "MultiIndi",
            description = "三个经典指标的信号加权融合：MACD金叉+RSI不超买+价格在均线上方。",
            tags = listOf("融合", "多确认", "稳健"),
            difficulty = "中级",
            timeframes = listOf("H1", "H4", "D1"),
            bestMarket = "通用",
            config = defaultFusionConfig(),
        ),
        StrategyTemplate(
            name = "ADX趋势+CCI入场",
            category = "MultiIndi",
            description = "ADX确认强趋势，CCI回调至-100以下做多/+100以上做空。",
            tags = listOf("ADX", "CCI", "顺势"),
            difficulty = "中级",
            timeframes = listOf("H1", "H4"),
            bestMarket = "强趋势市场",
            config = defaultAdxCciConfig(),
        ),

        // ═══ 剥头皮 ═══
        StrategyTemplate(
            name = "ATR波动突破",
            category = "Scalping",
            description = "基于ATR的动态突破策略，适合短线快速进出。",
            tags = listOf("ATR", "波动", "短线"),
            difficulty = "中级",
            timeframes = listOf("M5", "M15"),
            bestMarket = "活跃时段",
            config = defaultAtrConfig(),
        ),
        StrategyTemplate(
            name = "成交量突破",
            category = "Scalping",
            description = "成交量放大+价格突破关键位，确认主力资金入场。",
            tags = listOf("Volume", "突破", "短线"),
            difficulty = "高级",
            timeframes = listOf("M5", "M15"),
            bestMarket = "活跃时段",
            config = defaultVolumeConfig(),
        ),
    )

    private fun defaultMaCrossConfig(): String = """{"mqlVersion":"MQL5","strategyName":"均线金叉死叉","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"MA","fastPeriod":10,"comparison":"CROSS_ABOVE","targetType":"PRICE","targetPrice":"Close","direction":"Both"}],"exits":[{"id":1,"exitType":"FIXED_SLTP","slPoints":500,"tpPoints":1000}],"money":{"mmType":"RISK_PERCENT","riskPercent":2.0}}"""
    private fun defaultTripleEmaConfig(): String = """{"mqlVersion":"MQL5","strategyName":"三重EMA趋势","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"MA","fastPeriod":10,"maMethod":"EMA","comparison":"GT","targetType":"PRICE","targetPrice":"Close","direction":"Both"}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":300,"trailingStep":100}],"money":{"mmType":"RISK_PERCENT","riskPercent":1.5}}"""
    private fun defaultIchimokuConfig(): String = """{"mqlVersion":"MQL5","strategyName":"一目均衡","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"ICHIMOKU","tenkan":9,"kijun":26,"senkou":52,"comparison":"GT","targetType":"PRICE","targetPrice":"Close","direction":"Both"}],"exits":[{"id":1,"exitType":"INDICATOR_EXIT","exitIndicator":"ICHIMOKU"}],"money":{"mmType":"FIXED_LOT","fixedLot":0.1}}"""
    private fun defaultBbConfig(): String = """{"mqlVersion":"MQL5","strategyName":"布林带突破","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"BOLLINGER","bbPeriod":20,"bbDeviation":2.0,"comparison":"GT","targetType":"PRICE","targetPrice":"Close","direction":"Both"}],"exits":[{"id":1,"exitType":"MA_SL","maExitPeriod":20}],"money":{"mmType":"RISK_PERCENT","riskPercent":2.0}}"""
    private fun defaultRsiConfig(): String = """{"mqlVersion":"MQL5","strategyName":"RSI超买超卖","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"RSI","period":14,"comparison":"LTE","targetType":"FIXED","targetFixed":30.0,"direction":"Both"}],"exits":[{"id":1,"exitType":"FIXED_SLTP","slPoints":300,"tpPoints":600}],"money":{"mmType":"RISK_PERCENT","riskPercent":1.0}}"""
    private fun defaultRsiDivConfig(): String = """{"mqlVersion":"MQL5","strategyName":"RSI背离策略","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"RSI","period":14,"comparison":"GTE","targetType":"FIXED","targetFixed":50.0,"direction":"Both"}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":200,"trailingStep":50}],"money":{"mmType":"RISK_PERCENT","riskPercent":1.5}}"""
    private fun defaultStochConfig(): String = """{"mqlVersion":"MQL5","strategyName":"Stochastic超买超卖","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"STOCH","kPeriod":5,"dPeriod":3,"slowing":3,"comparison":"LTE","targetType":"FIXED","targetFixed":20.0,"direction":"Both"}],"exits":[{"id":1,"exitType":"FIXED_SLTP","slPoints":400,"tpPoints":800}],"money":{"mmType":"FIXED_LOT","fixedLot":0.1}}"""
    private fun defaultBreakoutConfig(): String = """{"mqlVersion":"MQL5","strategyName":"前高前低突破","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"PRICE","srcBuffer":0,"comparison":"GT","targetType":"PRICE","targetPrice":"High","direction":"Both"}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":250,"trailingStep":100}],"money":{"mmType":"RISK_PERCENT","riskPercent":2.0}}"""
    private fun defaultAlligatorConfig(): String = """{"mqlVersion":"MQL5","strategyName":"鳄鱼线+分形","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"ALLIGATOR","jawPeriod":13,"teethPeriod":8,"lipsPeriod":5,"comparison":"GT","targetType":"PRICE","targetPrice":"Close","direction":"Both"}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":300,"trailingStep":100}],"money":{"mmType":"FIXED_LOT","fixedLot":0.1}}"""
    private fun defaultFusionConfig(): String = """{"mqlVersion":"MQL5","strategyName":"MACD+RSI+均线融合","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"MACD","direction":"Both","logicOp":"AND","comparison":"GT","targetType":"FIXED","targetFixed":0.0},{"id":2,"indicator":"RSI","period":14,"direction":"Both","logicOp":"AND","comparison":"LTE","targetType":"FIXED","targetFixed":30.0},{"id":3,"indicator":"MA","fastPeriod":50,"direction":"Both","comparison":"GT","targetType":"PRICE","targetPrice":"Close"}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":300,"trailingStep":100}],"signalFusion":{"enabled":true,"weights":[{"indicator":"MACD","weight":1.5},{"indicator":"RSI","weight":1.0},{"indicator":"MA","weight":1.0}],"buyThreshold":2.0,"sellThreshold":2.0},"money":{"mmType":"RISK_PERCENT","riskPercent":2.0}}"""
    private fun defaultAdxCciConfig(): String = """{"mqlVersion":"MQL5","strategyName":"ADX趋势+CCI入场","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"ADX","period":14,"direction":"Both","logicOp":"AND","comparison":"GT","targetType":"FIXED","targetFixed":25.0},{"id":2,"indicator":"CCI","period":14,"direction":"Both","comparison":"GT","targetType":"FIXED","targetFixed":100.0}],"exits":[{"id":1,"exitType":"TRAILING","trailingStart":300,"trailingStep":100}],"money":{"mmType":"RISK_PERCENT","riskPercent":1.5}}"""
    private fun defaultAtrConfig(): String = """{"mqlVersion":"MQL5","strategyName":"ATR波动突破","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"ATR","atrPeriod":14,"comparison":"GT","targetType":"FIXED","targetFixed":0.001,"direction":"Both"}],"exits":[{"id":1,"exitType":"ATR_SL","atrMultSL":2.0,"atrPeriodSL":14}],"money":{"mmType":"ATR_BASED","atrLotPeriod":14,"atrRiskPerN":0.02}}"""
    private fun defaultVolumeConfig(): String = """{"mqlVersion":"MQL5","strategyName":"成交量突破","magicNumber":${randomMagic()},"entries":[{"id":1,"indicator":"VOLUME","period":20,"comparison":"GT","targetType":"FIXED","targetFixed":500.0,"direction":"Both"}],"exits":[{"id":1,"exitType":"FIXED_SLTP","slPoints":200,"tpPoints":400}],"money":{"mmType":"RISK_PERCENT","riskPercent":0.5}}"""

    private fun randomMagic() = (10000000..99999999).random()
}
