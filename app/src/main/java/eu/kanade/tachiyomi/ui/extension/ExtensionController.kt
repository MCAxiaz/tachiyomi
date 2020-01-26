package eu.kanade.tachiyomi.ui.extension

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.support.v7.widget.queryTextChanges
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import kotlinx.android.synthetic.main.extension_controller.*


/**
 * Controller to manage the catalogues available in the app.
 */
open class ExtensionController : NucleusController<ExtensionPresenter>(),
        ExtensionAdapter.OnButtonClickListener,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        ExtensionTrustDialog.Listener {

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    private var query = ""

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_extensions)
    }

    override fun createPresenter(): ExtensionPresenter {
        return ExtensionPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.extension_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        ext_swipe_refresh.isRefreshing = true
        ext_swipe_refresh.refreshes().subscribeUntilDestroy {
            presenter.findAvailableExtensions()
        }

        // Initialize adapter, scroll listener and recycler views
        adapter = ExtensionAdapter(this)
        // Create recycler and set adapter.
        ext_recycler.layoutManager = LinearLayoutManager(view.context)
        ext_recycler.adapter = adapter
        ext_recycler.addItemDecoration(ExtensionDividerItemDecoration(view.context))
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extension_main, menu)

        // Initialize search option.
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        // Create query listener which opens the global search view.
        searchView.queryTextChanges()
                .filter { router.backstack.lastOrNull()?.controller() == this }
                .subscribeUntilDestroy {
                    setExtensions(it.toString())
                }

        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_settings -> {
                router.pushController((RouterTransaction.with(ExtensionFilterController()))
                        .popChangeHandler(SettingsExtensionsFadeChangeHandler())
                        .pushChangeHandler(FadeChangeHandler()))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isPush && handler is SettingsExtensionsFadeChangeHandler) {
            presenter.findAvailableExtensions()
        }
    }

    override fun onButtonClick(position: Int) {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        when (extension) {
            is Extension.Installed -> {
                if (!extension.hasUpdate) {
                    openDetails(extension)
                } else {
                    presenter.updateExtension(extension)
                }
            }
            is Extension.Available -> {
                presenter.installExtension(extension)
            }
            is Extension.Untrusted -> {
                openTrustDialog(extension)
            }
        }
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return false
        if (extension is Extension.Installed) {
            openDetails(extension)
        } else if (extension is Extension.Untrusted) {
            openTrustDialog(extension)
        }

        return false
    }

    override fun onItemLongClick(position: Int) {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        if (extension is Extension.Installed || extension is Extension.Untrusted) {
            uninstallExtension(extension.pkgName)
        }
    }

    private fun openDetails(extension: Extension.Installed) {
        val controller = ExtensionDetailsController(extension.pkgName)
        router.pushController(controller.withFadeTransaction())
    }

    private fun openTrustDialog(extension: Extension.Untrusted) {
        ExtensionTrustDialog(this, extension.signatureHash, extension.pkgName)
                .showDialog(router)
    }

    fun setExtensions() {
        ext_swipe_refresh?.isRefreshing = false
        adapter?.updateDataSet(presenter.getFilteredExtensions(query))
    }

    private fun setExtensions(query: String) {
        if (this.query != query) {
            this.query = query
            setExtensions()
        }
    }

    fun updateInstallStep(item: ExtensionItem) {
        adapter?.updateItem(item, item.installStep)
    }

    override fun trustSignature(signatureHash: String) {
        presenter.trustSignature(signatureHash)
    }

    override fun uninstallExtension(pkgName: String) {
        presenter.uninstallExtension(pkgName)
    }

    class SettingsExtensionsFadeChangeHandler : FadeChangeHandler()
}
