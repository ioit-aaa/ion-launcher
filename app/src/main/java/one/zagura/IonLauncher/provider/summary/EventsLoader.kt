package one.zagura.IonLauncher.provider.summary

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import one.zagura.IonLauncher.data.summary.Event
import one.zagura.IonLauncher.provider.ColorThemer
import java.util.Calendar


object EventsLoader {
    private val PROJECTION = arrayOf(
        CalendarContract.Instances._ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
    )

    fun load(context: Context): List<Event> {
        if (!hasPermission(context))
            return emptyList()
        val eventsUriBuilder = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, (Calendar.getInstance()[Calendar.HOUR_OF_DAY] - 3).coerceAtLeast(0))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        ContentUris.appendId(eventsUriBuilder, startOfDay.timeInMillis)
        ContentUris.appendId(eventsUriBuilder, startOfDay.apply {
            set(Calendar.HOUR_OF_DAY, 24)
        }.timeInMillis)
        val eventsUri = eventsUriBuilder.build()
        val cur = context.contentResolver.query(
            eventsUri,
            PROJECTION,
            null, null,
            CalendarContract.Instances.DTSTART + " ASC",
        ) ?: return emptyList()
        val events = ArrayList<Event>()
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

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
}