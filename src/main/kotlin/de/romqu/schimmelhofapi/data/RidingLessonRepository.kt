package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.*
import de.romqu.schimmelhofapi.shared.Result
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class RidingLessonRepository(
    private val httpCallDelegate: HttpCallDelegate,
    private val postCallDelegate: HttpPostCallDelegate,
) : HttpCall by httpCallDelegate, HttpPostCall by postCallDelegate {

    enum class CmdWeek(val symbol: String, val command: String) {
        SHOW_WEEK("anzeigen", "cmdSearch"),
        NEXT_WEEK(">>", "cmdNextWeek"),
        PREVIOUS_WEEK("<<", "cmdPrevWeek")
    }


    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private fun LocalDate.toDayMonthYear() = format(dayMonthYearFormatter)


    fun getRidingLessons(
        from: LocalDate,
        to: LocalDate,
        cmdWeek: CmdWeek,
        sessionEntity: SessionEntity,
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequest(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation,
                eventTarget = CmdWeek.SHOW_WEEK.command
            )
        }

        val requestString = buildGetRidingLessonsRequestString(
            from = from, to = to, cmdWeek = cmdWeek
        )


        val request = createPostRequest(
            INDEX_URL,
            addToRequestBody = requestString,
            requestData
        )

        return makeCall(request)
    }

    private fun buildGetRidingLessonsRequestString(
        from: LocalDate,
        to: LocalDate,
        cmdWeek: CmdWeek,
    ): String {

        val fromDateFormatted = from.toDayMonthYear()
        val toDateFormatted = to.toDayMonthYear()

        return "&rbList=Alle" +
            "&txtVon=$fromDateFormatted" +
            "&txtBis=$toDateFormatted" +
            "&${cmdWeek.command}=${cmdWeek.symbol}" +
            "&typ=Reitstd" +
            "&wunschpf=nein"
    }

    fun bookRidingLesson(
        ridingLessonId: String,
        sessionEntity: SessionEntity,
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequest(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation,
                eventArgument = "book%3B$ridingLessonId%3BReitstd%3Bnein%3B-1"
            )
        }

        val request = createPostRequest(INDEX_URL, "", requestData)

        return makeCall(request)
    }


    fun cancelRidingLesson(
        ridingLessonId: String,
        sessionEntity: SessionEntity,
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequest(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation,
            )
        }

        val requestString = buildCancelRidingLessonString(ridingLessonId)

        val request = createPostRequest(INDEX_URL, requestString, requestData)

        return makeCall(request)
    }

    private fun buildCancelRidingLessonString(
        ridingLessonId: String,
    ): String = "&cmdReitstd_$ridingLessonId=stornieren" +
        "&typ=Reitstd" +
        "&wunschpf=nein"
}