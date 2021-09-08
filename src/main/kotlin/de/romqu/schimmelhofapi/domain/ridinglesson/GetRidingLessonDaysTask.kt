package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.*
import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository.CmdWeek
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpCall
import de.romqu.schimmelhofapi.data.week.WeekEntity
import de.romqu.schimmelhofapi.domain.GetStateValuesTask
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Service
class GetRidingLessonDaysTask(
    private val ridingLessonRepository: RidingLessonRepository,
    private val getStateValuesTask: GetStateValuesTask,
    private val sessionRepository: SessionRepository,
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
        WARTELISTE_STORNIEREN("Warteliste stornieren"),
    }

    fun execute(
        forWeekEntities: List<WeekEntity>,
        session: SessionEntity,
    ): Result<Error, List<RidingLessonDayEntity>> =
        repeatForNumberOfWeeks(forWeekEntities) { week ->
            getRidingLessonsBody(week, session)
                .convertBodyToHtmlDocument()
                .getStateValuesFromIndexHtml(session)
                .updateSession()
                .parseRidingLessonTableEntries(week)
        }.mergeRidingLessonDayLists()

    private fun List<Result<Error, List<RidingLessonDayEntity>>>.mergeRidingLessonDayLists()
            : Result<Error, List<RidingLessonDayEntity>> =
        reduce { acc, result ->
            acc.flatMap { outerList ->
                result.map { innerList ->
                    outerList.union(innerList).toList()
                }
            }
        }

    private fun repeatForNumberOfWeeks(
        forWeekEntities: List<WeekEntity>,
        f: (weekEntity: WeekEntity) -> Result<Error, List<RidingLessonDayEntity>>,
    ): List<Result<Error, List<RidingLessonDayEntity>>> = forWeekEntities.map(f::invoke)

    private fun getRidingLessonsBody(
        forWeekEntity: WeekEntity,
        session: SessionEntity,
    ): Result<Error, HttpCall.Response> {

        val from = forWeekEntity.days.first()
        val to = forWeekEntity.days.last()

        return ridingLessonRepository.getRidingLessons(
            from = from,
            to = to,
            cmdWeek = CmdWeek.SHOW_WEEK,
            session
        ).mapError(Error.Network)
    }

    private fun Result<Error, HttpCall.Response>.convertBodyToHtmlDocument()
            : Result<Error, Document> =
        flatMap { response ->
            try {
                val htmlDocument = Jsoup.parse(
                    response.responseBody.byteStream(),
                    Charsets.UTF_8.name(),
                    INDEX_URL.toString()
                )

                ridingLessonRepository.closeConnection(response.responseBody)

                Result.Success(htmlDocument)
            } catch (ex: IOException) {
                ridingLessonRepository.closeConnection(response.responseBody)
                Result.Failure(Error.ConvertBodyToHtmlDocument)
            }
        }

    private fun Result<Error, Document>.getStateValuesFromIndexHtml(session: SessionEntity)
            : Result<Error, GetStateValuesFromIndexHtmlOut> =
        flatMap { document ->
            getStateValuesTask.execute(document)
                .mapError(Error.CouldNotParseSessionValuesFromIndexHtml) { stateValuesOut ->
                    GetStateValuesFromIndexHtmlOut(
                        session.copy(
                            viewState = stateValuesOut.viewState,
                            viewStateGenerator = stateValuesOut.viewStateGenerator,
                            eventValidation = stateValuesOut.eventValidation,
                        ),
                        document
                    )
                }
        }

    class GetStateValuesFromIndexHtmlOut(
        val session: SessionEntity,
        val document: Document,
    )

    private fun Result<Error, GetStateValuesFromIndexHtmlOut>.updateSession()
            : Result<Error, Document> = map { out ->
        sessionRepository.saveOrUpdate(out.session)
        out.document
    }

    private fun Result<Error, Document>.parseRidingLessonTableEntries(weekEntity: WeekEntity)
            : Result<Error, List<RidingLessonDayEntity>> = map { document ->

        weekEntity.days.zip(Weekday.values())
            .scan(listOf<RidingLessonDayEntity>()) { list, (date, weekday) ->

                val todayRidingLessonsTableEntries = document.body()
                    .getElementById("tbl${weekday.rawName}") ?: return@scan list


                val element1 = todayRidingLessonsTableEntries.select("tr td")


                /**
                 * ausgebucht
                 *      -> input
                 *          -> warteliste
                 *          -> stornieren (selbst gebucht)
                 *      -> kein input
                 *          -> abgelaufen
                 * nicht ausgebucht
                 *      -> input
                 *          -> buchen
                 *      -> kein input
                 *          -> abgelaufen
                 *
                 *
                 * input
                 *      -> warteliste
                 *      -> warteliste stornieren
                 *      -> stornieren
                 *      -> buchen
                 * kein input
                 *      -> abgelaufen
                 */

                element1.map {
                    if (it.hasClass("ausgebucht")) {
                        val isBookedUp = true
                        val first = it.selectFirst("td div")
                        //val classValue = first.attributes().get("class")
                        // first.select("span").textNodes()
                        first
                    } else {
                        val first = it.selectFirst("td div")
                        val classValue = first?.attributes()?.get("class")
                        first?.select("span")?.textNodes()
                    }
                }


                val bookableRidingLessons = todayRidingLessonsTableEntries
                    .getBookableRidingLessons(weekday, date)

                todayRidingLessonsTableEntries

                val bookedRidingLessons = todayRidingLessonsTableEntries
                    .getBookedRidingLessons(weekday, date)

                val ridingLessonsForDay = bookableRidingLessons.union(bookedRidingLessons)
                    .sortedBy(RidingLessonEntity::from)

                val ridingLessonDay = RidingLessonDayEntity(
                    weekday,
                    date,
                    ridingLessonsForDay
                )

                list.union(listOf(ridingLessonDay))
                    .toList()
            }.flatten().distinct()
    }

    private fun Element.getBookableRidingLessons(
        weekday: Weekday,
        date: LocalDate,
    ): List<RidingLessonEntity> = select("td:not(.ausgebucht) div")
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

                    val timeText = it.textNodes()[1]
                    val (from, to) = parseFromToTimeFromTimeText(timeText)

                    val ridingLesson = RidingLessonEntity(
                        weekday = weekday,
                        date = date,
                        title = it.textNodes()[0].wholeText,
                        from = from,
                        to = to,
                        teacher = it.textNodes()[2].wholeText,
                        place = it.textNodes()[3].wholeText,
                        state = state,
                        action = action,
                    )

                    if (inputElementIdValue.isNotEmpty()) {
                        val lessonCmd = inputElementIdValue.substringBefore("_")
                        val lessonId = inputElementIdValue.substringAfter("_")
                        ridingLesson.copy(lessonCmd = lessonCmd, lessonId = lessonId)
                    } else ridingLesson
                }

        }

    private fun Element.getBookedRidingLessons(
        weekday: Weekday,
        date: LocalDate,
    ): List<RidingLessonEntity> = select("td.ausgebucht div")
        .flatMap { element ->

            val inputElements = element.select("input")
            val inputElementIdValue = inputElements.attr("id")
            val inputValue = inputElements.attr("value")

            element.select("span:not(.ok)")
                .map {

                    val timeText = it.textNodes()[1]
                    val (from, to) = parseFromToTimeFromTimeText(timeText)

                    val ridingLesson = RidingLessonEntity(
                        weekday = weekday,
                        date = date,
                        title = it.textNodes()[0].wholeText,
                        from = from,
                        to = to,
                        teacher = it.textNodes()[2].wholeText,
                        place = it.textNodes()[3].wholeText,
                    )

                    val isBookedByUser = element.select("span[title=gebucht]")
                        .isNotEmpty()

                    val ridingLessonNext = when {
                        isBookedByUser && inputValue.isEmpty() -> {
                            ridingLesson.copy(
                                state = RidingLessonState.EXPIRED_BOOKED
                            )
                        }
                        inputValue.isNotEmpty() -> {
                            when (BookedInputValue.valueOf(
                                inputValue.uppercase(Locale.getDefault()).replace(" ", "_")
                            )) {
                                BookedInputValue.STORNIEREN -> ridingLesson.copy(
                                    state = RidingLessonState.BOOKED,
                                    action = RidingLessonAction.CANCEL_BOOKING
                                )
                                BookedInputValue.WARTELISTE ->
                                    ridingLesson.copy(
                                        state = RidingLessonState.BOOKED_OUT,
                                        action = RidingLessonAction.ON_WAIT_LIST
                                    )
                                BookedInputValue.WARTELISTE_STORNIEREN -> ridingLesson.copy(
                                    state = RidingLessonState.WAIT_LIST,
                                    action = RidingLessonAction.CANCEL_WAIT_LIST
                                )
                            }
                        }
                        else -> ridingLesson
                    }

                    if (inputElementIdValue.isNotEmpty()) {
                        val lessonCmd = inputElementIdValue.substringBefore("_")
                        val lessonId = inputElementIdValue.substringAfter("_")
                        ridingLessonNext.copy(lessonCmd = lessonCmd, lessonId = lessonId)
                    } else ridingLessonNext
                }
        }

    private fun parseFromToTimeFromTimeText(timeText: TextNode): Pair<LocalTime, LocalTime> {
        val fromToTimeValues = timeText.wholeText.split(" - ")


        val from = LocalTime.parse(fromToTimeValues.first())
        val to = LocalTime.parse(fromToTimeValues.last())
        return Pair(from, to)
    }

    sealed class Error {
        object Network : Error()
        object ConvertBodyToHtmlDocument : Error()
        object CouldNotParseSessionValuesFromIndexHtml : Error()
    }
}