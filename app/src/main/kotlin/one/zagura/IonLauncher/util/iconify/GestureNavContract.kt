package one.zagura.IonLauncher.util.iconify

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.EXTRA_COMPONENT_NAME
import android.content.Intent.EXTRA_USER
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.Q)
class GestureNavContract(
    val componentName: ComponentName,
    val user: UserHandle,
    private val callback: Message
) {
    /**
     * Sends the position information to the receiver
     */
    fun sendEndPosition(
        position: RectF,
        context: FloatingIconView,
        surfaceControl: SurfaceControl,
    ) {
        if (messageReceiver == null)
            messageReceiver = StaticMessageReceiver()
        else
            messageReceiver!!.removeSelf()
        val result = Bundle().apply {
            putParcelable(EXTRA_ICON_POSITION, position)
            putParcelable(EXTRA_ICON_SURFACE, surfaceControl)
            putParcelable(
                EXTRA_ON_FINISH_CALLBACK,
                messageReceiver!!.setCurrentContext(context)
            )
        }
        val callback = Message.obtain().apply {
            copyFrom(this@GestureNavContract.callback)
            data = result
        }
        try {
            callback.replyTo.send(callback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error sending icon position", e)
        }
    }

    companion object {
        private var messageReceiver: StaticMessageReceiver? = null

        private const val TAG = "GestureNavContract"
        private const val EXTRA_GESTURE_CONTRACT: String = "gesture_nav_contract_v1"
        private const val EXTRA_ICON_POSITION: String = "gesture_nav_contract_icon_position"
        private const val EXTRA_ICON_SURFACE: String = "gesture_nav_contract_surface_control"
        private const val EXTRA_REMOTE_CALLBACK: String = "android.intent.extra.REMOTE_CALLBACK"
        private const val EXTRA_ON_FINISH_CALLBACK: String = "gesture_nav_contract_finish_callback"

        /**
         * Clears and returns the GestureNavContract if it was present in the intent.
         */
        fun fromIntent(intent: Intent): GestureNavContract? {
            val extras = intent.getBundleExtra(EXTRA_GESTURE_CONTRACT)
                ?: return null
            intent.removeExtra(EXTRA_GESTURE_CONTRACT)
            val componentName = extras.getParcelable<ComponentName>(EXTRA_COMPONENT_NAME)
                ?: return null
            val userHandle = extras.getParcelable<UserHandle>(EXTRA_USER)
                ?: return null
            val callback = extras.getParcelable<Message>(EXTRA_REMOTE_CALLBACK)
                ?: return null
            if (callback.replyTo == null)
                return null
            return GestureNavContract(componentName, userHandle, callback)
        }
    }

    private class StaticMessageReceiver : Handler.Callback {
        private val mMessenger = Messenger(Handler(Looper.getMainLooper(), this))
        private var mLastTarget: WeakReference<FloatingIconView?> = WeakReference(null)

        fun setCurrentContext(context: FloatingIconView?): Message {
            mLastTarget = WeakReference(context)
            val msg = Message.obtain()
            msg.replyTo = mMessenger
            msg.what = MSG_CLOSE_LAST_TARGET
            return msg
        }

        fun removeSelf() {
            val view = mLastTarget.get() ?: return
            mLastTarget.clear()
            view.removeSelf()
        }

        override fun handleMessage(message: Message): Boolean {
            if (message.what == MSG_CLOSE_LAST_TARGET) {
                removeSelf()
                return true
            }
            return false
        }

        companion object {
            private const val MSG_CLOSE_LAST_TARGET = 0
        }
    }
}