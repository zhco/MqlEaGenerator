package com.auto.eagenerator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    cfg: StrategyConfig,
    geneticCfg: GeneticOptConfig, onGenetic: (GeneticOptConfig) -> Unit,
    monteCarloCfg: MonteCarloConfig, onMC: (MonteCarloConfig) -> Unit,
    walkForwardCfg: WalkForwardConfig, onWF: (WalkForwardConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)) {

        // ─── 遗传算法优化 ───
        item { SectionTitle("═══ 遗传算法优化 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(geneticCfg.enabled, { onGenetic(geneticCfg.copy(enabled = it)) })
        }}
        if (geneticCfg.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(geneticCfg.popSize.toString(), { onGenetic(geneticCfg.copy(popSize = it.toIntOrNull() ?: 50)) }, label = { Text("种群大小") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(geneticCfg.generations.toString(), { onGenetic(geneticCfg.copy(generations = it.toIntOrNull() ?: 20)) }, label = { Text("代数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(geneticCfg.mutationRate.toString(), { onGenetic(geneticCfg.copy(mutationRate = it.toDoubleOrNull() ?: 0.1)) }, label = { Text("变异率") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(geneticCfg.crossoverRate.toString(), { onGenetic(geneticCfg.copy(crossoverRate = it.toDoubleOrNull() ?: 0.7)) }, label = { Text("交叉率") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(geneticCfg.eliteCount.toString(), { onGenetic(geneticCfg.copy(eliteCount = it.toIntOrNull() ?: 3)) }, label = { Text("精英数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(geneticCfg.tournamentSize.toString(), { onGenetic(geneticCfg.copy(tournamentSize = it.toIntOrNull() ?: 4)) }, label = { Text("锦标赛") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item {
                val targets = listOf("Sharpe" to "夏普比率", "NetProfit" to "净利润", "WinRate" to "胜率", "ProfitFactor" to "盈利因子")
                var expT by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth().clickable { expT = true }) {
                    val label = targets.find { it.first == geneticCfg.target }?.second ?: "夏普比率"
                    OutlinedTextField("优化目标: $label", {}, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), trailingIcon = { Text("▾") })
                    DropdownMenu(expT, { expT = false }) { targets.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onGenetic(geneticCfg.copy(target = v)); expT = false }) } }
                }
            }
            item { Button({ /* TODO: run GA */ }, Modifier.fillMaxWidth().height(40.dp)) { Text("运行遗传算法优化", fontSize = 13.sp) } }
        }

        // ─── 蒙特卡洛分析 ───
        item { SectionTitle("═══ 蒙特卡洛分析 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(monteCarloCfg.enabled, { onMC(monteCarloCfg.copy(enabled = it)) })
        }}
        if (monteCarloCfg.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(monteCarloCfg.simulations.toString(), { onMC(monteCarloCfg.copy(simulations = it.toIntOrNull() ?: 200)) }, label = { Text("模拟次数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(monteCarloCfg.resampleRatio.toString(), { onMC(monteCarloCfg.copy(resampleRatio = it.toDoubleOrNull() ?: 0.8)) }, label = { Text("采样比例") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(monteCarloCfg.confidenceLevel.toString(), { onMC(monteCarloCfg.copy(confidenceLevel = it.toDoubleOrNull() ?: 95.0)) }, label = { Text("置信度 %") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Button({ /* TODO: run MC */ }, Modifier.fillMaxWidth().height(40.dp)) { Text("运行蒙特卡洛分析", fontSize = 13.sp) } }
        }

        // ─── 前进式优化 ───
        item { SectionTitle("═══ 前进式优化 ═══") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 13.sp); Switch(walkForwardCfg.enabled, { onWF(walkForwardCfg.copy(enabled = it)) })
        }}
        if (walkForwardCfg.enabled) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(walkForwardCfg.inSamplePct.toString(), { onWF(walkForwardCfg.copy(inSamplePct = it.toDoubleOrNull() ?: 60.0)) }, label = { Text("样本内%") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                OutlinedTextField(walkForwardCfg.outSamplePct.toString(), { onWF(walkForwardCfg.copy(outSamplePct = it.toDoubleOrNull() ?: 40.0)) }, label = { Text("样本外%") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(walkForwardCfg.stepSize.toString(), { onWF(walkForwardCfg.copy(stepSize = it.toIntOrNull() ?: 500)) }, label = { Text("步进K线数") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
            }}
            item { Button({ /* TODO: run WF */ }, Modifier.fillMaxWidth().height(40.dp)) { Text("运行前进式优化", fontSize = 13.sp) } }
        }
    }
}
