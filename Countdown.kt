package app.doomsdaycountdown.widget

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object Countdown {
    // Dec 18, 2026 05:00 UTC, plus the 18 second stream-sync offset from the web version.
    val TARGET_MILLIS: Long = Instant.parse("2026-12-18T05:00:18Z").toEpochMilli()

    const val DAY_MILLIS = 24L * 60 * 60 * 1000

    data class Parts(val months: Int, val days: Int, val remainderOfDayMillis: Long) {
        val finished get() = months == 0 && days == 0 && remainderOfDayMillis == 0L
    }

    /** Same month/day split as the web countdown, in the device's time zone. */
    fun parts(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Parts {
        if (nowMillis >= TARGET_MILLIS) return Parts(0, 0, 0)

        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val target = ZonedDateTime.ofInstant(Instant.ofEpochMilli(TARGET_MILLIS), zone)

        var months = (target.year - now.year) * 12 + (target.monthValue - now.monthValue)
        var anchor = now.plusMonths(months.toLong())
        if (anchor.toInstant().toEpochMilli() > TARGET_MILLIS) {
            months--
            anchor = now.plusMonths(months.toLong())
        }

        val diff = TARGET_MILLIS - anchor.toInstant().toEpochMilli()
        return Parts(
            months = months,
            days = (diff / DAY_MILLIS).toInt(),
            remainderOfDayMillis = diff % DAY_MILLIS,
        )
    }
}
