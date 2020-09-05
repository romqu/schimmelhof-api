package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.INDEX_URL
import de.romqu.schimmelhofapi.core.Result
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.springframework.stereotype.Repository
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Repository
class RidingLessonRepository(private val httpClient: OkHttpClient) {

    enum class CmdWeek(val symbol: String, val command: String) {
        NEXTWEEK(">>", "cmdNextWeek"),
        PREVIOUS_WEEK("<<", "cmdPrevWeek")
    }

    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun getRidingLessonsResponse(
        cookie: String,
        cookieWeb: String,
        viewState: String,
        viewStateGenerator: String,
        eventValidation: String,
    ): Result<Error, GetRidingLessonsResponse> {

        val eventValidationWithAscii = eventValidation.replace("/", "%2F")
            .replace("+", "%2B")

        val request = Request.Builder()
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .url(INDEX_URL)
            .post(
                ("__EVENTTARGET=" +
                    "&__EVENTARGUMENT=" +
                    "&__LASTFOCUS=" +
                    "&__VIEWSTATE=$viewState" +
                    "&__VIEWSTATEGENERATOR=$viewStateGenerator" +
                    "&__EVENTVALIDATION=$eventValidationWithAscii" +
                    "&rbList=Alle" +
                    "&__EVENTTARGET=cmdSearch" +
                    "&txtVon=17.08.2020" +
                    "&txtBis=30.08.2020" +
                    "&cmdNextWeek=%3E%3E" +
                    "&typ=Reitstd" +
                    "&wunschpf=nein")
                    .toRequestBody(
                        "application/x-www-form-urlencoded".toMediaType()
                    )
            )
            .build()

        return makeCall(request)
    }

    fun getRidingLessonsResponse(
        from: OffsetDateTime,
        to: OffsetDateTime,
        cmdWeek: CmdWeek,
        cookie: String,
        cookieWeb: String,
        viewState: String,
        viewStateGenerator: String,
        eventValidation: String,
    ): Result<Error, GetRidingLessonsResponse> {

        val eventValidationWithAscii = eventValidation.replace("/", "%2F")
            .replace("+", "%2B")

        val fromFormatted = from.toDayMonthYear()
        val toFormatted = to.toDayMonthYear()

        val request = Request.Builder()
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .url(INDEX_URL)
            .post(
                ("__EVENTTARGET=" +
                    "&__EVENTARGUMENT=" +
                    "&__LASTFOCUS=" +
                    "&__VIEWSTATE=$viewState" +
                    "&__VIEWSTATEGENERATOR=$viewStateGenerator" +
                    "&__EVENTVALIDATION=$eventValidationWithAscii" +
                    "&rbList=Alle" +
                    "&__EVENTTARGET=cmdSearch" +
                    "&txtVon=$fromFormatted" +
                    "&txtBis=$toFormatted" +
                    "&${cmdWeek.command}=${cmdWeek.symbol}" +
                    "&typ=Reitstd" +
                    "&wunschpf=nein")
                    .toRequestBody("application/x-www-form-urlencoded".toMediaType())
            )
            .build()

        return makeCall(request)
    }

    private fun OffsetDateTime.toDayMonthYear() = format(dayMonthYearFormatter)

    class GetRidingLessonsResponse(
        val response: Response,
        val responseBody: ResponseBody
    )

    fun postBookRidingLessonResponse(
        ridingLessonId: String,
        cookie: String,
        cookieWeb: String,
        viewState: String,
        viewStateGenerator: String,
        eventValidation: String,
    ): Result<Error, GetRidingLessonsResponse> {

        val eventValidationWithAscii = eventValidation.replace("/", "%2F")
            .replace("+", "%2B")

        val request = Request.Builder()
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .url(INDEX_URL)
            .post(
                ("__EVENTTARGET=" +
                    "&__EVENTARGUMENT=book%3B$ridingLessonId%3BReitstd%3Bnein%3B-1" +
                    "&__LASTFOCUS=" +
                    "&__VIEWSTATE=$viewState" +
                    "&__VIEWSTATEGENERATOR=$viewStateGenerator" +
                    "&__EVENTVALIDATION=$eventValidationWithAscii" +
                    "&").toRequestBody(
                    "application/x-www-form-urlencoded".toMediaType()
                )
            )
            .build()

        return makeCall(request)
    }


    fun postCancelRidingLessonResponse(
        ridingLessonId: String,
        cookie: String,
        cookieWeb: String,
        viewState: String,
        viewStateGenerator: String,
        eventValidation: String,
    ): Result<Error, GetRidingLessonsResponse> {

        val eventValidationWithAscii = eventValidation.replace("/", "%2F")
            .replace("+", "%2B")

        val request = Request.Builder()
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .url(INDEX_URL)
            .post(
                ("__EVENTTARGET=" +
                    "&__EVENTARGUMENT=" +
                    "&__LASTFOCUS=" +
                    "&__VIEWSTATE=$viewState" +
                    "&__VIEWSTATEGENERATOR=$viewStateGenerator" +
                    "&__EVENTVALIDATION=$eventValidationWithAscii" +
                    "&cmdReitstd_$ridingLessonId=stornieren" +
                    "&typ=Reitstd" +
                    "&wunschpf=nein")
                    .toRequestBody(
                        "application/x-www-form-urlencoded".toMediaType()
                    )
            )
            .build()

        return makeCall(request)
    }

    private fun makeCall(request: Request): Result<Error, GetRidingLessonsResponse> = try {
        val response = httpClient.newCall(request).execute()

        val body = response.body

        if (response.isSuccessful && body != null) {
            Result.Success(GetRidingLessonsResponse(response, body))
        } else {
            response.close()
            Result.Failure(Error.ResponseUnsuccessful(response.code, response.message))
        }
    } catch (ex: IOException) {
        Result.Failure(Error.CallUnsuccessful)
    }

    sealed class Error {
        class ResponseUnsuccessful(val statusCode: Int, statusMessage: String) : Error()
        object CallUnsuccessful : Error()
    }
}