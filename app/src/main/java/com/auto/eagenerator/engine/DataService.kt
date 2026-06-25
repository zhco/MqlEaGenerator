package com.auto.eagenerator.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object DataService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Yahoo Finance 常用品种快捷映射 */
    val QUICK_SYMBOLS_YAHOO = listOf(
        "EURUSD=X" to "欧元/美元",
        "GBPUSD=X" to "英镑/美元",
        "USDJPY=X" to "美元/日元",
        "AUDUSD=X" to "澳元/美元",
        "XAUUSD=X" to "黄金/美元",
        "BTC-USD"  to "比特币/美元",
        "ETH-USD"  to "以太坊/美元",
        "SPY"      to "标普500 ETF",
        "QQQ"      to "纳斯达克100 ETF",
        "AAPL"     to "苹果",
    )

    /** 东方财富 常用品种映射 */
    val QUICK_SYMBOLS_EASTMONEY = listOf(
        "1.EURUSD"  to "欧元/美元",
        "1.GBPUSD"  to "英镑/美元",
        "1.USDJPY"  to "美元/日元",
        "1.AUDUSD"  to "澳元/美元",
        "1.XAUUSD"  to "黄金/美元",
        "1.000001"  to "上证指数",
        "0.399001"  to "深证成指",
        "1.600519"  to "贵州茅台",
        "0.300750"  to "宁德时代",
        "1.000300"  to "沪深300",
    )

    val INTERVALS = listOf("1d" to "日线", "1h" to "1小时", "30m" to "30分钟", "15m" to "15分钟", "5m" to "5分钟", "1wk" to "周线", "1mo" to "月线")
    val RANGES = listOf("1mo" to "1个月", "3mo" to "3个月", "6mo" to "6个月", "1y" to "1年", "2y" to "2年", "5y" to "5年")

    /** 东方财富 K 线周期映射 */
    private val EASTMONEY_KLINES: Map<String, Int> = mapOf(
        "1d" to 101, "1wk" to 102, "1mo" to 103,
        "1h" to 60, "30m" to 30, "15m" to 15, "5m" to 5, "1m" to 1
    )

    /** 币安 常用品种 */
    val QUICK_SYMBOLS_BINANCE = listOf(
        "BTCUSDT" to "比特币",
        "ETHUSDT" to "以太坊",
        "BNBUSDT" to "币安币",
        "SOLUSDT" to "Solana",
        "XRPUSDT" to "瑞波币",
        "DOGEUSDT" to "狗狗币",
        "ADAUSDT" to "艾达币",
        "AVAXUSDT" to "雪崩协议",
        "DOTUSDT" to "波卡",
        "LINKUSDT" to "Chainlink",
    )

    /** 币安 K 线周期映射 */
    private val BINANCE_INTERVALS: Map<String, String> = mapOf(
        "1d" to "1d", "1wk" to "1w", "1mo" to "1M",
        "1h" to "1h", "30m" to "30m", "15m" to "15m", "5m" to "5m"
    )

    /** Frankfurter 常用外汇 */
    val QUICK_SYMBOLS_FRANKFURTER = listOf(
        "EUR" to "欧元/美元 (EUR/USD)",
        "GBP" to "英镑/美元 (GBP/USD)",
        "JPY" to "美元/日元 (USD/JPY)",
        "CNH" to "美元/离岸人民币 (USD/CNH)",
        "AUD" to "澳元/美元 (AUD/USD)",
        "CHF" to "美元/瑞士法郎 (USD/CHF)",
        "CAD" to "美元/加元 (USD/CAD)",
        "NZD" to "纽元/美元 (NZD/USD)",
        "HKD" to "美元/港元 (USD/HKD)",
        "XAU" to "黄金/美元 (XAU/USD)",
    )

    /**
     * 从 Yahoo Finance 拉取历史 OHLC CSV
     */
    suspend fun fetchYahooCsv(
        symbol: String,
        interval: String = "1d",
        range: String = "6mo"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val start = when (range) {
                "1mo"  -> now.minusSeconds(86400L * 30)
                "3mo"  -> now.minusSeconds(86400L * 90)
                "6mo"  -> now.minusSeconds(86400L * 180)
                "1y"   -> now.minusSeconds(86400L * 365)
                "2y"   -> now.minusSeconds(86400L * 730)
                "5y"   -> now.minusSeconds(86400L * 1825)
                else   -> now.minusSeconds(86400L * 180) // default 6mo
            }
            val url = "https://query1.finance.yahoo.com/v7/finance/download/$symbol" +
                    "?period1=${start.epochSecond}" +
                    "&period2=${now.epochSecond}" +
                    "&interval=$interval" +
                    "&events=history" +
                    "&includeAdjustedClose=true"

            val request = Request.Builder().url(url).header("User-Agent", "MqlEaGenerator/1.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                if (response.code == 404) {
                    Result.failure(Exception("品种代码无效: $symbol\n请检查代码格式，如 EURUSD=X, AAPL"))
                } else {
                    Result.failure(Exception("Yahoo 请求失败: HTTP ${response.code}\n${errBody.take(200)}"))
                }
            } else {
                val body = response.body?.string() ?: ""
                if (body.isBlank() || body.lines().size < 2) {
                    Result.failure(Exception("返回数据为空，可能该品种不存在或无历史"))
                } else {
                    Result.success(body)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络请求失败: ${e.message}"))
        }
    }

    /**
     * 从 URL 获取 CSV 数据
     */
    suspend fun fetchCsvFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", "MqlEaGenerator/1.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.isBlank()) Result.failure(Exception("返回内容为空"))
                else Result.success(body)
            } else {
                Result.failure(Exception("请求失败: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络请求失败: ${e.message}"))
        }
    }

    /**
     * 生成模拟 OHLC 数据（几何随机游走）
     * @param bars 生成K线数量
     * @param startPrice 起始价格
     * @param volatility 日波动率（0.01 = 1%）
     * @param drift 趋势偏移
     */
    fun generateSampleData(
        bars: Int = 500,
        startPrice: Double = 1.20000,
        volatility: Double = 0.008,
        drift: Double = 0.0001
    ): String {
        val dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        var date = LocalDate.of(2024, 1, 1)
        var price = startPrice
        val sb = StringBuilder("Date,Open,High,Low,Close,Volume\n")

        for (i in 0 until bars) {
            // 跳过周末
            while (date.dayOfWeek.value > 5) date = date.plusDays(1)

            val open = price
            // 生成日内高低
            val range = open * volatility * (0.8 + Random.nextDouble() * 0.4)
            val high = open + range * (0.5 + Random.nextDouble() * 0.5)
            val low = open - range * (0.3 + Random.nextDouble() * 0.5)
            val close = low + (high - low) * Random.nextDouble()
            val volume = 1000 + Random.nextInt(9000)

            // 带趋势的价格漂移
            price = close * (1.0 + drift + (Random.nextDouble() - 0.5) * volatility * 0.8)

            sb.append("${date.format(dtf)},$open,%.5f,%.5f,%.5f,$volume".format(high, low, close))
            sb.append("\n")
            date = date.plusDays(1)
        }

        return sb.toString().trimEnd()
    }

    /**
     * 从东方财富拉取历史 OHLC 数据，输出标准 CSV
     */
    suspend fun fetchEastMoneyCsv(
        symbol: String,
        interval: String = "1d",
        range: String = "6mo"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val klt = EASTMONEY_KLINES[interval] ?: return@withContext Result.failure(Exception("不支持的K线周期: $interval"))
            val limit = when (range) {
                "1mo" -> 30; "3mo" -> 90; "6mo" -> 180
                "1y" -> 365; "2y" -> 730; "5y" -> 1825
                else -> 180
            }
            val url = "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
                    "?secid=$symbol" +
                    "&fields1=f1,f2,f3,f4,f5,f6" +
                    "&fields2=f51,f52,f53,f54,f55,f56" +
                    "&klt=$klt" +
                    "&fqt=1" +
                    "&end=20500101" +
                    "&lmt=$limit"

            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("东方财富请求失败: HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: ""
            val json = org.json.JSONObject(body)
            val data = json.optJSONObject("data")
                ?: return@withContext Result.failure(Exception("无数据: 品种代码可能无效"))

            val klines = data.optJSONArray("klines")
            if (klines == null || klines.length() == 0) {
                return@withContext Result.failure(Exception("K线数据为空, 请检查品种代码"))
            }

            // 东方财富格式: 日期,开盘,收盘,最高,最低,成交量,...
            // 转为标准: Date,Open,High,Low,Close,Volume
            val sb = StringBuilder("Date,Open,High,Low,Close,Volume\n")
            for (i in 0 until klines.length()) {
                val parts = klines.getString(i).split(",")
                if (parts.size < 6) continue
                sb.append("${parts[0]},${parts[1]},${parts[3]},${parts[4]},${parts[2]},${parts[5]}\n")
            }

            Result.success(sb.toString().trimEnd())
        } catch (e: Exception) {
            Result.failure(Exception("东方财富请求失败: ${e.message}"))
        }
    }

    /**
     * 从币安拉取加密货币 OHLC 数据，输出标准 CSV
     */
    suspend fun fetchBinanceCsv(
        symbol: String,
        interval: String = "1d",
        range: String = "6mo"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bi = BINANCE_INTERVALS[interval] ?: return@withContext Result.failure(Exception("不支持的周期: $interval"))
            val limit = when (range) {
                "1mo" -> 30; "3mo" -> 90; "6mo" -> 180
                "1y" -> 365; "2y" -> 730; "5y" -> 1825
                else -> 180
            }
            val url = "https://api.binance.com/api/v3/klines?symbol=${symbol.uppercase()}&interval=$bi&limit=$limit"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("币安请求失败: HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: ""
            val arr = org.json.JSONArray(body)
            if (arr.length() == 0) return@withContext Result.failure(Exception("无数据"))

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val sb = StringBuilder("Date,Open,High,Low,Close,Volume\n")
            for (i in 0 until arr.length()) {
                val k = arr.getJSONArray(i)
                val ts = k.getLong(0)
                val open = k.getDouble(1)
                val high = k.getDouble(2)
                val low = k.getDouble(3)
                val close = k.getDouble(4)
                val volume = k.getDouble(5)
                sb.append("${sdf.format(java.util.Date(ts))},$open,$high,$low,$close,$volume\n")
            }
            Result.success(sb.toString().trimEnd())
        } catch (e: Exception) {
            Result.failure(Exception("币安请求失败: ${e.message}"))
        }
    }

    /**
     * 从 Frankfurter 拉取外汇日线汇率，输出标准 CSV（仅日线，OHLC 等同当日汇率）
     */
    suspend fun fetchFrankfurterCsv(
        symbol: String,
        range: String = "6mo"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = java.time.Instant.now()
            val to = java.time.LocalDate.now(java.time.ZoneId.of("UTC"))
            val from = to.minusDays(when(range) {
                "1mo" -> 30L; "3mo" -> 90L; "6mo" -> 180L
                "1y" -> 365L; "2y" -> 730L; "5y" -> 1825L
                else -> 180L
            })
            val url = "https://api.frankfurter.dev/v2/rates.csv?from=$from&to=$to&base=USD&quotes=${symbol.uppercase()}"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Frankfurter 请求失败: HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: ""
            // Frankfurter CSV: date,base,quote,rate
            val lines = body.trim().lines()
            if (lines.size < 2) return@withContext Result.failure(Exception("无数据"))
            val sb = StringBuilder("Date,Open,High,Low,Close,Volume\n")
            for (line in lines.drop(1)) {
                val parts = line.split(",")
                if (parts.size < 4) continue
                val date = parts[0]
                val rate = parts[3]
                sb.append("$date,$rate,$rate,$rate,$rate,0\n")
            }
            Result.success(sb.toString().trimEnd())
        } catch (e: Exception) {
            Result.failure(Exception("Frankfurter 请求失败: ${e.message}"))
        }
    }

    /**
     * 校验 CSV 是否为有效 OHLC 格式
     */
    fun validateCsv(csv: String): Boolean {
        val lines = csv.trim().lines()
        if (lines.size < 2) return false
        val header = lines[0].lowercase()
        return header.contains("date") && header.contains("open") && header.contains("close")
    }
}
