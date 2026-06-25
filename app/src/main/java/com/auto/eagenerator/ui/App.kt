package com.auto.eagenerator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.engine.Mql4Generator
import com.auto.eagenerator.engine.Mql5Generator
import com.auto.eagenerator.model.*
import com.auto.eagenerator.ui.theme.EaGeneratorTheme

typealias OnValue<T> = (T) -> Unit

val TF_OPTIONS = listOf("CURRENT" to "当前", "M1" to "M1", "M5" to "M5", "M15" to "M15", "M30" to "M30", "H1" to "H1", "H4" to "H4", "D1" to "D1", "W1" to "W1", "MN" to "MN")
val DIR_OPTIONS = listOf("Both" to "双向", "BuyOnly" to "只多", "SellOnly" to "只空")
val ORDER_OPTIONS = listOf(EntryOrderType.MARKET to "市价", EntryOrderType.LIMIT to "限价", EntryOrderType.STOP to "突破")
val MQL_OPTIONS = listOf("MQL5" to "MQL5", "MQL4" to "MQL4")
val INDICATOR_OPTIONS = IndicatorType.entries.map { it to it.label }
val EXIT_OPTIONS = ExitType.entries.map { it to it.label }
val MM_OPTIONS = MoneyManagement.entries.map { it to it.label }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EaGeneratorApp() {
    var dark by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("策略", "回测", "优化", "高级", "模板", "对比")

    EaGeneratorTheme(dark) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("EA Generator v4", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { dark = !dark }) { Text(if (dark) "☀" else "☾", fontSize = 18.sp) }
                    })
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {
                            Text(when (i) { 0 -> "⚙"; 1 -> "📊"; 2 -> "🔍"; 3 -> "🧬"; 4 -> "📋"; else -> "⚖" }, fontSize = 16.sp)
                        }, label = { Text(t, fontSize = 11.sp) })
                    }
                }
            }
        ) { padding -> TabContent(tab, Modifier.padding(padding)) }
    }
}

