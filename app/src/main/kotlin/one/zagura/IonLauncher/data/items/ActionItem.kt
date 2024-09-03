package one.zagura.IonLauncher.data.items

import android.content.Intent
import android.graphics.Rect
import android.view.View

class ActionItem(val action: String) : LauncherItem() {

    override fun open(view: View, bounds: Rect) {
        super.open(view, bounds)
        val anim = createOpeningAnimation(view, bounds.left, bounds.top, bounds.right, bounds.bottom).toBundle()
        try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            view.context.startActivity(intent, anim)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun open(view: View) {
        super.open(view)
        val anim = createOpeningAnimation(view).toBundle()
        try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            view.context.startActivity(intent, anim)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun toString() = "${ACTION.toString(16)}$action"

    override fun hashCode() = action.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ActionItem
        return action == other.action
    }
}