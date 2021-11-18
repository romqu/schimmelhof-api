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
import org.jsoup.nodes.TextNode
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

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

    enum class InputValue(val rawValue: String) {
        WARTELISTE("warteliste"),
        STORNIEREN("stornieren"),
        WARTELISTE_STORNIEREN("warteliste stornieren"),
        BUCHEN("buchen"),
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
    private fun Result<Error, Document>.parseRidingLessonTableEntries(weekEntity: WeekEntity)
            : Result<Error, List<RidingLessonDayEntity>> = map { document ->

        val today = LocalDate.now()

        weekEntity.days.zip(Weekday.values())
            .filter { (date, _) -> date.isEqual(today) || date.isAfter(today) }
            .scan(listOf<RidingLessonDayEntity>()) { list, (date, weekday) ->

                val todayRidingLessonsTableEntries = document.body()
                    .getElementById("tbl${weekday.rawName}") ?: return@scan list

                val ridingLessonsForDay = todayRidingLessonsTableEntries.select("tr td").mapNotNull {
                    it.getElementsByTag("input").first()
                }.map {
                    val parent = it.parent()
                    val inputElement = parent?.select("div input")?.first() ?: return@scan emptyList()
                    val inputElementIdValue = inputElement.attr("id")
                    val inputValueString = inputElement.attr("value").lowercase()
                    val inputValue = InputValue.values().first { value -> value.rawValue == inputValueString }
                    val (action, state) = when (inputValue) {
                        InputValue.WARTELISTE -> Pair(RidingLessonAction.ON_WAIT_LIST, RidingLessonState.BOOKED_OUT)
                        InputValue.STORNIEREN -> Pair(RidingLessonAction.CANCEL_BOOKING, RidingLessonState.BOOKED)
                        InputValue.WARTELISTE_STORNIEREN -> Pair(
                            RidingLessonAction.CANCEL_WAIT_LIST,
                            RidingLessonState.WAIT_LIST
                        )
                        InputValue.BUCHEN -> Pair(RidingLessonAction.BOOK, RidingLessonState.AVAILABLE)
                    }

                    val div = parent.selectFirst("div") ?: return@scan emptyList()
                    val classValue = div.attributes().get("class")
                    val textNodes = div.select("span").textNodes()

                    val timeText = textNodes[1] ?: return@scan emptyList()
                    val (from, to) = parseTimesFromText(timeText)

                    val lessonCmd = inputElementIdValue.substringBefore("_")
                    val lessonId = inputElementIdValue.substringAfter("_")

                    RidingLessonEntity(
                        lessonId = lessonId,
                        weekday = weekday,
                        date = date,
                        title = classValue,
                        from = from,
                        to = to,
                        teacher = textNodes[2]?.wholeText ?: "Unbekannt",
                        place = textNodes[3]?.wholeText ?: "Unbekannt",
                        state = state,
                        action = action,
                        lessonCmd = lessonCmd,
                    )
                }

                val ridingLessonDay = RidingLessonDayEntity(
                    weekday,
                    date,
                    ridingLessonsForDay
                )

                list.union(listOf(ridingLessonDay))
                    .toList()
            }.flatten().distinct()
    }

    private fun parseTimesFromText(timeText: TextNode): Pair<LocalTime, LocalTime> {
        val fromToTimeValues = timeText.wholeText.split(" - ")


        val from = LocalTime.parse(fromToTimeValues.first())
        val to = LocalTime.parse(fromToTimeValues.last())
        return Pair(from, to)
    }

    private fun List<Result<Error, List<RidingLessonDayEntity>>>.mergeRidingLessonDayLists()
            : Result<Error, List<RidingLessonDayEntity>> =
        reduce { acc, result ->
            acc.flatMap { outerList ->
                result.map { innerList ->
                    outerList.union(innerList).toList()
                }
            }
        }

    sealed class Error {
        object Network : Error()
        object ConvertBodyToHtmlDocument : Error()
        object CouldNotParseSessionValuesFromIndexHtml : Error()
    }
}