package app.doomsdaycountdown.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ROLLOVER,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refreshAll(context)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(rolloverIntent(context))
    }

    companion object {
        private const val ACTION_ROLLOVER = "app.doomsdaycountdown.widget.ROLLOVER"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
            if (ids.isNotEmpty()) refresh(context, manager, ids)
        }

        private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val now = System.currentTimeMillis()
            val parts = Countdown.parts(now)
            val views = buildViews(context, parts)
            ids.forEach { manager.updateAppWidget(it, views) }

            // The chronometer ticks the hours/minutes/seconds on its own; we only wake up
            // when the day count changes.
            if (!parts.finished) {
                context.getSystemService(AlarmManager::class.java).setWindow(
                    AlarmManager.RTC,
                    now + parts.remainderOfDayMillis + 500,
                    60_000,
                    rolloverIntent(context),
                )
            }
        }

        private fun buildViews(context: Context, parts: Countdown.Parts): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            views.setTextViewText(R.id.months, "%02d".format(parts.months))
            views.setTextViewText(R.id.days, "%02d".format(parts.days))

            if (parts.finished) {
                views.setChronometer(R.id.clock, SystemClock.elapsedRealtime(), "00:00:00", false)
            } else {
                views.setChronometer(
                    R.id.clock,
                    SystemClock.elapsedRealtime() + parts.remainderOfDayMillis,
                    null,
                    true,
                )
                views.setChronometerCountDown(R.id.clock, true)
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }

        private fun rolloverIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, CountdownWidget::class.java).setAction(ACTION_ROLLOVER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