@Composable
private fun TabContent(tab: Int, modifier: Modifier) {
    var mqlVersion by remember { mutableStateOf("MQL5") }
    var strategyName by remember { mutableStateOf("AutoEA") }
    var magicNumber by remember { mutableStateOf("20250101") }
    var slippage by remember { mutableStateOf("3") }
    var entries by remember { mutableStateOf(listOf(EntryCondition(id = 1))) }
    var exits by remember { mutableStateOf(listOf(ExitRule(id = 1))) }
    var money by remember { mutableStateOf(MoneyRule()) }
    var filter by remember { mutableStateOf(FilterRule()) }
    var enableHedging by remember { mutableStateOf(false) }
    var multiSymbol by remember { mutableStateOf(false) }
    var symbolsText by remember { mutableStateOf("EURUSD,GBPUSD") }
    var sessionFilter by remember { mutableStateOf(SessionFilter()) }
    var equityProtection by remember { mutableStateOf(EquityProtection()) }
    var newsFilter by remember { mutableStateOf(NewsFilter()) }
    var notifications by remember { mutableStateOf(Notifications()) }
    var signalFusion by remember { mutableStateOf(SignalFusion()) }
    var correlationFilter by remember { mutableStateOf(CorrelationFilter()) }
    var mql5CloudReady by remember { mutableStateOf(false) }
    var geneticCfg by remember { mutableStateOf(GeneticOptConfig()) }
    var monteCarloCfg by remember { mutableStateOf(MonteCarloConfig()) }
    var walkForwardCfg by remember { mutableStateOf(WalkForwardConfig()) }
    var generatedCode by remember { mutableStateOf("// 点击生成按钮") }
    var showCode by remember { mutableStateOf(false) }

    val currentCfg = StrategyConfig(mqlVersion, strategyName, magicNumber.toIntOrNull() ?: 20250101, slippage.toIntOrNull() ?: 3,
        entries, exits, money, filter, enableHedging, multiSymbol, symbolsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
        sessionFilter, newsFilter, equityProtection, notifications, signalFusion, correlationFilter, mql5CloudReady)

    when (tab) {
        0 -> StrategyScreen(mqlVersion, { mqlVersion = it }, strategyName, { strategyName = it },
            magicNumber, { magicNumber = it }, slippage, { slippage = it },
            entries, { entries = it }, exits, { exits = it }, money, { money = it },
            filter, { filter = it }, enableHedging, { enableHedging = it },
            multiSymbol, { multiSymbol = it }, symbolsText, { symbolsText = it },
            sessionFilter, { sessionFilter = it }, equityProtection, { equityProtection = it },
            newsFilter, { newsFilter = it }, notifications, { notifications = it },
            signalFusion, { signalFusion = it }, correlationFilter, { correlationFilter = it },
            mql5CloudReady, { mql5CloudReady = it },
            generatedCode, showCode, { gen ->
                val cfg = currentCfg
                generatedCode = if (cfg.mqlVersion == "MQL5") Mql5Generator.generate(cfg) else Mql4Generator.generate(cfg)
                showCode = true
            }, modifier
        )
        1 -> BacktestScreen(currentCfg, modifier)
        2 -> OptimizeScreen(currentCfg, modifier)
        3 -> AdvancedScreen(currentCfg, geneticCfg, { geneticCfg = it }, monteCarloCfg, { monteCarloCfg = it }, walkForwardCfg, { walkForwardCfg = it }, modifier)
        4 -> TemplateScreen(currentCfg, modifier)
        5 -> CompareScreen(currentCfg, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyScreen(
    mqlVersion: String, onMql: (String) -> Unit,
    strategyName: String, onName: (String) -> Unit,
    magicNumber: String, onMagic: (String) -> Unit,
    slippage: String, onSlip: (String) -> Unit,
    entries: List<EntryCondition>, onEntries: (List<EntryCondition>) -> Unit,
    exits: List<ExitRule>, onExits: (List<ExitRule>) -> Unit,
    money: MoneyRule, onMoney: (MoneyRule) -> Unit,
    filter: FilterRule, onFilter: (FilterRule) -> Unit,
    enableHedging: Boolean, onHedge: (Boolean) -> Unit,
    multiSymbol: Boolean, onMulti: (Boolean) -> Unit,
    symbolsText: String, onSymbols: (String) -> Unit,
    sessionFilter: SessionFilter, onSess: (SessionFilter) -> Unit,
    equityProtection: EquityProtection, onEquity: (EquityProtection) -> Unit,
    newsFilter: NewsFilter, onNews: (NewsFilter) -> Unit,
    notifications: Notifications, onNotif: (Notifications) -> Unit,
    signalFusion: SignalFusion, onFusion: (SignalFusion) -> Unit,
    correlationFilter: CorrelationFilter, onCorr: (CorrelationFilter) -> Unit,
    mql5CloudReady: Boolean, onCloud: (Boolean) -> Unit,
    generatedCode: String, showCode: Boolean,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        // ─── 基础设置
        item { SectionTitle("═══ 基础设置 ═══") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MQL_OPTIONS.forEach { (v, l) -> FilterChip(v == mqlVersion, { onMql(v) }, label = { Text(l, fontSize = 12.sp) }) }
            Spacer(Modifier.weight(1f))
            Button({ onGenerate() }, Modifier.height(36.dp)) { Text("生成", fontSize = 13.sp) }
        }}
        item { OutlinedTextField(strategyName, { onName(it) }, label = { Text("策略名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(magicNumber, { onMagic(it) }, label = { Text("魔术号") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
            OutlinedTextField(slippage, { onSlip(it) }, label = { Text("滑点") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
        }}

        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("多币种", Modifier.align(Alignment.CenterVertically), fontSize = 13.sp)
            Switch(multiSymbol, { onMulti(it) })
            if (multiSymbol) OutlinedTextField(symbolsText, { onSymbols(it) }, label = { Text("币种,逗号分隔") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            Spacer(Modifier.weight(1f))
            Text("对冲", fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
            Switch(enableHedging, { onHedge(it) })
        }}

        // ─── 入场条件
        item { Row(Modifier.fillMaxWidth()) { SectionTitle("═══ 入场条件 ═══"); Spacer(Modifier.weight(1f)); TextButton({ onEntries(entries + EntryCondition(id = entries.size + 1)) }) { Text("+", fontSize = 18.sp) } } }
        itemsIndexed(entries) { i, e ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.weight(1f))
                        if (i > 0) TextButton({ onEntries(entries.filter { it.id != e.id }) }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                    }
                    // Indicator + direction
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        var expInd by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(e.indicator.label, { expInd = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                            DropdownMenu(expInd, { expInd = false }) { INDICATOR_OPTIONS.forEach { (t, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(indicator = t) else it }); expInd = false }) } }
                        }
                        var expDir by remember { mutableStateOf(false) }
                        Box(Modifier.weight(0.7f)) {
                            val dText = DIR_OPTIONS.find { it.first == e.direction }?.second ?: "双向"
                            OutlinedTextField(dText, { expDir = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                            DropdownMenu(expDir, { expDir = false }) { DIR_OPTIONS.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(direction = v) else it }); expDir = false }) } }
                        }
                        // Logic op
                        if (i > 0) {
                            var expL by remember { mutableStateOf(false) }
                            Box(Modifier.weight(0.4f)) {
                                OutlinedTextField(if (e.logicOp == LogicOp.AND) "AND" else "OR", { expL = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                                DropdownMenu(expL, { expL = false }) {
                                    DropdownMenuItem(text = { Text("AND") }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(logicOp = LogicOp.AND) else it }); expL = false })
                                    DropdownMenuItem(text = { Text("OR") }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(logicOp = LogicOp.OR) else it }); expL = false })
                                }
                            }
                        }
                    }
                    // Timeframe
                    var expTF by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        val tfText = TF_OPTIONS.find { it.first == e.timeframe }?.second ?: "当前"
                        OutlinedTextField("时框: $tfText", { expTF = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                        DropdownMenu(expTF, { expTF = false }) { TF_OPTIONS.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(timeframe = v) else it }); expTF = false }) } }
                    }
                    // Entry order type
                    var expOrd by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField("入场方式: ${e.entryType.label}", { expOrd = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                        DropdownMenu(expOrd, { expOrd = false }) { ORDER_OPTIONS.forEach { (t, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onEntries(entries.map { if (it.id == e.id) it.copy(entryType = t) else it }); expOrd = false }) } }
                    }
                    if (e.entryType == EntryOrderType.LIMIT) {
                        OutlinedTextField(e.limitOffset.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(limitOffset = it.toIntOrNull() ?: 50) else it }) }, label = { Text("限价偏移(点)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    }
                    if (e.entryType == EntryOrderType.STOP) {
                        OutlinedTextField(e.stopOffset.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(stopOffset = it.toIntOrNull() ?: 50) else it }) }, label = { Text("突破偏移(点)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    }
                    // Indicator-specific params
                    when (e.indicator) {
                        IndicatorType.MA_CROSS, IndicatorType.MA_TREND, IndicatorType.MA_PRICE -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(e.fastPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(fastPeriod = it.toIntOrNull() ?: 5) else it }) }, label = { Text("快均线") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                OutlinedTextField(e.slowPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(slowPeriod = it.toIntOrNull() ?: 20) else it }) }, label = { Text("慢均线") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                if (e.indicator == IndicatorType.MA_TREND) OutlinedTextField(e.midPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(midPeriod = it.toIntOrNull() ?: 50) else it }) }, label = { Text("中均线") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            }
                        }
                        IndicatorType.RSI, IndicatorType.RSI_DIVERGENCE -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(e.period.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(period = it.toIntOrNull() ?: 14) else it }) }, label = { Text("周期") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                if (e.indicator == IndicatorType.RSI) { OutlinedTextField(e.obLevel.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(obLevel = it.toDoubleOrNull() ?: 70.0) else it }) }, label = { Text("超买") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)); OutlinedTextField(e.osLevel.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(osLevel = it.toDoubleOrNull() ?: 30.0) else it }) }, label = { Text("超卖") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)) }
                                else OutlinedTextField(e.divLookback.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(divLookback = it.toIntOrNull() ?: 20) else it }) }, label = { Text("回溯K线") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            }
                        }
                        IndicatorType.ICUSTOM -> {
                            OutlinedTextField(e.customIndiName, { onEntries(entries.map { if (it.id == e.id) it.copy(customIndiName = it) else it }) }, label = { Text("指标名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.customIndiParams, { onEntries(entries.map { if (it.id == e.id) it.copy(customIndiParams = it) else it }) }, label = { Text("参数(逗号分隔)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(e.customIndiBuffer.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(customIndiBuffer = it.toIntOrNull() ?: 0) else it }) }, label = { Text("Buffer") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                OutlinedTextField(e.customIndiSignal.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(customIndiSignal = it.toDoubleOrNull() ?: 0.0) else it }) }, label = { Text("信号阈值") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            }
                        }
                        IndicatorType.CUSTOM_EXPRESSION -> {
                            OutlinedTextField(e.customExpression, { onEntries(entries.map { if (it.id == e.id) it.copy(customExpression = it) else it }) }, label = { Text("MQL条件表达式") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.MACD, IndicatorType.MACD_DIVERGENCE -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(e.fastPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(fastPeriod = it.toIntOrNull() ?: 12) else it }) }, label = { Text("快") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                OutlinedTextField(e.slowPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(slowPeriod = it.toIntOrNull() ?: 26) else it }) }, label = { Text("慢") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                                OutlinedTextField(e.period.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(period = it.toIntOrNull() ?: 9) else it }) }, label = { Text("信号") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            }
                            if (e.indicator == IndicatorType.MACD_DIVERGENCE) OutlinedTextField(e.divLookback.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(divLookback = it.toIntOrNull() ?: 20) else it }) }, label = { Text("背离回溯K线") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.BOLLINGER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(e.bbPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(bbPeriod = it.toIntOrNull() ?: 20) else it }) }, label = { Text("周期") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.bbDeviation.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(bbDeviation = it.toDoubleOrNull() ?: 2.0) else it }) }, label = { Text("偏差") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.ATR -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(e.atrPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(atrPeriod = it.toIntOrNull() ?: 14) else it }) }, label = { Text("周期") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.atrMultiplier.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(atrMultiplier = it.toDoubleOrNull() ?: 1.5) else it }) }, label = { Text("倍数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.ADX -> OutlinedTextField(e.adxLevel.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(adxLevel = it.toDoubleOrNull() ?: 25.0) else it }) }, label = { Text("ADX阈值") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        IndicatorType.CCI -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(e.period.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(period = it.toIntOrNull() ?: 14) else it }) }, label = { Text("周期") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.obLevel.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(obLevel = it.toDoubleOrNull() ?: 100.0) else it }) }, label = { Text("超买") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.osLevel.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(osLevel = it.toDoubleOrNull() ?: 100.0) else it }) }, label = { Text("超卖") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.PRICE_BREAK -> OutlinedTextField(e.lookbackBars.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(lookbackBars = it.toIntOrNull() ?: 20) else it }) }, label = { Text("回溯K线数") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        IndicatorType.STOCH -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(e.kPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(kPeriod = it.toIntOrNull() ?: 5) else it }) }, label = { Text("K") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.dPeriod.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(dPeriod = it.toIntOrNull() ?: 3) else it }) }, label = { Text("D") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.slowing.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(slowing = it.toIntOrNull() ?: 3) else it }) }, label = { Text("Sl") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        IndicatorType.SAR -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(e.sarStep.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(sarStep = it.toDoubleOrNull() ?: 0.02) else it }) }, label = { Text("步长") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(e.sarMax.toString(), { onEntries(entries.map { if (it.id == e.id) it.copy(sarMax = it.toDoubleOrNull() ?: 0.2) else it }) }, label = { Text("最大") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        else -> {}
                    }
                }
            }
        }

        // ─── 出场规则
        item { Row(Modifier.fillMaxWidth()) { SectionTitle("═══ 出场规则 ═══"); Spacer(Modifier.weight(1f)); TextButton({ onExits(exits + ExitRule(id = exits.size + 1)) }) { Text("+", fontSize = 18.sp) } } }
        itemsIndexed(exits) { i, ex ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth()) { Text("${i + 1}.", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.weight(1f)); if (i > 0) TextButton({ onExits(exits.filter { it.id != ex.id }) }) { Text("✕", color = MaterialTheme.colorScheme.error) } }
                    var expExit by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(ex.exitType.label, { expExit = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                        DropdownMenu(expExit, { expExit = false }) { EXIT_OPTIONS.forEach { (t, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onExits(exits.map { if (it.id == ex.id) it.copy(exitType = t) else it }); expExit = false }) } }
                    }
                    when (ex.exitType) {
                        ExitType.FIXED_SLTP -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(ex.slPoints.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(slPoints = it.toIntOrNull() ?: 300) else it }) }, label = { Text("SL(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(ex.tpPoints.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(tpPoints = it.toIntOrNull() ?: 600) else it }) }, label = { Text("TP(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        ExitType.TRAILING -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(ex.trailingStart.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(trailingStart = it.toIntOrNull() ?: 200) else it }) }, label = { Text("启动(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(ex.trailingStep.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(trailingStep = it.toIntOrNull() ?: 50) else it }) }, label = { Text("步长(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        ExitType.BREAKEVEN -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(ex.breakevenPips.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(breakevenPips = it.toIntOrNull() ?: 100) else it }) }, label = { Text("触发(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(ex.breakevenLock.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(breakevenLock = it.toIntOrNull() ?: 10) else it }) }, label = { Text("锁定(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        ExitType.TIME_EXIT -> OutlinedTextField(ex.exitMinutes.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(exitMinutes = it.toIntOrNull() ?: 240) else it }) }, label = { Text("超时(分钟)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        ExitType.TRAILING_PROFIT -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(ex.tpTrailingStart.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(tpTrailingStart = it.toIntOrNull() ?: 300) else it }) }, label = { Text("启动") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(ex.tpTrailingStep.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(tpTrailingStep = it.toIntOrNull() ?: 100) else it }) }, label = { Text("步长") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        ExitType.MA_SL -> OutlinedTextField(ex.maExitPeriod.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(maExitPeriod = it.toIntOrNull() ?: 20) else it }) }, label = { Text("均线周期") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        ExitType.ATR_SL -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(ex.atrPeriodSL.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(atrPeriodSL = it.toIntOrNull() ?: 14) else it }) }, label = { Text("ATR周期") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                            OutlinedTextField(ex.atrMultSL.toString(), { onExits(exits.map { if (it.id == ex.id) it.copy(atrMultSL = it.toDoubleOrNull() ?: 2.0) else it }) }, label = { Text("ATR倍数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        }
                        ExitType.PARTIAL_CLOSE -> {
                            ex.partialTPs.forEachIndexed { j, tp ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(tp.tpPoints.toString(), { val nl = ex.partialTPs.toMutableList(); nl[j] = tp.copy(tpPoints = it.toIntOrNull() ?: 300); exits = exits.map { if (it.id == ex.id) it.copy(partialTPs = nl) else it } }, label = { Text("止盈${j+1}(点)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                                    OutlinedTextField(tp.closePercent.toString(), { val nl = ex.partialTPs.toMutableList(); nl[j] = tp.copy(closePercent = it.toDoubleOrNull() ?: 50.0); exits = exits.map { if (it.id == ex.id) it.copy(partialTPs = nl) else it } }, label = { Text("平仓%") }, singleLine = true, modifier = Modifier.weight(0.7f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // ─── 资金管理
        item { SectionTitle("═══ 资金管理 ═══") }
        item {
            var expMM by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(money.mmType.label, { expMM = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), trailingIcon = { Text("▾") })
                DropdownMenu(expMM, { expMM = false }) { MM_OPTIONS.forEach { (t, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onMoney(money.copy(mmType = t)); expMM = false }) } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(money.maxLot.toString(), { onMoney(money.copy(maxLot = it.toDoubleOrNull() ?: 1.0)) }, label = { Text("最大手数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                when (money.mmType) {
                    MoneyManagement.FIXED_LOT -> OutlinedTextField(money.fixedLot.toString(), { onMoney(money.copy(fixedLot = it.toDoubleOrNull() ?: 0.01)) }, label = { Text("固定手数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                    MoneyManagement.RISK_PERCENT -> OutlinedTextField(money.riskPercent.toString(), { onMoney(money.copy(riskPercent = it.toDoubleOrNull() ?: 2.0)) }, label = { Text("风险%") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                    MoneyManagement.KELLY -> {
                        OutlinedTextField(money.kellyWinRate.toString(), { onMoney(money.copy(kellyWinRate = it.toDoubleOrNull() ?: 0.5)) }, label = { Text("胜率") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                        OutlinedTextField(money.kellyWinLossRatio.toString(), { onMoney(money.copy(kellyWinLossRatio = it.toDoubleOrNull() ?: 1.5)) }, label = { Text("盈亏比") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                    }
                    MoneyManagement.ATR_BASED -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(money.atrLotPeriod.toString(), { onMoney(money.copy(atrLotPeriod = it.toIntOrNull() ?: 14)) }, label = { Text("ATR周") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        OutlinedTextField(money.atrRiskPerN.toString(), { onMoney(money.copy(atrRiskPerN = it.toDoubleOrNull() ?: 0.02)) }, label = { Text("风险/N") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    else -> {}
                }
            }
        }

        // ─── 风控
        item { SectionTitle("═══ 风控 ═══") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(filter.maxPositions.toString(), { onFilter(filter.copy(maxPositions = it.toIntOrNull() ?: 3)) }, label = { Text("最大持仓") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(filter.maxDailyTrades.toString(), { onFilter(filter.copy(maxDailyTrades = it.toIntOrNull() ?: 0)) }, label = { Text("日最大交易") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(filter.maxSpread.toString(), { onFilter(filter.copy(maxSpread = it.toIntOrNull() ?: 0)) }, label = { Text("最大点差") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("时段过滤", fontSize = 13.sp); Spacer(Modifier.width(8.dp)); Switch(filter.useTimeFilter, { onFilter(filter.copy(useTimeFilter = it)) })
                if (filter.useTimeFilter) { OutlinedTextField(filter.startHour.toString(), { onFilter(filter.copy(startHour = it.toIntOrNull() ?: 8)) }, label = { Text("开始") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)); Text("~", fontSize = 14.sp); OutlinedTextField(filter.endHour.toString(), { onFilter(filter.copy(endHour = it.toIntOrNull() ?: 22)) }, label = { Text("结束") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("冷却K线", fontSize = 13.sp); Spacer(Modifier.width(4.dp))
                OutlinedTextField(filter.cooldownBars.toString(), { onFilter(filter.copy(cooldownBars = it.toIntOrNull() ?: 0)) }, label = { Text("(0=关)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }
        }

        // ─── 交易时段（Session Filter）
        item { SectionTitle("═══ 交易时段 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(sessionFilter.enabled, { onSess(sessionFilter.copy(enabled = it)) })
        }}
        if (sessionFilter.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(sessionFilter.asian, { onSess(sessionFilter.copy(asian = !sessionFilter.asian)) }, label = { Text("亚洲", fontSize = 11.sp) })
                FilterChip(sessionFilter.london, { onSess(sessionFilter.copy(london = !sessionFilter.london)) }, label = { Text("伦敦", fontSize = 11.sp) })
                FilterChip(sessionFilter.ny, { onSess(sessionFilter.copy(ny = !sessionFilter.ny)) }, label = { Text("纽约", fontSize = 11.sp) })
            }}
            if (sessionFilter.asian) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("亚洲:", fontSize = 12.sp); OutlinedTextField(sessionFilter.asianStart.toString(), { onSess(sessionFilter.copy(asianStart = it.toIntOrNull() ?: 0)) }, label = { Text("始") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)); OutlinedTextField(sessionFilter.asianEnd.toString(), { sessionFilter = sessionFilter.copy(asianEnd = it.toIntOrNull() ?: 9) }, label = { Text("终") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) } }
            if (sessionFilter.london) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("伦敦:", fontSize = 12.sp); OutlinedTextField(sessionFilter.londonStart.toString(), { sessionFilter = sessionFilter.copy(londonStart = it.toIntOrNull() ?: 8) }, label = { Text("始") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)); OutlinedTextField(sessionFilter.nyEnd.toString(), { onSess(sessionFilter.copy(londonEnd = it.toIntOrNull() ?: 17)) }, label = { Text("终") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) } }
            if (sessionFilter.ny) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("纽约:", fontSize = 12.sp); OutlinedTextField(sessionFilter.nyStart.toString(), { onSess(sessionFilter.copy(nyStart = it.toIntOrNull() ?: 13)) }, label = { Text("始") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)); OutlinedTextField(sessionFilter.nyEnd.toString(), { onSess(sessionFilter.copy(nyEnd = it.toIntOrNull() ?: 22)) }, label = { Text("终") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(sessionFilter.monday, { onSess(sessionFilter.copy(monday = !sessionFilter.monday)) }, label = { Text("周一", fontSize = 10.sp) })
                FilterChip(sessionFilter.tuesday, { onSess(sessionFilter.copy(tuesday = !sessionFilter.tuesday)) }, label = { Text("周二", fontSize = 10.sp) })
                FilterChip(sessionFilter.wednesday, { onSess(sessionFilter.copy(wednesday = !sessionFilter.wednesday)) }, label = { Text("周三", fontSize = 10.sp) })
                FilterChip(sessionFilter.thursday, { onSess(sessionFilter.copy(thursday = !sessionFilter.thursday)) }, label = { Text("周四", fontSize = 10.sp) })
                FilterChip(sessionFilter.friday, { onSess(sessionFilter.copy(friday = !sessionFilter.friday)) }, label = { Text("周五", fontSize = 10.sp) })
            }}
        }

        // ─── 资金保护（Equity Protection）
        item { SectionTitle("═══ 资金保护 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(equityProtection.enabled, { onEquity(equityProtection.copy(enabled = it)) })
        }}
        if (equityProtection.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(equityProtection.maxDrawdownPct.toString(), { onEquity(equityProtection.copy(maxDrawdownPct = it.toDoubleOrNull() ?: 20.0)) }, label = { Text("最大回撤%") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(equityProtection.dailyLossLimit.toString(), { onEquity(equityProtection.copy(dailyLossLimit = it.toDoubleOrNull() ?: 5.0)) }, label = { Text("日亏损%") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(equityProtection.consecutiveLosses.toString(), { onEquity(equityProtection.copy(consecutiveLosses = it.toIntOrNull() ?: 0)) }, label = { Text("连亏数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
        }

        // ─── v6: 信号融合（Signal Fusion）───
        item { SectionTitle("═══ 信号融合 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(signalFusion.enabled, { onFusion(signalFusion.copy(enabled = it)) })
        }}
        if (signalFusion.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(signalFusion.buyThreshold.toString(), { onFusion(signalFusion.copy(buyThreshold = it.toDoubleOrNull() ?: 1.5)) }, label = { Text("买入阈值") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(signalFusion.sellThreshold.toString(), { onFusion(signalFusion.copy(sellThreshold = it.toDoubleOrNull() ?: 1.5)) }, label = { Text("卖出阈值") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("归一化", fontSize = 12.sp); Switch(signalFusion.useNormalization, { onFusion(signalFusion.copy(useNormalization = it)) })
            }}
            item { Text("信号权重", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            signalFusion.weights.forEachIndexed { j, w ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        var expW by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1.2f)) {
                            OutlinedTextField(w.indicator.label, { expW = true }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp), trailingIcon = { Text("▾") })
                            DropdownMenu(expW, { expW = false }) { INDICATOR_OPTIONS.forEach { (t, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { val nl = signalFusion.weights.toMutableList(); nl[j] = w.copy(indicator = t); onFusion(signalFusion.copy(weights = nl)); expW = false }) } }
                        }
                        OutlinedTextField(w.weight.toString(), { val nl = signalFusion.weights.toMutableList(); nl[j] = w.copy(weight = it.toDoubleOrNull() ?: 1.0); onFusion(signalFusion.copy(weights = nl)) }, label = { Text("权重") }, singleLine = true, modifier = Modifier.weight(0.8f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        OutlinedTextField(w.minStrength.toString(), { val nl = signalFusion.weights.toMutableList(); nl[j] = w.copy(minStrength = it.toDoubleOrNull() ?: 0.0); onFusion(signalFusion.copy(weights = nl)) }, label = { Text("最小强度") }, singleLine = true, modifier = Modifier.weight(0.8f), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth()) { Spacer(Modifier.weight(1f))
                TextButton({ onFusion(signalFusion.copy(weights = signalFusion.weights + SignalWeight(IndicatorType.MA_CROSS, 1.0))) }) { Text("+权重", fontSize = 11.sp) }
            }}
        }

        // ─── v6: 相关性过滤 ───
        item { SectionTitle("═══ 相关性过滤 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(correlationFilter.enabled, { onCorr(correlationFilter.copy(enabled = it)) })
        }}
        if (correlationFilter.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(correlationFilter.maxCorrelation.toString(), { onCorr(correlationFilter.copy(maxCorrelation = it.toDoubleOrNull() ?: 0.8)) }, label = { Text("最大相关性") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(correlationFilter.lookbackBars.toString(), { onCorr(correlationFilter.copy(lookbackBars = it.toIntOrNull() ?: 100)) }, label = { Text("回溯K线") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
        }

        // ─── v6: 新闻过滤 ───
        item { SectionTitle("═══ 新闻过滤 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(newsFilter.enabled, { onNews(newsFilter.copy(enabled = it)) })
        }}
        if (newsFilter.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(newsFilter.beforeMinutes.toString(), { onNews(newsFilter.copy(beforeMinutes = it.toIntOrNull() ?: 60)) }, label = { Text("前(分)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(newsFilter.afterMinutes.toString(), { onNews(newsFilter.copy(afterMinutes = it.toIntOrNull() ?: 30)) }, label = { Text("后(分)") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(newsFilter.importance == NewsImportance.HIGH, { onNews(newsFilter.copy(importance = NewsImportance.HIGH)) }, label = { Text("高", fontSize = 11.sp) })
                FilterChip(newsFilter.importance == NewsImportance.MEDIUM, { onNews(newsFilter.copy(importance = NewsImportance.MEDIUM)) }, label = { Text("中", fontSize = 11.sp) })
                FilterChip(newsFilter.importance == NewsImportance.LOW, { onNews(newsFilter.copy(importance = NewsImportance.LOW)) }, label = { Text("低", fontSize = 11.sp) })
            }}
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("仅红色事件", fontSize = 12.sp); Switch(newsFilter.onlyRedEvents, { onNews(newsFilter.copy(onlyRedEvents = it)) })
            }}
        }

        // ─── v6: 通知（Notifications）───
        item { SectionTitle("═══ 通知 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(notifications.enabled, { onNotif(notifications.copy(enabled = it)) })
        }}
        if (notifications.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(notifications.onTrade, { onNotif(notifications.copy(onTrade = !notifications.onTrade)) }, label = { Text("交易", fontSize = 11.sp) })
                FilterChip(notifications.onSignal, { onNotif(notifications.copy(onSignal = !notifications.onSignal)) }, label = { Text("信号", fontSize = 11.sp) })
                FilterChip(notifications.onError, { onNotif(notifications.copy(onError = !notifications.onError)) }, label = { Text("错误", fontSize = 11.sp) })
                FilterChip(notifications.onDaily, { onNotif(notifications.copy(onDaily = !notifications.onDaily)) }, label = { Text("日报", fontSize = 11.sp) })
            }}
            item { SectionTitle("── Telegram ──") }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("启用", fontSize = 12.sp); Switch(notifications.telegram.enabled, { onNotif(notifications.copy(telegram = notifications.telegram.copy(enabled = it))) })
            }}
            if (notifications.telegram.enabled) {
                item { OutlinedTextField(notifications.telegram.botToken, { onNotif(notifications.copy(telegram = notifications.telegram.copy(botToken = it))) }, label = { Text("Bot Token") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                item { OutlinedTextField(notifications.telegram.chatId, { onNotif(notifications.copy(telegram = notifications.telegram.copy(chatId = it))) }, label = { Text("Chat ID") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
            }
        }

        // ─── v6: MQL5云端优化 ───
        item { SectionTitle("═══ MQL5设置 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("云端优化就绪头", fontSize = 13.sp); Switch(mql5CloudReady, { onCloud(it) })
        }}

        // ─── 源码输出
        item { SectionTitle("═══ 生成结果 ═══") }
        if (showCode) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            val btnText = if (generatedCode.contains("MQL5")) "复制 MQL5" else "复制 MQL4"
                            Button({ (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("ea", generatedCode)); Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show() }, Modifier.height(32.dp)) { Text(btnText, fontSize = 12.sp) }
                            Spacer(Modifier.weight(1f))
                            Button({ val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, generatedCode) }; ctx.startActivity(Intent.createChooser(intent, "分享")) }, Modifier.height(32.dp)) { Text("分享", fontSize = 12.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(generatedCode, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}
