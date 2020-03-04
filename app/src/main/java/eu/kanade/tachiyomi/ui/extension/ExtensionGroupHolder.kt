package eu.kanade.tachiyomi.ui.extension

import android.annotation.SuppressLint
import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.android.synthetic.main.extension_card_header.*

class ExtensionGroupHolder(view: View, adapter: FlexibleAdapter<*>) :
        BaseFlexibleViewHolder(view, adapter) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ExtensionGroupItem) {
        val context = itemView.context
        val name = when (item.status) {
            ExtensionGroupItem.Status.HAS_UPDATE -> context.getString(R.string.ext_updates_pending)
            ExtensionGroupItem.Status.INSTALLED -> context.getString(R.string.ext_installed)
            ExtensionGroupItem.Status.NOT_INSTALLED -> {
                item.lang?.let {
                    LocaleHelper.getDisplayName(item.lang, context)
                } ?: context.getString(R.string.ext_available)
            }
        }

        title.text = "$name (${item.size})"
    }
}
