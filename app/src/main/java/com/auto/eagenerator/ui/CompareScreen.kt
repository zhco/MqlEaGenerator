package com.auto.eagenerator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(cfg: StrategyConfig, modifier: Modifier = Modifier) {
    var compareList by remember { mutableStateOf(listOf("v1", "v2")) }
    var compareName by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)) {

        item { SectionTitle("═══ 策略对比 ═══") }

        // 添加对比策略
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(compareName, { compareName = it }, label = { Text("策略版本名") }, singleLine = true,
                    modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                Button({ if (compareName.isNotBlank()) { compareList = compareList + compareName; compareName = "" } },
                    Modifier.height(48.dp)) { Text("添加", fontSize = 12.sp) }
            }
        }

        // 已添加列表
        item { Text("对比列表", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        compareList.forEachIndexed { i, name ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) {
                    Row(Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. $name", fontSize = 13.sp)
                        TextButton({ compareList = compareList.filterIndexed { j, _ -> j != i } }) {
                            Text("✕", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 对比指标预览
        item { SectionTitle("═══ 指标预览 ═══") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("当前策略: ${cfg.strategyName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("待对比: ${compareList.drop(1).joinToString(", ")}", fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBox("净利润", "——", Modifier.weight(1f))
                        MetricBox("夏普比率", "——", Modifier.weight(1f))
                        MetricBox("胜率", "——", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBox("最大回撤", "——", Modifier.weight(1f))
                        MetricBox("盈利因子", "——", Modifier.weight(1f))
                        MetricBox("交易次数", "——", Modifier.weight(1f))
                    }
                }
            }
        }

        // 生成报告
        item { SectionTitle("═══ 导出 ═══") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ /* TODO: export HTML */ }, Modifier.weight(1f).height(40.dp)) { Text("导出HTML报告", fontSize = 12.sp) }
                Button({ /* TODO: export JSON */ }, Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("导出JSON", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
