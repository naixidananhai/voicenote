package com.whispercpp.whisper

import android.util.Log
import java.io.BufferedReader
import java.io.FileReader

object WhisperCpuConfig {
    /**
     * 推荐的线程数
     * 
     * 可以通过修改 PREFERRED_THREADS 来手动设置线程数
     * - 设置为 0: 自动检测高性能CPU核心数
     * - 设置为 1-8: 使用指定的线程数
     * 
     * 建议:
     * - 高性能手机: 4-6 线程
     * - 普通手机: 2-4 线程
     * - 省电模式: 1-2 线程
     */
    private const val PREFERRED_THREADS = 4  // 手动设置线程数，0表示自动检测
    
    val preferredThreadCount: Int
        get() = if (PREFERRED_THREADS > 0) {
            PREFERRED_THREADS
        } else {
            // 自动检测高性能CPU核心数，至少使用2线程
            CpuInfo.getHighPerfCpuCount().coerceAtLeast(2)
        }
}

private class CpuInfo(private val lines: List<String>) {
    private fun getHighPerfCpuCount(): Int = try {
        getHighPerfCpuCountByFrequencies()
    } catch (e: Exception) {
        Log.d(LOG_TAG, "Couldn't read CPU frequencies", e)
        getHighPerfCpuCountByVariant()
    }

    private fun getHighPerfCpuCountByFrequencies(): Int =
        getCpuValues(property = "processor") { getMaxCpuFrequency(it.toInt()) }
            .also { Log.d(LOG_TAG, "Binned cpu frequencies (frequency, count): ${it.binnedValues()}") }
            .countDroppingMin()

    private fun getHighPerfCpuCountByVariant(): Int =
        getCpuValues(property = "CPU variant") { it.substringAfter("0x").toInt(radix = 16) }
            .also { Log.d(LOG_TAG, "Binned cpu variants (variant, count): ${it.binnedValues()}") }
            .countKeepingMin()

    private fun List<Int>.binnedValues() = groupingBy { it }.eachCount()

    private fun getCpuValues(property: String, mapper: (String) -> Int) = lines
        .asSequence()
        .filter { it.startsWith(property) }
        .map { mapper(it.substringAfter(':').trim()) }
        .sorted()
        .toList()


    private fun List<Int>.countDroppingMin(): Int {
        val min = min()
        return count { it > min }
    }

    private fun List<Int>.countKeepingMin(): Int {
        val min = min()
        return count { it == min }
    }

    companion object {
        private const val LOG_TAG = "WhisperCpuConfig"

        fun getHighPerfCpuCount(): Int = try {
            readCpuInfo().getHighPerfCpuCount()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Couldn't read CPU info", e)
            // Our best guess -- just return the # of CPUs minus 4.
            (Runtime.getRuntime().availableProcessors() - 4).coerceAtLeast(0)
        }

        private fun readCpuInfo() = CpuInfo(
            BufferedReader(FileReader("/proc/cpuinfo"))
                .useLines { it.toList() }
        )

        private fun getMaxCpuFrequency(cpuIndex: Int): Int {
            val path = "/sys/devices/system/cpu/cpu${cpuIndex}/cpufreq/cpuinfo_max_freq"
            val maxFreq = BufferedReader(FileReader(path)).use { it.readLine() }
            return maxFreq.toInt()
        }
    }
}