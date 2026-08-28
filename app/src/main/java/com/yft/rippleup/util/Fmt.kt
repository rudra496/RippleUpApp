package com.yft.rippleup.util

/** Formatting helpers + day math used by the guard. */
object Fmt {
    fun points(n: Int): String = "+$n pts"
    fun grouped(n: Int): String = java.text.DecimalFormat("#,###").format(n)
    fun co2(kg: Float): String = if (kg >= 1f) "%.1f kg CO₂e".format(kg) else "%d g CO₂e".format((kg * 1000).toInt())
    fun mmss(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)
}

fun dayStartMs(now: Long = System.currentTimeMillis()): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
