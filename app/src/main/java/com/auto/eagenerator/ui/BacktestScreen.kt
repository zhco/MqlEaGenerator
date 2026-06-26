package com.auto.eagenerator.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auto.eagenerator.engine.BacktestEngine
import com.auto.eagenerator.engine.DataService
import com.auto.eagenerator.model.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ImportChannel { PASTE, SAMPLE, ONLINE, URL, FILE }
enum class DataSource { EASTMONEY, YAHOO, BINANCE, FRANKFURTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    cfg: StrategyConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var channel by remember { mutableStateOf(ImportChannel.PASTE) }
    var csvInput by remember { mutableStateOf("") }

    // 样本参数
    var sampleBars by remember { mutableStateOf("500") }
    var samplePrice by remember { mutableStateOf("1.20000") }
    var sampleVolatility by remember { mutableStateOf("0.008") }

    // URL
    var urlInput by remember { mutableStateOf("") }
    var urlFetching by remember { mutableStateOf(false) }

    // 在线
    var dataSource by remember { mutableStateOf(DataSource.EASTMONEY) }  // 默认东方财富(国内可用)
    var yahooSymbol by remember { mutableStateOf("EURUSD=X") }
    var yahooInterval by remember { mutableStateOf("1d") }
    var yahooRange by remember { mutableStateOf("6mo") }
    var yahooFetching by remember { mutableStateOf(false) }
    var yahooExpanded by remember { mutableStateOf(false) }  // 品种下拉

