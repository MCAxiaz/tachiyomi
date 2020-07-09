package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class MangaImpl : Manga {

    override var id: Long? = null

    override var source: Long = -1

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var initialized: Boolean = false

    override var viewer: Int = 0

    override var chapter_flags: Int = 0

    override var cover_last_modified: Long = 0

    override fun copyFrom(other: SManga) {
        if (other.title.isNotBlank() && title != other.title) {
            val downloadManager = Injekt.get<DownloadManager>()
            downloadManager.renameManga(this, other.title)
            title = other.title
        }

        super.copyFrom(other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val manga = other as Manga

        return url == manga.url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}
