package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.INITIAL_URL
import de.romqu.schimmelhofapi.data.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.OffsetDateTime

@Service
class GetRidingLessonsTask(
    private val ridingLessonRepository: RidingLessonRepository,
    private val sessionRepository: SessionRepository
) {

    enum class Weekday(val rawName: String) {
        MONDAY("Monday"),
        TUESDAY("Tuesday"),
        WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"),
        FRIDAY("Friday"),
        SATURDAY("Saturday"),
        SUNDAY("Sunday"),
    }

    enum class BookedInputValue(val rawValue: String) {
        WARTELISTE("Warteliste"),
        STORNIEREN("stornieren"),
        WARTELISTE_STORNIEREN("Warteliste stornieren")
    }

    fun execute(session: SessionEntity) =
        getRidingLessonsBody(session)
            .convertBodyToHtmlDocument()
            .parseRidingLessonTableEntries()


    private fun getRidingLessonsBody(session: SessionEntity): Result<Error, RidingLessonRepository.GetRidingLessonsResponse> {

        val now = OffsetDateTime.now()

        val from =
            if (now.dayOfWeek == DayOfWeek.MONDAY) now else now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))

        val to = from.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

        return ridingLessonRepository.getRidingLessons(session).mapError(Error.Network)
    }

    private fun Result<Error, RidingLessonRepository.GetRidingLessonsResponse>.convertBodyToHtmlDocument()
        : Result<Error, Document> =
        flatMap { response ->
            try {
                val htmlDocument = Jsoup.parse(
                    response.responseBody.byteStream(),
                    Charsets.UTF_8.name(),
                    INITIAL_URL
                )
                Result.Success(htmlDocument)
            } catch (ex: IOException) {
                response.response.close()
                Result.Failure(Error.ConvertBodyToHtmlDocument)
            }
        }

    private fun Result<Error, Document>.parseRidingLessonTableEntries(): Result<Error, Map<Weekday, List<RidingLessonTableEntry>>> =
        map { document ->

            val entries = Weekday.values()
                .scan(listOf<RidingLessonTableEntry>()) { list, weekday ->

                    val tableForDayElement = document.body()
                        .getElementById("tbl${weekday.rawName}")

                    val bookableTableEntriesForDay = tableForDayElement
                        .select("td:not(.ausgebucht) div")
                        .flatMap { element ->

                            val inputElements = element.select("input")
                            val inputElementIdValue = inputElements.attr("id")
                            val isInputValueNotEmpty = inputElements.attr("value").isNotEmpty()
                            val state = if (isInputValueNotEmpty)
                                RidingLessonState.AVAILABLE else RidingLessonState.EXPIRED
                            val action = if (isInputValueNotEmpty)
                                RidingLessonAction.BOOK else RidingLessonAction.NONE

                            element.select("span:not(.ok)")
                                .map {
                                    val entry = RidingLessonTableEntry(
                                        weekday = weekday,
                                        title = it.textNodes()[0].wholeText,
                                        time = it.textNodes()[1].wholeText,
                                        teacher = it.textNodes()[2].wholeText,
                                        place = it.textNodes()[3].wholeText,
                                        state = state,
                                        action = action,
                                    )

                                    if (inputElementIdValue.isNotEmpty()) {
                                        val lessonCmd = inputElementIdValue.substringBefore("_")
                                        val lessonId = inputElementIdValue.substringAfter("_")
                                        entry.copy(lessonCmd = lessonCmd, lessonId = lessonId)
                                    } else entry
                                }

                        }

                    val bookedTableEntriesForDay = tableForDayElement
                        .select("td.ausgebucht div")
                        .flatMap { element ->

                            val inputElements = element.select("input")
                            val inputElementIdValue = inputElements.attr("id")
                            val inputValue = inputElements.attr("value")

                            element.select("span:not(.ok)")
                                .map {
                                    val tableEntry = RidingLessonTableEntry(
                                        weekday = weekday,
                                        title = it.textNodes()[0].wholeText,
                                        time = it.textNodes()[1].wholeText,
                                        teacher = it.textNodes()[2].wholeText,
                                        place = it.textNodes()[3].wholeText,
                                    )

                                    val tableEntryNext = if (inputValue.isNotEmpty()) {
                                        when (BookedInputValue.valueOf(inputValue.toUpperCase().replace(" ", "_"))) {
                                            BookedInputValue.STORNIEREN -> tableEntry.copy(
                                                state = RidingLessonState.BOOKED,
                                                action = RidingLessonAction.CANCEL_BOOKING
                                            )
                                            BookedInputValue.WARTELISTE ->
                                                tableEntry.copy(
                                                    state = RidingLessonState.BOOKED_OUT,
                                                    action = RidingLessonAction.ON_WAIT_LIST
                                                )
                                            BookedInputValue.WARTELISTE_STORNIEREN -> tableEntry.copy(
                                                state = RidingLessonState.WAIT_LIST,
                                                action = RidingLessonAction.CANCEL_WAIT_LIST
                                            )
                                        }
                                    } else tableEntry

                                    if (inputElementIdValue.isNotEmpty()) {
                                        val lessonCmd = inputElementIdValue.substringBefore("_")
                                        val lessonId = inputElementIdValue.substringAfter("_")
                                        tableEntryNext.copy(lessonCmd = lessonCmd, lessonId = lessonId)
                                    } else tableEntryNext
                                }
                        }

                    list.union(bookableTableEntriesForDay.union(bookedTableEntriesForDay)).toList()
                }
                .flatten()
                .distinct()
                .sortedBy(RidingLessonTableEntry::time)
                .groupBy(RidingLessonTableEntry::weekday)
                .toSortedMap(compareBy { it })


            entries
        }

    data class RidingLessonTableEntry(
        val weekday: Weekday,
        val title: String,
        val time: String,
        val teacher: String,
        val place: String,
        val lessonCmd: String = "",
        val lessonId: String = "",
        val state: RidingLessonState = RidingLessonState.EXPIRED,
        val action: RidingLessonAction = RidingLessonAction.NONE,
    )

    enum class RidingLessonState {
        EXPIRED,
        BOOKED_OUT,
        WAIT_LIST,
        BOOKED,
        AVAILABLE,
    }

    enum class RidingLessonAction {
        NONE,
        BOOK,
        ON_WAIT_LIST,
        CANCEL_BOOKING,
        CANCEL_WAIT_LIST
    }

    sealed class Error {
        object Network : Error()
        object ConvertBodyToHtmlDocument : Error()
    }

}