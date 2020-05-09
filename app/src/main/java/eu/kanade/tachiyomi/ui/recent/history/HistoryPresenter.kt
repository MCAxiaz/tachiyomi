package eu.kanade.tachiyomi.ui.recent.history

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.reader.ChapterLoadByNumber
import eu.kanade.tachiyomi.ui.reader.ChapterLoadBySource
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.toDateKey
import java.util.Calendar
import java.util.Date
import java.util.TreeMap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of HistoryFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class HistoryPresenter(
    private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<HistoryController>() {

    /**
     * Used to connect to database
     */
    val db: DatabaseHelper by injectLazy()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read manga
        getRecentMangaObservable()
            .subscribeLatestCache(HistoryController::onNextManga)
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    fun getRecentMangaObservable(): Observable<List<HistoryItem>> {
        // Set date limit for recent manga
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        return db.getRecentManga(cal.time).asRxObservable()
            .map { recents ->
                val map = TreeMap<Date, MutableList<MangaChapterHistory>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = recents
                    .groupByTo(map, { it.history.last_read.toDateKey() })
                byDay.flatMap { entry ->
                    val dateItem = DateSectionItem(entry.key)
                    entry.value.map { HistoryItem(it, dateItem) }
                }
            }
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
