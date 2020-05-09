package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Chapter

/**
 * Load strategy using the source order. This is the default ordering.
 */
object ChapterLoadBySource {
    fun get(allChapters: List<Chapter>): List<Chapter> {
        return allChapters.sortedByDescending { it.source_order }
    }

    fun getNextChapter(allChapters: List<Chapter>, selectedChapter: Chapter): Chapter? {
        val nextSourceOrder = selectedChapter.source_order - 1
        return if (nextSourceOrder >= 0) {
            allChapters.find { it.source_order == nextSourceOrder }
        } else null
    }
}

/**
 * Load strategy using unique chapter numbers with same scanlator preference.
 */
object ChapterLoadByNumber {
    fun get(allChapters: List<Chapter>, selectedChapter: Chapter): List<Chapter> {
        val chaptersByNumber = allChapters.groupBy { it.chapter_number }

        return chaptersByNumber.map { chaptersForNumber ->
            val chapterNumber = chaptersForNumber.key
            val chapters = chaptersForNumber.value
            when (chapterNumber) {
                // Make sure the selected chapter is always present
                selectedChapter.chapter_number -> selectedChapter
                // Prefer a chapter of the same scanlator as the selected
                else ->
                    chapters.find { it.scanlator == selectedChapter.scanlator }
                        ?: chapters.first()
            }
        }.sortedBy { it.chapter_number }
    }

    fun getNextChapter(allChapters: List<Chapter>, selectedChapter: Chapter): Chapter? {
        var potentialNextChapters = allChapters.filter {
            it.chapter_number > selectedChapter.chapter_number
        }

        val nextChapterNumber = potentialNextChapters.minBy {
            it.chapter_number
        } ?: return null

        potentialNextChapters = potentialNextChapters.filter {
            it.chapter_number == nextChapterNumber.chapter_number
        }

        return potentialNextChapters.find {
            it.scanlator == selectedChapter.scanlator
        } ?: potentialNextChapters.first()
    }
}
