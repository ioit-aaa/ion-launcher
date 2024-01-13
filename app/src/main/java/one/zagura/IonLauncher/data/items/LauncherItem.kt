package one.zagura.IonLauncher.data.items

import android.app.ActivityOptions
import android.content.ContentUris
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.UserHandle
import android.provider.ContactsContract
import android.view.View
import androidx.annotation.CallSuper
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.ShortcutLoader
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager

sealed class LauncherItem {

    abstract val label: String

    /**
     * What to do when the item is clicked
     */
    @CallSuper
    open fun open(view: View, bounds: Rect) {
        SuggestionsManager.onItemOpened(view.context, this)
    }

    /**
     * What to do when the item is clicked
     *
     * @param view    The view that was clicked
     */
    @CallSuper
    open fun open(view: View) {
        SuggestionsManager.onItemOpened(view.context, this)
    }

    /**
     * Text representation of the item, used to save it
     */
    abstract override fun toString(): String

    companion object {
        const val APP = 0
        const val SHORTCUT = 1
        const val CONTACT = 2
        const val ACTION = 3

        fun createOpeningAnimation(view: View): Bundle {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.measuredWidth, view.measuredHeight).toBundle()
            else ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.measuredWidth, view.measuredHeight).toBundle()
        }

        fun createOpeningAnimation(view: View, startX: Int, startY: Int, width: Int, height: Int): Bundle {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ActivityOptions.makeClipRevealAnimation(view, startX, startY, width, height).toBundle()
            else ActivityOptions.makeScaleUpAnimation(view, startX, startY, width, height).toBundle()
        }

        fun decode(context: Context, data: String): LauncherItem? {
            if (data.length < 2)
                return null
            return when (data[0].digitToIntOrNull(16) ?: return null) {
                APP -> {
                    val content = data.substring(1)
                    val x = content.split('/')
                    if (x.size < 3)
                        return null
                    val user = UserHandle.readFromParcel(Parcel.obtain().apply { writeInt(x[2].toInt(16)) })
                    AppLoader.loadApp(context, x[0], x[1], user)
                }
                SHORTCUT -> {
                    val content = data.substring(1)
                    val x = content.split('/')
                    if (x.size < 3)
                        return null
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1)
                        return null
                    val user = UserHandle.readFromParcel(Parcel.obtain().apply { writeInt(x[2].toInt(16)) })
                    try {
                        ShortcutLoader.getShortcut(context, x[0], x[1], user)
                    } catch (_: SecurityException) { null }
                }
                CONTACT -> {
                    val content = data.substring(1)
                    val cur = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.STARRED,
                            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                            ContactsContract.Contacts.PHOTO_ID
                        ), "${ContactsContract.Contacts.LOOKUP_KEY} = ?", arrayOf(content), null)
                        ?: return null

                    if (cur.count == 0)
                        return null

                    if (!cur.moveToNext())
                        return null

                    val starred = cur.getInt(3) != 0
                    val lookupKey = cur.getString(0)
                    var name = cur.getString(1)
                    val phone = cur.getString(2)
                    val photoId = cur.getString(5)
                    val iconUri: Uri? = if (photoId != null) {
                        ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoId.toLong())
                    } else null

                    cur.close()

                    val nicknameCur = context.contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Nickname.NAME,
                            ContactsContract.Data.LOOKUP_KEY
                        ),
                        "(${ContactsContract.Data.MIMETYPE} = ?) AND (${ContactsContract.Data.LOOKUP_KEY} = ?)",
                        arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE, content), null)

                    if (nicknameCur != null) {
                        if (nicknameCur.count > 0) {
                            if (nicknameCur.moveToNext()) {
                                val nickname = nicknameCur.getString(0)
                                if (nickname != null)
                                    name = nickname
                            }
                        }
                        nicknameCur.close()
                    }

                    ContactItem(name, lookupKey, phone, starred, iconUri)
                }
                ACTION -> {
                    val i = data.indexOf('/')
                    if (i == -1)
                        return null
                    val action = data.substring(1, i)
                    val label = data.substring(i + 1)
                    ActionItem(action, label)
                }
                else -> null
            }
        }
    }
}