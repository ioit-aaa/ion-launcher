package one.zagura.IonLauncher.util.iconify

import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.data.items.LauncherItem

@RequiresApi(Build.VERSION_CODES.Q)
data class IconifyAnim(
    val item: LauncherItem,
    val bounds: RectF,
    val onAnimEnd: () -> Unit,
)