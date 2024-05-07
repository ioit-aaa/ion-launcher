package one.zagura.IonLauncher.provider.summary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog.Calls
import android.provider.CallLog.Calls.USER_MISSED_CALL_SCREENING_SERVICE_SILENCED

object MissedCalls {

    fun get(context: Context, after: Long): Int {
        if (!hasPermission(context))
            return 0
        val cur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.contentResolver.query(
                Calls.CONTENT_URI,
                arrayOf(
                    Calls.MISSED_REASON,
                    Calls.TYPE,
                    Calls.DATE),
                Calls.TYPE + "=" + Calls.MISSED_TYPE + " and " +
                        Calls.DATE + ">?" + " and " +
                        Calls.MISSED_REASON  + "<>" + USER_MISSED_CALL_SCREENING_SERVICE_SILENCED,
                arrayOf(after.toString()), null
            )
        } else {
            context.contentResolver.query(
                Calls.CONTENT_URI,
                arrayOf(
                    Calls.TYPE,
                    Calls.DATE),
                Calls.TYPE + "=" + Calls.MISSED_TYPE + " and " + Calls.DATE + ">?",
                arrayOf(after.toString()), null
            )
        } ?: return 0
        val total = cur.count
        cur.close()
        return total
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
}