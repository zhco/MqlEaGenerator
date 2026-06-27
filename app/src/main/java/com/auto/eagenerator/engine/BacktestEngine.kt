package com.auto.eagenerator.engine

import com.auto.eagenerator.model.*
import kotlin.math.*

object BacktestEngine {

    // ─── 指标缓冲区 ───
    private class Buffers(val size: Int) {
        val ma = mutableMapOf<String, DoubleArray>()
        val rsi = mutableMapOf<Int, DoubleArray>()
        val macdMain = mutableMapOf<String, DoubleArray>()
        val macdSig = mutableMapOf<String, DoubleArray>()
        val bbUpper = mutableMapOf<String, DoubleArray>()
        val bbLower = mutableMapOf<String, DoubleArray>()
        val atr = mutableMapOf<Int, DoubleArray>()
        val adx = mutableMapOf<Int, DoubleArray>()
        val cci = mutableMapOf<Int, DoubleArray>()
        val stochK = mutableMapOf<String, DoubleArray>()
        val stochD = mutableMapOf<String, DoubleArray>()
        val sar = mutableMapOf<String, DoubleArray>()
    }

    // ─── 入场条件配置 ───
    private class SimEntry(val condition: EntryCondition)

    // ─── 出场规则配置 ───
    private class SimExit(val rule: ExitRule)

    data class SimPosition(var ticket: Int, var dir: Int, var openPrice: Double, var openBar: Int,
                           var lots: Double, var sl: Double = 0.0, var tp: Double = 0.0,
                           var partialDone: BooleanArray = BooleanArray(0))

