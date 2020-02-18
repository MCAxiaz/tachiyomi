package eu.kanade.tachiyomi.ui.extension

import android.os.Bundle
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

private typealias ExtensionTuple
        = Triple<List<Extension.Installed>, List<Extension.Untrusted>, List<Extension.Available>>

/**
 * Presenter of [ExtensionController].
 */
open class ExtensionPresenter(
        private val extensionManager: ExtensionManager = Injekt.get(),
        private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<ExtensionController>() {

    private var extensions = emptyList<Pair<ExtensionGroupItem, List<ExtensionItem>>>()

    private var currentDownloads = hashMapOf<String, InstallStep>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        extensionManager.findAvailableExtensions()
        bindToExtensionsObservable()
    }

    private fun bindToExtensionsObservable(): Subscription {
        val installedObservable = extensionManager.getInstalledExtensionsObservable()
        val untrustedObservable = extensionManager.getUntrustedExtensionsObservable()
        val availableObservable = extensionManager.getAvailableExtensionsObservable()
                .startWith(emptyList<Extension.Available>())

        return Observable.combineLatest(installedObservable, untrustedObservable, availableObservable)
        { installed, untrusted, available -> Triple(installed, untrusted, available) }
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(::toItems)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache({view, _ ->
                    view.setExtensions()
                })
    }

    @Synchronized
    private fun toItems(tuple: ExtensionTuple): List<ExtensionItem> {
        val activeLangs = preferences.enabledLanguages().getOrDefault()

        val (installed, untrusted, available) = tuple

        val items = mutableListOf<Pair<ExtensionGroupItem, List<ExtensionItem>>>()

        val updatePartition = installed.partition { it.hasUpdate }
        val updatesSorted = updatePartition.first.sortedBy { it.pkgName }
        val installedSorted = updatePartition.second.sortedWith(compareBy({ !it.isObsolete }, { it.pkgName }))

        val untrustedSorted = untrusted.sortedBy { it.pkgName }
        val availableSorted = available
                // Filter out already installed extensions
                .filter { avail -> installed.none { it.pkgName == avail.pkgName }
                        && untrusted.none { it.pkgName == avail.pkgName }
                        && (avail.lang in activeLangs || avail.lang == "all")
                }
                .sortedBy { it.pkgName }

        if (updatesSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(ExtensionGroupItem.Status.HAS_UPDATE, updatesSorted.size)
            val updatePendingItems = updatesSorted.map { extension ->
                ExtensionItem(extension, header)
            }
            items += Pair(header, updatePendingItems)
        }
        if (installedSorted.isNotEmpty() || untrustedSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(ExtensionGroupItem.Status.INSTALLED, installedSorted.size + untrustedSorted.size)
            val installedItems = mutableListOf<ExtensionItem>()
            items += Pair(header, installedItems)

            installedItems += installedSorted.map { extension ->
                ExtensionItem(extension, header)
            }
            installedItems += untrustedSorted.map { extension ->
                ExtensionItem(extension, header)
            }
        }
        if (availableSorted.isNotEmpty()) {
            availableSorted
                    .groupBy { it.lang }
                    .forEach {
                        val header = ExtensionGroupItem(ExtensionGroupItem.Status.NOT_INSTALLED, it.value.size, it.key)
                        val langItems = mutableListOf<ExtensionItem>()
                        items += Pair(header, langItems)

                        langItems += it.value.map { extension ->
                            ExtensionItem(extension, header, currentDownloads[extension.pkgName])
                        }
                    }
        }

        this.extensions = items
        return extensionsAsList()
    }

    fun getFilteredExtensions(query: String): List<ExtensionItem> {
        return extensions.flatMap {
            val filteredItems = it.second.filter { extensionItem ->
                extensionItem.extension.name.contains(query, true)
            }
            it.first.size = filteredItems.size
            filteredItems
        }
    }

    private fun extensionsAsList(): List<ExtensionItem> {
        return extensions.flatMap {
            it.second
        }
    }

    @Synchronized
    private fun updateInstallStep(extension: Extension, state: InstallStep): ExtensionItem? {
        if (state == InstallStep.Installed) return null

        return extensionsAsList().find {
            it.extension.pkgName == extension.pkgName
        }?.copy(installStep = state)
    }

    fun installExtension(extension: Extension.Available) {
        extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun updateExtension(extension: Extension.Installed) {
        extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
    }

    private fun Observable<InstallStep>.subscribeToInstallUpdate(extension: Extension) {
        this.doOnNext { currentDownloads[extension.pkgName] = it }
                .doOnUnsubscribe { currentDownloads.remove(extension.pkgName) }
                .map { state -> updateInstallStep(extension, state) }
                .subscribeWithView({ view, item ->
                    if (item != null) {
                        view.updateInstallStep(item)
                    }
                })
    }

    fun uninstallExtension(pkgName: String) {
        extensionManager.uninstallExtension(pkgName)
    }

    fun findAvailableExtensions() {
        extensionManager.findAvailableExtensions()
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }

}
