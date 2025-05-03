package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.view.DragEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppCategorizer
import one.zagura.IonLauncher.ui.view.CategoryBoxView
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.ui.view.SharedDrawingContext

class LibraryAdapter(
    val showDropTargets: () -> Unit,
    val onItemOpened: (LauncherItem) -> Unit,
    val activity: Activity,
    val drawCtx: SharedDrawingContext,
    val openCategory: (AppCategorizer.AppCategory, List<App>, CategoryBoxView) -> Unit,
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var categories = emptyList<Pair<AppCategorizer.AppCategory, List<App>>>()

    class ViewHolder(
        val view: CategoryBoxView
    ) : RecyclerView.ViewHolder(view)

    override fun getItemId(i: Int) = getItem(i).first.ordinal.toLong()

    override fun getItemCount() = categories.size

    private fun getItem(i: Int) = categories[i]

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        ViewHolder(CategoryBoxView(parent.context, drawCtx, onItemOpened)).apply {
            view.setDragState { v -> v }
            view.setOnDragListener { v, e ->
                if (e.action == DragEvent.ACTION_DRAG_EXITED) {
                    if (e.localState === v) {
                        LongPressMenu.dismissCurrent()
                        showDropTargets()
                    }
                } else if (e.action == DragEvent.ACTION_DRAG_ENDED) {
                    if (e.localState === v)
                        LongPressMenu.onDragEnded()
                } else if (e.action == DragEvent.ACTION_DRAG_LOCATION && e.localState != v) {
                    LongPressMenu.dismissCurrent()
                    showDropTargets()
                }
                true
            }
            view.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                openCategory(item.first, item.second, view)
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        val (category, apps) = getItem(i)
        holder.view.setApps(apps)
        holder.view.category = category
    }

    fun update(categories: List<Pair<AppCategorizer.AppCategory, List<App>>>) {
        this.categories = categories
        notifyDataSetChanged()
    }
}