    // 文件
    var selectedFileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }

    // 回测
    var report by remember { mutableStateOf<BTReport?>(null) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val name = uri.lastPathSegment ?: "unknown.csv"
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                selectedFileName = name; fileContent = content; csvInput = content; error = ""
            } catch (e: Exception) {
                error = "读取文件失败: ${e.message}"
            }
        }
    }

    fun resolveCsv(): String = when (channel) {
        ImportChannel.PASTE -> csvInput
        ImportChannel.SAMPLE -> {
            try {
                DataService.generateSampleData(
                    bars = sampleBars.toIntOrNull() ?: 500,
                    startPrice = samplePrice.toDoubleOrNull() ?: 1.20000,
                    volatility = sampleVolatility.toDoubleOrNull() ?: 0.008
                ).also { csvInput = it }
            } catch (e: Exception) { error = "参数错误: ${e.message}"; "" }
        }
        ImportChannel.URL -> csvInput
        ImportChannel.ONLINE -> csvInput
        ImportChannel.FILE -> fileContent
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { SectionTitle("═══ 回测 ═══") }

        // 渠道选择
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChannelChip("粘贴", ImportChannel.PASTE, channel, Icons.Default.ContentPaste) { channel = it }
                ChannelChip("样本", ImportChannel.SAMPLE, channel, Icons.Default.AutoAwesome) {
                    channel = it; csvInput = ""; fileContent = ""; selectedFileName = ""
                }
                ChannelChip("在线", ImportChannel.ONLINE, channel, Icons.Default.CloudDownload) {
                    channel = it; csvInput = ""; fileContent = ""; selectedFileName = ""
                }
                ChannelChip("网址", ImportChannel.URL, channel, Icons.Default.Link) {
                    channel = it; csvInput = ""; fileContent = ""; selectedFileName = ""
                }
                ChannelChip("文件", ImportChannel.FILE, channel, Icons.Default.FolderOpen) {
                    channel = it; csvInput = ""; fileContent = ""; selectedFileName = ""
                }
            }
        }

        // 面板
        item {
            when (channel) {
                ImportChannel.PASTE -> {
                    OutlinedTextField(
                        csvInput, { csvInput = it },
                        label = { Text("粘贴CSV (Date,Open,High,Low,Close[,Volume])") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                        maxLines = 8
                    )
                }
                ImportChannel.SAMPLE -> {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("生成模拟 OHLC 数据", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(sampleBars, { sampleBars = it },
                                    label = { Text("K线数") }, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true)
                                OutlinedTextField(samplePrice, { samplePrice = it },
                                    label = { Text("起始价") }, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true)
                                OutlinedTextField(sampleVolatility, { sampleVolatility = it },
                                    label = { Text("波动率") }, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true)
                            }
                            Text("波动率 0.008 ≈ 日振幅 0.8%，带随机趋势", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (csvInput.isNotBlank()) Text("已生成 ${csvInput.lines().size - 1} 条K线", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                ImportChannel.ONLINE -> {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 数据源切换
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(selected = dataSource == DataSource.EASTMONEY,
                                        onClick = { dataSource = DataSource.EASTMONEY; yahooSymbol = DataService.QUICK_SYMBOLS_EASTMONEY.first().first },
                                        label = { Text("东方财富", fontSize = 10.sp) })
                                    FilterChip(selected = dataSource == DataSource.YAHOO,
                                        onClick = { dataSource = DataSource.YAHOO; yahooSymbol = DataService.QUICK_SYMBOLS_YAHOO.first().first },
                                        label = { Text("Yahoo", fontSize = 10.sp) })
                                    FilterChip(selected = dataSource == DataSource.BINANCE,
                                        onClick = { dataSource = DataSource.BINANCE; yahooSymbol = DataService.QUICK_SYMBOLS_BINANCE.first().first },
                                        label = { Text("币安", fontSize = 10.sp) })
                                    FilterChip(selected = dataSource == DataSource.FRANKFURTER,
                                        onClick = { dataSource = DataSource.FRANKFURTER; yahooSymbol = DataService.QUICK_SYMBOLS_FRANKFURTER.first().first },
                                        label = { Text("Frankfurter", fontSize = 10.sp) })
                                }
                                Text(when(dataSource) {
                                    DataSource.EASTMONEY -> "A股/港股/美股"
                                    DataSource.YAHOO -> "海外全品类"
                                    DataSource.BINANCE -> "加密货币 OHLC"
                                    DataSource.FRANKFURTER -> "外汇汇率(仅日线)"
                                }, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // 品种选择 + 下拉
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    yahooSymbol, { yahooSymbol = it },
                                    label = { Text("品种代码") },
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { yahooExpanded = !yahooExpanded }) {
                                            Icon(if (yahooExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "快捷", Modifier.size(20.dp))
                                        }
                                    }
                                )
                            }
                            // 快捷品种下拉
                            if (yahooExpanded) {
                                val quickList = when (dataSource) {
                                    DataSource.EASTMONEY -> DataService.QUICK_SYMBOLS_EASTMONEY
                                    DataSource.YAHOO -> DataService.QUICK_SYMBOLS_YAHOO
                                    DataSource.BINANCE -> DataService.QUICK_SYMBOLS_BINANCE
                                    DataSource.FRANKFURTER -> DataService.QUICK_SYMBOLS_FRANKFURTER
                                }
                                quickList.forEach { (sym, name) ->
                                    val selected = sym == yahooSymbol
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { yahooSymbol = sym; yahooExpanded = false },
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(sym, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f))
                                            Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // 周期（Frankfurter 仅日线，隐藏）
                            if (dataSource != DataSource.FRANKFURTER) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DataService.INTERVALS.forEach { (iv, label) ->
                                        FilterChip(
                                            selected = yahooInterval == iv,
                                            onClick = { yahooInterval = iv },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            // 范围
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DataService.RANGES.forEach { (rg, label) ->
                                    FilterChip(
                                        selected = yahooRange == rg,
                                        onClick = { yahooRange = rg },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }

                            // 拉取按钮
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        yahooFetching = true; error = ""
                                        scope.launch {
                                            val result = when (dataSource) {
                                                DataSource.EASTMONEY -> DataService.fetchEastMoneyCsv(yahooSymbol.trim(), yahooInterval, yahooRange)
                                                DataSource.YAHOO -> DataService.fetchYahooCsv(yahooSymbol.trim(), yahooInterval, yahooRange)
                                                DataSource.BINANCE -> DataService.fetchBinanceCsv(yahooSymbol.trim(), yahooInterval, yahooRange)
                                                DataSource.FRANKFURTER -> DataService.fetchFrankfurterCsv(yahooSymbol.trim(), yahooRange)
                                            }
                                            result.fold(
                                                onSuccess = {
                                                    csvInput = it
                                                    val bars = it.lines().size - 1
                                                    error = if (dataSource == DataSource.FRANKFURTER)
                                                        "获取成功: $yahooSymbol · 日线 · $bars 条"
                                                    else
                                                        "获取成功: $yahooSymbol · $yahooInterval · $bars 条K线"
                                                },
                                                onFailure = { error = it.message ?: "获取失败" }
                                            )
                                            yahooFetching = false
                                        }
                                    },
                                    enabled = yahooSymbol.isNotBlank() && !yahooFetching
                                ) {
                                    if (yahooFetching) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp)); Text("获取中...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp)); Text("获取数据", fontSize = 13.sp)
                                    }
                                }
                            }
                            Text("支持外汇(EURUSD=X)、加密货币(BTC-USD)、美股(AAPL)、指数(SPY)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (csvInput.isNotBlank()) Text("已加载 ${csvInput.lines().size - 1} 条K线", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                ImportChannel.URL -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(urlInput, { urlInput = it },
                            label = { Text("CSV 文件网址（公开直链）") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    urlFetching = true; error = ""
                                    scope.launch {
                                        DataService.fetchCsvFromUrl(urlInput.trim()).fold(
                                            onSuccess = { csvInput = it; error = "下载成功，${it.lines().size - 1} 条K线" },
                                            onFailure = { error = it.message ?: "下载失败" }
                                        )
                                        urlFetching = false
                                    }
                                },
                                enabled = urlInput.isNotBlank() && !urlFetching
                            ) {
                                if (urlFetching) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp)); Text("下载中...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp)); Text("下载", fontSize = 13.sp)
                                }
                            }
                            if (csvInput.isNotBlank()) Text("${csvInput.lines().size - 1} 条K线", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        Text("支持 MT5/TradingView 导出的 CSV 直链", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ImportChannel.FILE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { filePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("选择 CSV 文件", fontSize = 13.sp)
                        }
                        if (selectedFileName.isNotEmpty()) {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(6.dp))
                                    Text(selectedFileName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.weight(1f))
                                    Text("${fileContent.lines().size - 1} 条", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 运行按钮
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        running = true; error = ""
                        val data = resolveCsv()
                        if (data.isBlank()) { error = "请先获取/输入 CSV 数据"; running = false; return@Button }
                        if (!DataService.validateCsv(data)) { error = "CSV 格式无效，需包含 Date/Open/High/Low/Close 列"; running = false; return@Button }
                        try { report = BacktestEngine.run(data, cfg) } catch (e: Exception) { error = e.message ?: "回测错误"; report = null }
                        running = false
                    },
                    enabled = !running
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text(if (running) "运行中..." else "运行回测", fontSize = 13.sp)
                }
                if (error.isNotEmpty()) {
                    if (error.startsWith("下载成功")) {
                        Text(error, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically).weight(1f))
                    } else {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically).weight(1f))
                    }
                }
            }
        }

        // 报告
        report?.let { rpt ->
            item { Divider() }
            item { SectionTitle("═══ 回测报告 ═══") }
            item { Text(rpt.strategyName, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            item { Text("${rpt.startDate} ~ ${rpt.endDate}  |  K线: ${rpt.totalBars}  |  交易: ${rpt.totalTrades}", fontSize = 12.sp) }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        statRow("净收益", "\$${rpt.netProfit.roundToInt()}", if (rpt.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        statRow("总盈利 / 总亏损", "\$${rpt.grossProfit.roundToInt()} / \$${rpt.grossLoss.roundToInt()}")
                        statRow("胜率", "${"%.1f".format(rpt.winRate)}%  (${rpt.wins}胜 / ${rpt.losses}负)")
                        statRow("盈亏比 (PF)", "%.2f".format(rpt.profitFactor))
                        statRow("平均盈利 / 亏损", "\$${rpt.avgWin.roundToInt()} / \$${rpt.avgLoss.roundToInt()}")
                        statRow("期望值", "\$${rpt.expect.roundToInt()}")
                        statRow("最大回撤", "\$${rpt.maxDrawdown.roundToInt()} (${"%.1f".format(rpt.maxDrawdownPct)}%)")
                        statRow("夏普比率", "%.2f".format(rpt.sharpeRatio))
                    }
                }
            }
            if (rpt.trades.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)); Text("最近交易:", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                for (t in rpt.trades.takeLast(10).reversed()) {
                    val color = if (t.profit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            Text("${t.entryTime}  ${if (t.dir == 1) "BUY" else "SELL"}  ${"%.5f".format(t.entryPrice)}→${"%.5f".format(t.exitPrice)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text("\$${t.profit.roundToInt()}", color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelChip(label: String, channel: ImportChannel, current: ImportChannel, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (ImportChannel) -> Unit) {
    val selected = channel == current
    FilterChip(selected = selected, onClick = { onSelect(channel) },
        label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(label, fontSize = 12.sp) } })
}

@Composable
private fun statRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = LocalContentColor.current) {
    Row(Modifier.fillMaxWidth()) { Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f)); Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor) }
}

private fun Double.roundToInt() = this.roundToInt()
