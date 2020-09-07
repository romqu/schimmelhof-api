package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.HttpCallRequestData
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.shared.Result
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Repository
class RidingLessonRepository(
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {

    enum class CmdWeek(val symbol: String, val command: String) {
        NEXTWEEK(">>", "cmdNextWeek"),
        PREVIOUS_WEEK("<<", "cmdPrevWeek")
    }

    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")


    fun getRidingLessons(
        from: OffsetDateTime,
        to: OffsetDateTime,
        cmdWeek: CmdWeek,
        sessionEntity: SessionEntity
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequestData(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation,
                eventArgument = "cmdSearch"
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
        from: OffsetDateTime,
        to: OffsetDateTime,
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

    private fun OffsetDateTime.toDayMonthYear() = format(dayMonthYearFormatter)

    fun bookRidingLesson(
        ridingLessonId: String,
        sessionEntity: SessionEntity,
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequestData(
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
        sessionEntity: SessionEntity
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequestData(
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