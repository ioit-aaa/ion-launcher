package one.zagura.IonLauncher.ui.smartspacer

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.kieronquinn.app.smartspacer.sdk.client.views.SmartspacerView
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

class CardPagerAdapter(
    private val interactionListener: SmartspacerBasePageView.SmartspaceTargetInteractionListener
) : PagerAdapter() {

    private val targets = mutableListOf<SmartspaceTarget>()
    private var smartspaceTargets = targets
    private val holders = SparseArray<ViewHolder>()

    private var tintColor: Int? = null
    private var padding: Int = 0
    private var forceReload = false

    fun setTargets(newTargets: List<SmartspaceTarget>) {
        targets.clear()
        targets.addAll(newTargets)
        notifyDataSetChanged()
    }

    fun applyCustomizations(tintColor: Int, padding: Int) {
        this.tintColor = tintColor
        this.padding = padding
        forceReload = true
        notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): ViewHolder {
        val target = smartspaceTargets[position]
        val card = SmartspacerView(container.context).apply {
            setTarget(target, interactionListener, tintColor, false)
        }
        val viewHolder = ViewHolder(position, card, target)
        onBindViewHolder(viewHolder)
        container.addView(card)
        holders.put(position, viewHolder)
        return viewHolder
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val viewHolder = obj as ViewHolder
        container.removeView(viewHolder.card)
        if (holders[position] == viewHolder) {
            holders.remove(position)
        }
    }

    fun getCardAtPosition(position: Int) = holders[position]?.card

    override fun getItemPosition(obj: Any): Int {
        val viewHolder = obj as ViewHolder
        val target = getTargetAtPosition(viewHolder.position)
        if (viewHolder.target === target && !forceReload) {
            return POSITION_UNCHANGED
        }
        forceReload = false
        if (target == null
            || target.featureType != viewHolder.target.featureType
            || target.smartspaceTargetId != viewHolder.target.smartspaceTargetId
        ) {
            return POSITION_NONE
        }
        viewHolder.target = target
        onBindViewHolder(viewHolder)
        return POSITION_UNCHANGED
    }

    fun getTargetAtPosition(position: Int): SmartspaceTarget? {
        if (position !in 0 until smartspaceTargets.size) {
            return null
        }
        return smartspaceTargets[position]
    }

    private fun onBindViewHolder(viewHolder: ViewHolder) {
        val target = smartspaceTargets[viewHolder.position]
        val card = viewHolder.card
        card.setTarget(target, interactionListener, tintColor, false)
        card.setPadding(padding, 0, padding, 0)
    }

    override fun getCount() = smartspaceTargets.size

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === (obj as ViewHolder).card
    }

    class ViewHolder internal constructor(
        val position: Int,
        val card: SmartspacerView,
        var target: SmartspaceTarget
    )
}
