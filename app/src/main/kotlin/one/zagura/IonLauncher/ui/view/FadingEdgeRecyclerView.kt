package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

open class FadingEdgeRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    override fun isPaddingOffsetRequired() = !clipToPadding

    override fun getLeftPaddingOffset() = if (clipToPadding) 0 else -paddingLeft

    override fun getTopPaddingOffset() = if (clipToPadding) 0 else -paddingTop

    override fun getRightPaddingOffset() = if (clipToPadding) 0 else paddingRight

    override fun getBottomPaddingOffset() = if (clipToPadding) 0 else paddingBottom
}