    // ─── CSV解析 ───
    fun parseCsv(csv: String): List<Candle> {
        return csv.lines().drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split(",", ";", "\t").map { it.trim() }.filter { it.isNotEmpty() }
            if (p.size < 5) return@mapNotNull null
            try {
                Candle(p[0], p[1].toDouble(), p[2].toDouble(), p[3].toDouble(), p[4].toDouble(),
                    p.getOrNull(5)?.toDoubleOrNull()?.toLong() ?: 1000)
            } catch (_: Exception) { null }
        }
    }

    // ─── 预计算全部指标 ───
    private fun precompute(candles: List<Candle>, config: StrategyConfig): Buffers {
        val n = candles.size
        val b = Buffers(n)
        val close = DoubleArray(n) { candles[it].close }
        val high = DoubleArray(n) { candles[it].high }
        val low = DoubleArray(n) { candles[it].low }
        val hlc3 = DoubleArray(n) { (candles[it].high + candles[it].low + candles[it].close) / 3.0 }

        // 收集所有需要的周期
        val maPeriods = mutableSetOf<Pair<Int, String>>()
        val rsiPeriods = mutableSetOf<Int>()
        val macdSets = mutableSetOf<Triple<Int, Int, Int>>()
        val bbSets = mutableSetOf<Pair<Int, Double>>()
        val atrPeriods = mutableSetOf<Int>()
        val adxPeriods = mutableSetOf<Int>()
        val cciPeriods = mutableSetOf<Int>()
        val stochSets = mutableSetOf<Triple<Int, Int, Int>>()
        val sarSets = mutableSetOf<Pair<Double, Double>>()

        config.entries.forEach { e ->
            when (e.indicator) {
                IndicatorType.MA, IndicatorType.MA, IndicatorType.PRICE -> {
                    maPeriods += e.fastPeriod to e.maMethod
                    maPeriods += e.slowPeriod to e.maMethod
                }
                IndicatorType.RSI, IndicatorType.RSI -> rsiPeriods += e.period
                IndicatorType.MACD, IndicatorType.MACD -> macdSets += Triple(e.fastPeriod, e.slowPeriod, e.period)
                IndicatorType.BOLLINGER -> bbSets += e.bbPeriod to e.bbDeviation
                IndicatorType.ATR -> atrPeriods += e.atrPeriod
                IndicatorType.ADX -> adxPeriods += e.period
                IndicatorType.CCI -> cciPeriods += e.period
                IndicatorType.STOCH -> stochSets += Triple(e.kPeriod, e.dPeriod, e.slowing)
                IndicatorType.SAR -> sarSets += e.sarStep to e.sarMax
                else -> {}
            }
        }
        config.exits.forEach { ex ->
            when (ex.exitType) {
                ExitType.MA_SL -> maPeriods += ex.maExitPeriod to "SMA"
                ExitType.ATR_SL -> atrPeriods += ex.atrPeriodSL
                else -> {}
            }
        }
        if (config.money.mmType == MoneyManagement.ATR_BASED) atrPeriods += config.money.atrLotPeriod

        // 逐周期计算
        for ((p, m) in maPeriods) b.ma["${p}_$m"] = sma(close, p)
        for (p in rsiPeriods) b.rsi[p] = rsi(close, p)
        for ((f, s, sig) in macdSets) {
            b.macdMain["${f}_${s}_$sig"] = macdLine(close, f, s)
            b.macdSig["${f}_${s}_$sig"] = macdSignal(close, f, s, sig)
        }
        for ((p, d) in bbSets) {
            b.bbUpper["${p}_$d"] = bollingerBand(close, p, d, true)
            b.bbLower["${p}_$d"] = bollingerBand(close, p, d, false)
        }
        for (p in atrPeriods) b.atr[p] = atr(high, low, close, p)
        for (p in adxPeriods) b.adx[p] = adx(high, low, close, p)
        for (p in cciPeriods) b.cci[p] = cci(hlc3, p)
        for ((k, d, sl) in stochSets) {
            b.stochK["${k}_${d}_$sl"] = stochastic(high, low, close, k, d, sl, true)
            b.stochD["${k}_${d}_$sl"] = stochastic(high, low, close, k, d, sl, false)
        }
        for ((step, mx) in sarSets) b.sar["${step}_$mx"] = parabolicSAR(high, low, step, mx)

        return b
    }

    // ─── 运行回测 ───
    fun run(csv: String, config: StrategyConfig): BTReport {
        val candles = parseCsv(csv)
        if (candles.size < 100) return BTReport(config.strategyName, "", "", 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), emptyList())

        val bufs = precompute(candles, config)
        val n = candles.size
        val trades = mutableListOf<BTTrade>()
        val positions = mutableListOf<SimPosition>()
        val equity = mutableListOf<Double>()
        var balance = 10000.0
        var ticket = 0

        for (i in 100 until n) {
            val c = candles[i]
            val spread = c.close * 0.0001

            // ── 管理已有持仓 ──
            val toRemove = mutableListOf<SimPosition>()
            for (pos in positions) {
                var closed = false
                val ask = c.close + spread; val bid = c.close - spread
                val curPrice = if (pos.dir == 1) bid else ask
                val profitPips = (if (pos.dir == 1) bid - pos.openPrice else pos.openPrice - ask) / (c.close * 0.0001)

                // 固定SL/TP
                for (exit in config.exits) {
                    when (exit.exitType) {
                        ExitType.FIXED_SLTP -> {
                            if (pos.sl > 0 && curPrice <= pos.sl) { balance += calcPL(pos, curPrice, spread); closed = true }
                            if (pos.tp > 0 && curPrice >= pos.tp) { balance += calcPL(pos, curPrice, spread); closed = true }
                        }
                        ExitType.BREAKEVEN -> {
                            if (profitPips >= exit.breakevenPips && pos.sl == 0.0) pos.sl = pos.openPrice + if (pos.dir == 1) exit.breakevenLock * c.close * 0.0001 else -exit.breakevenLock * c.close * 0.0001
                            if (pos.sl > 0 && curPrice <= pos.sl) { balance += calcPL(pos, curPrice, spread); closed = true }
                        }
                        ExitType.TRAILING -> {
                            if (profitPips >= exit.trailingStart) {
                                val newSL = curPrice - if (pos.dir == 1) exit.trailingStep * c.close * 0.0001 else -exit.trailingStep * c.close * 0.0001
                                if (newSL > pos.sl || pos.sl == 0.0) pos.sl = newSL
                            }
                            if (pos.sl > 0 && curPrice <= pos.sl) { balance += calcPL(pos, curPrice, spread); closed = true }
                        }
                        ExitType.TIME_EXIT -> {
                            if (i - pos.openBar > exit.exitMinutes) { balance += calcPL(pos, curPrice, spread); closed = true }
                        }
                        ExitType.PARTIAL_CLOSE -> {
                            exit.partialTPs.forEachIndexed { j, tp ->
                                if (!pos.partialDone.getOrElse(j) { false } && profitPips >= tp.tpPoints) {
                                    val partialLot = pos.lots * tp.closePercent / 100.0
                                    val pl = (if (pos.dir == 1) c.close - pos.openPrice else pos.openPrice - c.close) * partialLot * 100000
                                    balance += pl
                                    pos.lots -= partialLot
                                    pos.partialDone = BooleanArray(exit.partialTPs.size) { k -> if (k == j) true else pos.partialDone.getOrElse(k) { false } }
                                    if (tp.moveSLToBE) pos.sl = pos.openPrice
                                }
                            }
                        }
                        ExitType.TRAILING_PROFIT -> {}
                        ExitType.MA_SL -> {}
                        ExitType.ATR_SL -> {}
                        ExitType.INDICATOR_EXIT -> {}
                        else -> {}
                    }
                    if (closed) break
                }
                if (closed || pos.lots <= 0.0) {
                    val exitPrice = if (pos.dir == 1) c.close - spread else c.close + spread
                    val profit = (if (pos.dir == 1) exitPrice - pos.openPrice else pos.openPrice - exitPrice) * pos.lots * 100000
                    trades += BTTrade(pos.openBar, candles[pos.openBar].date, pos.openPrice, i, c.date, exitPrice, pos.dir, profit, profit / balance * 100)
                    toRemove += pos
                }
            }
            positions.removeAll(toRemove)

            // ── 检查入场信号 ──
            if (positions.size < config.filter.maxPositions) {
                val sig = evalSignals(config, bufs, i, candles)
                if (sig != 0) {
                    val dir = sig
                    val lot = calcLot(config, balance, bufs, i)
                    val ask = c.close + spread; val bid = c.close - spread
                    val ep = if (dir == 1) ask else bid
                    var sl = 0.0; var tp = 0.0
                    config.exits.forEach { exit ->
                        if (exit.exitType == ExitType.FIXED_SLTP) {
                            val pt = c.close * 0.0001
                            if (exit.slPoints > 0) sl = if (dir == 1) ep - exit.slPoints * pt else ep + exit.slPoints * pt
                            if (exit.tpPoints > 0) tp = if (dir == 1) ep + exit.tpPoints * pt else ep - exit.tpPoints * pt
                        }
                    }
                    ticket++
                    val partialDone = BooleanArray(config.exits.filter { it.exitType == ExitType.PARTIAL_CLOSE }.flatMap { it.partialTPs }.size)
                    positions += SimPosition(ticket, dir, ep, i, lot, sl, tp, partialDone)
                }
            }
            equity += balance + positions.sumOf { calcPL(it, candles[i].close, spread) }
        }
        // 收盘平仓
        val last = candles.last()
        val lstSpr = last.close * 0.0001
        for (pos in positions) {
            val ep = if (pos.dir == 1) last.close - lstSpr else last.close + lstSpr
            val profit = (if (pos.dir == 1) ep - pos.openPrice else pos.openPrice - ep) * pos.lots * 100000
            trades += BTTrade(pos.openBar, candles[pos.openBar].date, pos.openPrice, n - 1, last.date, ep, pos.dir, profit, profit / balance * 100)
            balance += profit
        }

        return buildReport(candles, trades, balance, equity, config.strategyName)
    }

    private fun calcPL(pos: SimPosition, price: Double, spread: Double) =
        (if (pos.dir == 1) price - pos.openPrice else pos.openPrice - price) * pos.lots * 100000

    private fun evalSignals(cfg: StrategyConfig, buf: Buffers, i: Int, candles: List<Candle>): Int {
        if (cfg.entries.isEmpty()) return 0
        val close = DoubleArray(candles.size) { candles[it].close }
        val results = cfg.entries.map { e ->
            when (e.indicator) {
                IndicatorType.MA -> {
                    val f = buf.ma["${e.fastPeriod}_${e.maMethod}"] ?: return@map 0
                    val s = buf.ma["${e.slowPeriod}_${e.maMethod}"] ?: return@map 0
                    if (i < 1) 0 else if (f[i - 1] <= s[i - 1] && f[i] > s[i]) 1 else if (f[i - 1] >= s[i - 1] && f[i] < s[i]) -1 else 0
                }
                IndicatorType.PRICE -> {
                    val m = buf.ma["${e.fastPeriod}_${e.maMethod}"] ?: return@map 0
                    if (i < 1) 0 else if (close[i - 1] < m[i - 1] && close[i] > m[i]) 1 else if (close[i - 1] > m[i - 1] && close[i] < m[i]) -1 else 0
                }
                IndicatorType.RSI -> {
                    val r = buf.rsi[e.period] ?: return@map 0
                    if (r[i] < e.osLevel) 1 else if (r[i] > e.obLevel) -1 else 0
                }
                IndicatorType.MACD -> {
                    val m = buf.macdMain["${e.fastPeriod}_${e.slowPeriod}_${e.period}"] ?: return@map 0
                    val s = buf.macdSig["${e.fastPeriod}_${e.slowPeriod}_${e.period}"] ?: return@map 0
                    if (i < 1) 0 else if (m[i - 1] <= s[i - 1] && m[i] > s[i]) 1 else if (m[i - 1] >= s[i - 1] && m[i] < s[i]) -1 else 0
                }
                IndicatorType.BOLLINGER -> {
                    val u = buf.bbUpper["${e.bbPeriod}_${e.bbDeviation}"] ?: return@map 0
                    val l = buf.bbLower["${e.bbPeriod}_${e.bbDeviation}"] ?: return@map 0
                    if (close[i] <= l[i]) 1 else if (close[i] >= u[i]) -1 else 0
                }
                IndicatorType.ADX -> {
                    val a = buf.adx[e.period] ?: return@map 0
                    if (a[i] > e.adxLevel) 1 else 0
                }
                IndicatorType.CCI -> {
                    val c = buf.cci[e.period] ?: return@map 0
                    if (c[i] < -e.obLevel) 1 else if (c[i] > e.obLevel) -1 else 0
                }
                IndicatorType.PRICE -> {
                    val hh = (0 until e.lookbackBars).maxOfOrNull { candles[i - it].high } ?: return@map 0
                    val ll = (0 until e.lookbackBars).minOfOrNull { candles[i - it].low } ?: return@map 0
                    if (close[i] > hh) 1 else if (close[i] < ll) -1 else 0
                }
                else -> 0
            }
        }

        var buyOk = false; var sellOk = false
        results.forEachIndexed { idx, s ->
            val e = cfg.entries[idx]
            val bs = s > 0; val ss = s < 0
            if (idx == 0) { buyOk = bs; sellOk = ss }
            else {
                if (e.logicOp == LogicOp.AND) { buyOk = buyOk && bs; sellOk = sellOk && ss }
                else { buyOk = buyOk || bs; sellOk = sellOk || ss }
            }
            when (e.direction) { "BuyOnly" -> sellOk = false; "SellOnly" -> buyOk = false }
        }
        return if (buyOk && !sellOk) 1 else if (sellOk && !buyOk) -1 else 0
    }

    private fun calcLot(cfg: StrategyConfig, balance: Double, buf: Buffers, i: Int): Double {
        val m = cfg.money
        return when (m.mmType) {
            MoneyManagement.FIXED_LOT -> min(m.fixedLot, m.maxLot)
            MoneyManagement.RISK_PERCENT -> min(balance * m.riskPercent / 100 / 1000.0, m.maxLot)
            MoneyManagement.ATR_BASED -> {
                val a = buf.atr[m.atrLotPeriod]?.get(i) ?: return m.fixedLot
                min(balance * m.atrRiskPerN / (a * 100000), m.maxLot)
            }
            else -> m.fixedLot
        }
    }

    private fun buildReport(candles: List<Candle>, trades: List<BTTrade>, finalBalance: Double, equity: List<Double>, name: String): BTReport {
        val wins = trades.count { it.profit > 0 }
        val losses = trades.size - wins
        val grossProfit = trades.filter { it.profit > 0 }.sumOf { it.profit }
        val grossLoss = trades.filter { it.profit < 0 }.sumOf { it.profit }
        val netProfit = trades.sumOf { it.profit }
        val profitFactor = if (grossLoss != 0.0) abs(grossProfit / grossLoss) else 999.0
        val winRate = if (trades.isNotEmpty()) wins * 100.0 / trades.size else 0.0
        val avgWin = if (wins > 0) grossProfit / wins else 0.0
        val avgLoss = if (losses > 0) grossLoss / losses else 0.0
        val expect = if (trades.isNotEmpty()) netProfit / trades.size else 0.0

        var peak = 10000.0; var maxDD = 0.0; var maxDDPct = 0.0
        for (eq in equity) { if (eq > peak) peak = eq; val dd = peak - eq; if (dd > maxDD) maxDD = dd; val ddp = (peak - eq) / peak * 100; if (ddp > maxDDPct) maxDDPct = ddp }

        val returns = equity.windowed(2).map { (prev, cur) -> (cur - prev) / prev.coerceAtLeast(1.0) }
        val avgRet = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdRet = if (returns.isNotEmpty()) sqrt(returns.map { (it - avgRet).pow(2) }.average()) else 1.0
        val sharpe = if (stdRet > 0) avgRet / stdRet * sqrt(252.0) else 0.0

        return BTReport(name, candles.first().date, candles.last().date, candles.size, trades.size, wins, losses,
            winRate, grossProfit, grossLoss, netProfit, profitFactor, maxDD, maxDDPct, avgWin, avgLoss, expect, sharpe, equity, trades)
    }

    // ─── 指标计算 ───
    private fun sma(data: DoubleArray, period: Int): DoubleArray {
        val r = DoubleArray(data.size)
        var sum = 0.0
        for (i in data.indices) { sum += data[i]; if (i >= period) sum -= data[i - period]; r[i] = if (i >= period - 1) sum / period else Double.NaN }
        return r
    }

    private fun rsi(data: DoubleArray, period: Int): DoubleArray {
        val r = DoubleArray(data.size)
        var up = 0.0; var dn = 0.0
        for (i in 1 until data.size) {
            val ch = data[i] - data[i - 1]
            up += if (ch > 0) ch else 0.0; dn += if (ch < 0) -ch else 0.0
            if (i >= period) { val prv = data[i - period + 1] - data[i - period]; if (prv > 0) up -= prv else dn -= -prv }
            r[i] = if (i >= period) 100.0 - 100.0 / (1.0 + up / dn.coerceAtLeast(1e-10)) else Double.NaN
        }
        return r
    }

    private fun ema(data: DoubleArray, period: Int): DoubleArray {
        val r = DoubleArray(data.size); val k = 2.0 / (period + 1)
        var prev = data[0]; r[0] = prev
        for (i in 1 until data.size) { prev = data[i] * k + prev * (1 - k); r[i] = prev }
        return r
    }

    private fun macdLine(close: DoubleArray, fast: Int, slow: Int) = smaCloseOnly(close, fast).let { f ->
        smaCloseOnly(close, slow).let { s -> DoubleArray(close.size) { i -> f[i] - s[i] } } }

    private fun macdSignal(close: DoubleArray, fast: Int, slow: Int, sig: Int) = ema(macdLine(close, fast, slow), sig)

    private fun smaCloseOnly(data: DoubleArray, period: Int) = sma(data, period)

    private fun bollingerBand(close: DoubleArray, period: Int, dev: Double, upper: Boolean): DoubleArray {
        val sma = sma(close, period)
        val r = DoubleArray(close.size)
        for (i in period - 1 until close.size) {
            val mean = sma[i]; var variance = 0.0
            for (j in i - period + 1..i) variance += (close[j] - mean).pow(2)
            val std = sqrt(variance / period)
            r[i] = if (upper) mean + dev * std else mean - dev * std
        }
        for (i in 0 until period - 1) r[i] = Double.NaN
        return r
    }

    private fun atr(high: DoubleArray, low: DoubleArray, close: DoubleArray, period: Int): DoubleArray {
        val tr = DoubleArray(high.size) { if (it == 0) high[0] - low[0] else max(high[it] - low[it], max(abs(high[it] - close[it - 1]), abs(low[it] - close[it - 1]))) }
        return rma(tr, period)
    }

    private fun adx(high: DoubleArray, low: DoubleArray, close: DoubleArray, period: Int): DoubleArray {
        val dmPlus = DoubleArray(high.size); val dmMinus = DoubleArray(low.size); val tr = DoubleArray(high.size)
        for (i in 1 until high.size) {
            val upMv = high[i] - high[i - 1]; val dnMv = low[i - 1] - low[i]
            dmPlus[i] = if (upMv > dnMv && upMv > 0) upMv else 0.0
            dmMinus[i] = if (dnMv > upMv && dnMv > 0) dnMv else 0.0
            tr[i] = max(high[i] - low[i], max(abs(high[i] - close[i - 1]), abs(low[i] - close[i - 1])))
        }
        val atrR = rma(tr, period)
        val diPlus = DoubleArray(high.size); val diMinus = DoubleArray(high.size)
        var sp = 0.0; var sm = 0.0; var st = 0.0
        for (i in 1 until high.size) {
            sp += dmPlus[i]; sm += dmMinus[i]; st += tr[i]
            if (i > period) { sp -= dmPlus[i - period]; sm -= dmMinus[i - period]; st -= tr[i - period] }
            diPlus[i] = (sp / st * 100).coerceIn(0.0, 100.0); diMinus[i] = (sm / st * 100).coerceIn(0.0, 100.0)
        }
        val adx = DoubleArray(high.size)
        for (i in period..high.lastIndex) {
            val dx = abs(diPlus[i] - diMinus[i]) / (diPlus[i] + diMinus[i]).coerceAtLeast(1e-10) * 100
            adx[i] = if (i == period) dx else (adx[i - 1] * (period - 1) + dx) / period
        }
        return adx
    }

    private fun cci(hlc3: DoubleArray, period: Int): DoubleArray {
        val sma = sma(hlc3, period); val r = DoubleArray(hlc3.size)
        for (i in period - 1 until hlc3.size) {
            var mad = 0.0; val mean = sma[i]
            for (j in i - period + 1..i) mad += abs(hlc3[j] - mean)
            r[i] = (hlc3[i] - mean) / (0.015 * mad / period).coerceAtLeast(1e-10)
        }
        for (i in 0 until period - 1) r[i] = Double.NaN
        return r
    }

    private fun stochastic(high: DoubleArray, low: DoubleArray, close: DoubleArray, kPeriod: Int, dPeriod: Int, slowing: Int, isK: Boolean): DoubleArray {
        val rawK = DoubleArray(close.size)
        for (i in kPeriod - 1 until close.size) {
            val h = (i - kPeriod + 1..i).maxOf { high[it] }; val l = (i - kPeriod + 1..i).minOf { low[it] }
            rawK[i] = (close[i] - l) / (h - l).coerceAtLeast(1e-10) * 100
        }
        for (i in 0 until kPeriod - 1) rawK[i] = Double.NaN
        if (isK) return if (slowing > 1) sma(rawK, slowing) else rawK
        val smoothK = if (slowing > 1) sma(rawK, slowing) else rawK
        return sma(smoothK, dPeriod)
    }

    private fun parabolicSAR(high: DoubleArray, low: DoubleArray, step: Double, max: Double): DoubleArray {
        val sar = DoubleArray(high.size)
        var af = step; var ep = low[0]; var isLong = true; sar[0] = low[0]
        for (i in 1 until high.size) {
            val prevSAR = sar[i - 1]
            val newSAR = prevSAR + af * (ep - prevSAR)
            sar[i] = if (isLong) {
                val ns = min(newSAR, low[i - 1].coerceAtMost(low[i]))
                if (high[i] > ep) { ep = high[i]; af = min(af + step, max) }
                if (low[i] < ns) { isLong = false; sar[i] = ep; ep = low[i]; af = step }
                ns
            } else {
                val ns = max(newSAR, high[i - 1].coerceAtLeast(high[i]))
                if (low[i] < ep) { ep = low[i]; af = min(af + step, max) }
                if (high[i] > ns) { isLong = true; sar[i] = ep; ep = high[i]; af = step }
                ns
            }
        }
        return sar
    }

    private fun rma(data: DoubleArray, period: Int): DoubleArray {
        val r = DoubleArray(data.size); var sum = 0.0
        for (i in data.indices) { sum += data[i]; if (i >= period) sum -= data[i - period]; r[i] = if (i >= period - 1) sum / period else Double.NaN }
        return r
    }
}

fun Double.pow(n: Int) = this.let { var r = 1.0; repeat(n) { r *= it }; r }

private fun Double.pow(d: Double) = Math.pow(this, d)