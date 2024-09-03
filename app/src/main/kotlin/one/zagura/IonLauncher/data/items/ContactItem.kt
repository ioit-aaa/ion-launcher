package one.zagura.IonLauncher.data.items

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.provider.ContactsContract
import android.view.View

class ContactItem(
    val lookupKey: String,
    val phone: String?,
    val isFavorite: Boolean,
    val iconUri: Uri?,
) : LauncherItem() {

    override fun open(view: View, bounds: Rect) {
        super.open(view, bounds)
        val anim = createOpeningAnimation(view, bounds.left, bounds.top, bounds.right, bounds.bottom).toBundle()
        val viewContact = Intent(Intent.ACTION_VIEW)
        viewContact.data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        view.context.startActivity(viewContact, anim)
    }

    override fun open(view: View) {
        super.open(view)
        val anim = createOpeningAnimation(view).toBundle()
        val viewContact = Intent(Intent.ACTION_VIEW)
        viewContact.data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        view.context.startActivity(viewContact, anim)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ContactItem
        if (lookupKey != other.lookupKey) return false
        return phone == other.phone
    }

    override fun hashCode() = lookupKey.hashCode()

    override fun toString() = "${CONTACT.toString(16)}$lookupKey"
}