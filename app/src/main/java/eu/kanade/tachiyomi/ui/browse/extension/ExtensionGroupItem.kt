package eu.kanade.tachiyomi.ui.browse.extension

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

/**
 * Item that contains the extension group header.
 *
 * @param status For which type of extensions
 * @param size Number of extensions in this group
 * @param lang Language tag of the extension group
 */
data class ExtensionGroupItem(val status: Status, var size: Int, val lang: String? = null) : AbstractHeaderItem<ExtensionGroupHolder>() {

    enum class Status {
        HAS_UPDATE, INSTALLED, NOT_INSTALLED
    }

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.source_main_controller_card_header
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ExtensionGroupHolder {
        return ExtensionGroupHolder(view, adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: ExtensionGroupHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ExtensionGroupItem) {
            return status == other.status && lang == other.lang
        }
        return false
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (lang?.hashCode() ?: 0)
        return result
    }
}
