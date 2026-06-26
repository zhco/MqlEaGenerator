package com.auto.eagenerator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.engine.Optimizer
import com.auto.eagenerator.model.*
import kotlin.math.roundToInt

data class OptParamUI(val name: String, val min: String, val max: String, val step: String)

@Composable
fun OptimizeScreen(
    cfg: StrategyConfig,
    modifier: Modifier = Modifier
) {
    var csvInput by remember { mutableStateOf("") }
    var params by remember { mutableStateOf(listOf(OptParamUI("", "1", "100", "1"))) }
    var report by remember { mutableStateOf<OptReport?>(null) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun run() {
        if (csvInput.isBlank()) return
        val ranges = params.filter { it.name.isNotBlank() }.map {
            ParamRange(it.name, it.min.toDoubleOrNull() ?: 1.0, it.max.toDoubleOrNull() ?: 100.0, it.step.toDoubleOrNull() ?: 1.0)
        }
        if (ranges.isEmpty()) return
        running = true; error = ""
        try { report = Optimizer.run(csvInput, cfg, ranges) }
        catch (e: Exception) { error = e.message ?: "错误" }
        running = false
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { SectionTitle("═══ 参数优化 ═══") }
        item {
            OutlinedTextField(csvInput, { csvInput = it },
                label = { Text("粘贴CSV数据") }, modifier = Modifier.fillMaxWidth().height(80.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp), maxLines = 4
            )
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                Text("优化参数:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton({ params = params + OptParamUI("", "1", "100", "1") }) { Text("+参数", fontSize = 12.sp) }
            }
        }
        itemsIndexed(params) { i, p ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp)) {
                Column(Modifier.padding(6.dp)) {
                    OutlinedTextField(p.name, { v -> params = params.toMutableList().also { it[i] = p.copy(name = v) } },
                        label = { Text("参数名") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(p.min, { v -> params = params.toMutableList().also { it[i] = p.copy(min = v) } },
                            label = { Text("最小") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        OutlinedTextField(p.max, { v -> params = params.toMutableList().also { it[i] = p.copy(max = v) } },
                            label = { Text("最大") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                        OutlinedTextField(p.step, { v -> params = params.toMutableList().also { it[i] = p.copy(step = v) } },
                            label = { Text("步长") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ run() }, enabled = !running && csvInput.isNotBlank()) { Text(if (running) "优化中..." else "开始优化", fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        report?.let { rpt ->
            val best = rpt.best ?: return@let
            item { Divider() }
            item { SectionTitle("═══ 优化结果 ═══") }
            item { Text("总组合: ${rpt.totalRuns}  |  耗时: ${rpt.elapsedMs / 1000.0}s", fontSize = 12.sp) }
            item { Text("最优参数 (夏普: ${"%.2f".format(best.sharpe)}):", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("净收益: \$${best.netProfit.roundToInt()}  |  胜率: ${"%.1f".format(best.winRate)}%  |  回撤: ${"%.1f".format(best.maxDD)}%  |  交易: ${best.trades}", fontSize = 12.sp)
                        best.params.forEach { (k, v) -> Text("  $k = $v", fontSize = 11.sp) }
                    }
                }
            }
            if (rpt.top10.size > 1) {
                item { Text("Top 10:", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                rpt.top10.take(10).forEachIndexed { idx, r ->
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("#${idx + 1}  S:${"%.2f".format(r.sharpe)}  \$:${r.netProfit.roundToInt()}  DD:${"%.1f".format(r.maxDD)}%", fontSize = 10.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Double.roundToInt() = this.roundToInt()
