package one.zagura.IonLauncher.provider.summary

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import one.zagura.IonLauncher.data.summary.Event
import one.zagura.IonLauncher.provider.ColorThemer
import java.util.Calendar


object EventsLoader {

    fun load(context: Context): List<Event> {
        if (!hasPermission(context))
            return emptyList()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, (Calendar.getInstance()[Calendar.HOUR_OF_DAY] - 3).coerceAtLeast(0))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val eventsUri = CalendarContract.Instances.CONTENT_URI
            .buildUpon().apply {
                ContentUris.appendId(this, startOfDay.timeInMillis)
                ContentUris.appendId(this, startOfDay.apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                }.timeInMillis)
            }.build()
        val cur = context.contentResolver.query(
            eventsUri,
            arrayOf(
                CalendarContract.Instances._ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END),
            null, null,
            CalendarContract.Instances.BEGIN + " ASC",
        ) ?: return emptyList()
        val events = ArrayList<Event>(cur.count)
        while (cur.moveToNext()) {
            val id = cur.getLong(0)
            val title = cur.getString(1)
            val color = cur.getInt(2)
            val allDay = cur.getInt(3) != 0
            val begin = cur.getLong(4)
            val end = cur.getLong(5)
            if (events.any {
                it.title == title &&
                it.allDay == allDay &&
                it.begin == begin &&
                it.end == end })
                continue
            events.add(Event(id, title, ColorThemer.saturate(color), allDay, begin, end))
        }
        cur.close()
        return events
    }

    fun mightWantToSetAlarm(context: Context): Boolean {
        if (!hasPermission(context))
            return false
        if (Calendar.getInstance()[Calendar.HOUR_OF_DAY] < 19)
            return false
        val nextDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val eventsUri = CalendarContract.Instances.CONTENT_URI
            .buildUpon().apply {
                ContentUris.appendId(this, nextDay.timeInMillis)
                ContentUris.appendId(this, nextDay.apply {
                    add(Calendar.HOUR_OF_DAY, 12)
                }.timeInMillis)
            }.build()
        val cur = context.contentResolver.query(
            eventsUri,
            arrayOf(
                CalendarContract.Instances._ID,
                CalendarContract.Instances.BEGIN),
            CalendarContract.Instances.ALL_DAY+"=0", null, null
        ) ?: return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = alarmManager.nextAlarmClock
        try {
            if (cur.count == 0)
                return false
            while (cur.moveToNext()) {
                if (alarm == null)
                    return true
                val begin = cur.getLong(1)
                if (alarm.triggerTime <= begin)
                    return false
            }
        } finally { cur.close() }
        return true
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
}