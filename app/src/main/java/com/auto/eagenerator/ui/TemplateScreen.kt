package com.auto.eagenerator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(cfg: StrategyConfig, modifier: Modifier = Modifier) {
    val templates = remember {
        listOf(
            StrategyTemplate("均线金叉趋势", "双均线金叉+趋势过滤，适合H1/D1趋势跟随", "Trend", "MQL5",
                listOf(EntryCondition(1, IndicatorType.MA, comparison = ComparisonOp.CROSS_ABOVE), EntryCondition(2, IndicatorType.ADX, logicOp = LogicOp.AND)),
                listOf(ExitRule(1, ExitType.FIXED_SLTP), ExitRule(2, ExitType.TRAILING)),
                MoneyRule(MoneyManagement.RISK_PERCENT),
                tags = listOf("趋势", "均线"), difficulty = "初级",
                timeframes = listOf("H1", "D1"), bestMarket = "趋势市场"),
            StrategyTemplate("RSI超卖反弹", "RSI超卖+布林带确认，适合震荡市场均值回归", "MeanReversion", "MQL5",
                listOf(EntryCondition(1, IndicatorType.RSI), EntryCondition(2, IndicatorType.BOLLINGER, logicOp = LogicOp.AND)),
                listOf(ExitRule(1, ExitType.FIXED_SLTP, tpPoints = 200)),
                MoneyRule(MoneyManagement.FIXED_LOT),
                tags = listOf("震荡", "均值回归"), difficulty = "中级",
                timeframes = listOf("M15", "H1"), bestMarket = "震荡市场"),
            StrategyTemplate("MACD背离反转", "MACD顶底背离捕捉反转点，配合Stochastic确认", "Trend", "MQL5",
                listOf(EntryCondition(1, IndicatorType.MACD, comparison = ComparisonOp.CROSS_ABOVE), EntryCondition(2, IndicatorType.STOCH, logicOp = LogicOp.AND)),
                listOf(ExitRule(1, ExitType.TRAILING, trailingStart = 150)),
                MoneyRule(MoneyManagement.ATR_BASED),
                tags = listOf("反转", "背离"), difficulty = "高级",
                timeframes = listOf("H1", "H4"), bestMarket = "趋势转折"),
            StrategyTemplate("布林带突破", "布林带收窄后突破，配合成交量确认", "Breakout", "MQL5",
                listOf(EntryCondition(1, IndicatorType.BOLLINGER), EntryCondition(2, IndicatorType.VOLUME, logicOp = LogicOp.AND)),
                listOf(ExitRule(1, ExitType.BREAKEVEN), ExitRule(2, ExitType.TRAILING)),
                MoneyRule(MoneyManagement.KELLY),
                tags = listOf("突破", "波动率"), difficulty = "中级",
                timeframes = listOf("H1", "H4"), bestMarket = "突破行情"),
            StrategyTemplate("一目均衡云图", "Ichimoku云层突破+鳄鱼线趋势确认", "Trend", "MQL5",
                listOf(EntryCondition(1, IndicatorType.ICHIMOKU), EntryCondition(2, IndicatorType.ALLIGATOR, logicOp = LogicOp.AND)),
                listOf(ExitRule(1, ExitType.MA_SL, maExitPeriod = 26)),
                MoneyRule(MoneyManagement.PYRAMID),
                tags = listOf("云图", "趋势/金字塔"), difficulty = "高级",
                timeframes = listOf("H4", "D1"), bestMarket = "强趋势市场"),
            StrategyTemplate("网格多指标", "结合多个指标信号融合的网格策略", "MultiIndi", "MQL5",
                listOf(EntryCondition(1, IndicatorType.MA, comparison = ComparisonOp.CROSS_ABOVE), EntryCondition(2, IndicatorType.RSI, logicOp = LogicOp.OR)),
                listOf(ExitRule(1, ExitType.PARTIAL_CLOSE)),
                MoneyRule(MoneyManagement.GRID),
                tags = listOf("网格", "多指标"), difficulty = "高级",
                timeframes = listOf("M5", "M15"), bestMarket = "盘整市场"),
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)) {

        item { SectionTitle("═══ 策略模板库 ═══") }

        items(templates) { tmpl ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tmpl.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(tmpl.difficulty, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(tmpl.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tmpl.tags.forEach { tag ->
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(tag, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${tmpl.category} / ${tmpl.mqlVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("时框: ${tmpl.timeframes.joinToString(",")}", fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("适用: ${tmpl.bestMarket}", fontSize = 11.sp)
                    }
                    Button({ /* TODO: apply template */ }, Modifier.fillMaxWidth().height(32.dp)) { Text("应用模板", fontSize = 12.sp) }
                }
            }
        }
    }
}
