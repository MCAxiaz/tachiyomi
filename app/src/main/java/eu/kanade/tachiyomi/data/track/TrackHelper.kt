package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import java.util.Calendar

fun TrackItem.setAutoComplete(change: Change) {
    track!!.apply {
        when (change) {
            Change.CHAPTER -> {
                if (last_chapter_read == total_chapters && total_chapters != 0) {
                    status = service.getCompletionStatus()

                    setFinishedDateNow()
                }
            }
            Change.STATUS -> {
                if (status == service.getCompletionStatus()) {
                    if (total_chapters != 0) {
                        last_chapter_read = total_chapters
                    }

                    setFinishedDateNow()
                }
            }
        }
    }
}

private fun Track.setFinishedDateNow() {
    if (finished_reading_date == 0L) {
        finished_reading_date = Calendar.getInstance().timeInMillis
    }
}

enum class Change {
    CHAPTER, STATUS
}
