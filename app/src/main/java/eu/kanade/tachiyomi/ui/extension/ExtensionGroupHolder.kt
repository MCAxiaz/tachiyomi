package eu.kanade.tachiyomi.ui.extension

import android.annotation.SuppressLint
import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.LocaleHelper
import kotlinx.android.synthetic.main.extension_card_header.*

class ExtensionGroupHolder(view: View, adapter: FlexibleAdapter<*>) :
        BaseFlexibleViewHolder(view, adapter) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ExtensionGroupItem) {
        val context = itemView.context
        val name =
                if (item.installed) context.getString(R.string.ext_installed)
                else item.lang?.let{
                    LocaleHelper.getDisplayName(item.lang, context)
                } ?: context.getString(R.string.ext_available)

        title.text = "$name (${item.size})"
    }
}
