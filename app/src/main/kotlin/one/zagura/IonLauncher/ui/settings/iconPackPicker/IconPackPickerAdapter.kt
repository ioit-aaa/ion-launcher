package one.zagura.IonLauncher.ui.settings.iconPackPicker

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder
import one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder.IconPackViewHolder
import one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder.SectionViewHolder
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import java.util.*

class IconPackPickerAdapter(
    val settings: Settings,
    private val chosenIconPacks: LinkedList<IconPackPickerActivity.IconPack>,
    private val availableIconPacks: LinkedList<IconPackPickerActivity.IconPack>,
    private val systemPack: IconPackPickerActivity.IconPack,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var itemTouchHelper: ItemTouchHelper

    override fun getItemViewType(i: Int) = when (i) {
        0 -> TITLE
        1, 2 + chosenIconPacks.size + 1 -> SECTION
        2 + chosenIconPacks.size -> SYSTEM_ICON_PACK
        else -> ICON_PACK
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = when (type) {
        TITLE -> TitleViewHolder(parent.context)
        SECTION -> SectionViewHolder(parent.context)
        SYSTEM_ICON_PACK -> IconPackViewHolder(parent.context, SYSTEM_ICON_PACK)
        else -> IconPackViewHolder(parent.context, type).apply {
            dragIndicator.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnClickListener {
                Utils.click(it.context)
                val i = bindingAdapterPosition
                if (i >= 2 && i < 2 + chosenIconPacks.size) {
                    val iconPack = chosenIconPacks.removeAt(i - 2)
                    availableIconPacks.add(0, iconPack)
                    notifyItemMoved(i, 2 + chosenIconPacks.size + 2)
                } else if (i >= 2 + chosenIconPacks.size + 2) {
                    val iconPack = availableIconPacks.removeAt(i - (2 + chosenIconPacks.size + 2))
                    chosenIconPacks.add(0, iconPack)
                    notifyItemMoved(i, 2)
                }
                settings.edit(it.context) {
                    "icon_packs" set chosenIconPacks.map(IconPackPickerActivity.IconPack::packageName)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) = when (i) {
        0 -> (holder as TitleViewHolder).bind(holder.itemView.context.getString(R.string.icon_packs))
        1 -> (holder as SectionViewHolder).bind(holder.itemView.context.getString(R.string.chosen))
        2 + chosenIconPacks.size + 1 -> (holder as SectionViewHolder).bind(holder.itemView.context.getString(R.string.available))
        2 + chosenIconPacks.size -> (holder as IconPackViewHolder).bind(systemPack)
        else -> {
            holder as IconPackViewHolder
            val pack = if (i > 2 + chosenIconPacks.size + 1)
                availableIconPacks[i - 2 - chosenIconPacks.size - 1 - 1]
            else chosenIconPacks[i - 2]
            holder.bind(pack)
        }
    }

    override fun getItemCount(): Int = 2 + chosenIconPacks.size + 2 + availableIconPacks.size

    fun onItemMove(
        context: Context,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val toPosition = target.bindingAdapterPosition
        if (target !is IconPackViewHolder && toPosition <= 1) {
            return false
        }
        if (viewHolder is IconPackViewHolder && viewHolder.type == SYSTEM_ICON_PACK) {
            return false
        }

        val fromPosition = viewHolder.bindingAdapterPosition
        val chosenIconPacksSize = chosenIconPacks.size
        val isFromAvailable = fromPosition >= 2 + chosenIconPacksSize + 1
        val isToAvailable = toPosition >= 2 + chosenIconPacksSize + 1

        val r = if (isFromAvailable) {
            if (isToAvailable)
                return false
            availableIconPacks.removeAt(fromPosition - 2 - chosenIconPacksSize - 2)
        } else
            chosenIconPacks.removeAt(fromPosition - 2)

        if (isToAvailable) {
            availableIconPacks.add(toPosition - 2 - chosenIconPacksSize - 1, r)
        } else {
            if (!isFromAvailable) {
                if (toPosition == 2 + chosenIconPacksSize) {
                    chosenIconPacks.add(fromPosition - 2, r)
                    return false
                }
            }
            chosenIconPacks.add(toPosition - 2, r)
        }
        notifyItemMoved(fromPosition, toPosition)
        settings.edit(context) {
            "icon_packs" set chosenIconPacks.map(IconPackPickerActivity.IconPack::packageName)
        }
        return true
    }

    companion object {
        const val ICON_PACK = 0
        const val SYSTEM_ICON_PACK = 1
        const val SECTION = 2
        const val TITLE = 3
    }
}