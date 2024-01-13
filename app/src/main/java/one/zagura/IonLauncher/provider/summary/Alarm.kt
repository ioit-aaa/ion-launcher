package one.zagura.IonLauncher.provider.summary

import android.app.AlarmManager
import android.content.Context
import one.zagura.IonLauncher.data.summary.NextAlarm
import java.util.Calendar

object Alarm {
    fun get(context: Context): NextAlarm? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock ?: return null
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        val alarmHour = Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }[Calendar.HOUR_OF_DAY]
        val diff = (alarmHour - hour + 24) % 24
        if (diff in 0..5)
            return NextAlarm(nextAlarm.showIntent, nextAlarm.triggerTime)
        return null
    }
}