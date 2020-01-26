package eu.kanade.tachiyomi.ui.recently_read

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.reader.ChapterLoadByNumber
import eu.kanade.tachiyomi.ui.reader.ChapterLoadBySource
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import java.util.Date

/**
 * Presenter of RecentlyReadFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class RecentlyReadPresenter(
        private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<RecentlyReadController>() {

    /**
     * Used to connect to database
     */
    val db: DatabaseHelper by injectLazy()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read manga
        getRecentMangaObservable()
                .subscribeLatestCache(RecentlyReadController::onNextManga)
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    fun getRecentMangaObservable(): Observable<List<RecentlyReadItem>> {
        // Set date for recent manga
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)

        return db.getRecentManga(cal.time).asRxObservable()
                .map { recents -> recents.map(::RecentlyReadItem) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Reset last read of chapter to 0L
     * @param history history belonging to chapter
     */
    fun removeFromHistory(history: History) {
        history.last_read = 0L
        db.updateHistoryLastRead(history).asRxObservable()
                .subscribe()
    }

    /**
     * Removes all chapters belonging to manga from history.
     * @param mangaId id of manga
     */
    fun removeAllFromHistory(mangaId: Long) {
        db.getHistoryByMangaId(mangaId).asRxSingle()
                .map { list ->
                    list.forEach { it.last_read = 0L }
                    db.updateHistoryLastRead(list).executeAsBlocking()
                }
                .subscribe()
    }

    /**
     * Retrieves the next chapter of the given one.
     *
     * @param chapter the chapter of the history object.
     * @param manga the manga of the chapter.
     */
    fun getNextChapter(chapter: Chapter, manga: Manga): Chapter? {
        if (!chapter.read) {
            return chapter
        }

        val allChapters = db.getChapters(manga).executeAsBlocking()

        return when (manga.order == Manga.ORDER_DEFAULT && preferences.optimizeChapterOrder()) {
            true -> ChapterLoadByNumber.getNextChapter(allChapters, chapter)
            false -> ChapterLoadBySource.getNextChapter(allChapters, chapter)
        }
    }

}
