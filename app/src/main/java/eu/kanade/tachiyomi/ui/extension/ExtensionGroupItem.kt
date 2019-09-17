package eu.kanade.tachiyomi.ui.extension

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.kanade.tachiyomi.R
import java.util.Objects

/**
 * Item that contains the extension group header.
 *
 * @param installed For installed or uninstalled extensions
 * @param size Number of extensions in this group
 * @param lang Language tag of the extension group
 */
data class ExtensionGroupItem(val installed: Boolean, val size: Int, val lang: String? = null) : AbstractHeaderItem<ExtensionGroupHolder>() {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.extension_card_header
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): ExtensionGroupHolder {
        return ExtensionGroupHolder(view, adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: ExtensionGroupHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ExtensionGroupItem) {
            return installed == other.installed && lang == other.lang
        }
        return false
    }

    override fun hashCode(): Int {
        var result = installed.hashCode()
        result = 31 * result + (lang?.hashCode() ?: 0)
        return result
    }

}
