package de.romqu.schimmelhofapi.domain


import de.romqu.schimmelhofapi.SET_COOKIE_HEADER
import de.romqu.schimmelhofapi.data.UserRepository
import de.romqu.schimmelhofapi.data.WebpageRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.data.shared.constant.INITIAL_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpCall
import de.romqu.schimmelhofapi.data.week.WeekRepository
import de.romqu.schimmelhofapi.domain.ridinglesson.GetRidingLessonDaysTask
import de.romqu.schimmelhofapi.shared.*
import okhttp3.Headers
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URL


@Service
class LoginService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val getStateValuesTask: GetStateValuesTask,
    private val webpageRepository: WebpageRepository,
    private val getRidingLessonDaysTask: GetRidingLessonDaysTask,
    private val weekRepository: WeekRepository,
) {

    fun execute(username: String, passwordPlain: String): Result<Error, SessionEntity> =
        webpageRepository.getHomePage()
            .getInitialSanitizedCookie()
            .getHtmlDocumentFromBody()
            .getStateValuesFromInitialHtml()
            .doLogin(username, passwordPlain)
            .getSanitizedCookieWeb()
            .getHtmlDocumentFromIndexBody()
            .getSateValuesFromIndexHtml()
            .saveSession()

    private fun Result<HttpCall.Error, HttpCall.Response>.getInitialSanitizedCookie()
        : Result<Error, GetInitialSanitizedCookieOut> =
        doOn({ response ->
            val setCookieHeaderValue = response.headers.getSetCookieValue()?.sanitizeCookie()

            if (setCookieHeaderValue != null)
                Result.Success(
                    GetInitialSanitizedCookieOut(
                        setCookieHeaderValue,
                        response.responseBody
                    )
                )
            else Result.Failure(Error.CookieDoesNotExist)
        }, { error ->
            when (error) {
                is HttpCall.Error.ResponseUnsuccessful ->
                    Result.Failure(Error.Network(statusCode = error.statusCode))
                is HttpCall.Error.CallUnsuccessful ->
                    Result.Failure(Error.Network(error.message))
            }
        })

    class GetInitialSanitizedCookieOut(val initialSanitizedCookie: String, val responseBody: ResponseBody)

    private fun Result<Error, GetInitialSanitizedCookieOut>.getHtmlDocumentFromBody(): Result<Error, GetHtmlDocumentFromBodyOut> =
        flatMap { out ->
            try {
                val htmlDocument = out.responseBody.convertToDocument(INITIAL_URL)

                webpageRepository.closeConnection(out.responseBody)

                Result.Success(GetHtmlDocumentFromBodyOut(out.initialSanitizedCookie, htmlDocument))

            } catch (ex: IOException) {
                Result.Failure(Error.CouldNotParseResponseBody)
            }
        }

    class GetHtmlDocumentFromBodyOut(val sanitizedCookie: String, val htmlDocument: Document)

    private fun Result<Error, GetHtmlDocumentFromBodyOut>.getStateValuesFromInitialHtml()
        : Result<Error, GetSessionValuesFromHtmlOut> =
        flatMap { out ->
            getStateValuesTask.execute(out.htmlDocument)
                .mapError(Error.CouldNotParseSessionValuesFromInitialHtml) { taskOut ->
                    GetSessionValuesFromHtmlOut(
                        sanitizedCookie = out.sanitizedCookie,
                        viewState = taskOut.viewState,
                        viewStateGenerator = taskOut.viewStateGenerator,
                        eventValidation = taskOut.eventValidation
                    )
                }
        }

    class GetSessionValuesFromHtmlOut(
        val sanitizedCookie: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
    )

    private fun Result<Error, GetSessionValuesFromHtmlOut>.doLogin(
        userName: String,
        passwordPlain: String,
    ): Result<Error, DoLoginOut> = flatMap { out ->

        val session = SessionEntity(
            viewState = out.viewState,
            viewStateGenerator = out.viewStateGenerator,
            eventValidation = out.eventValidation,
            cookie = out.sanitizedCookie
        )

        userRepository.login(
            username = userName,
            password = passwordPlain,
            session = session
        ).doOnResult({ loginHeaders ->
            DoLoginOut(
                session = session,
                loginHeaders = loginHeaders
            )
        }, { error ->
            when (error) {

                is HttpCall.Error.ResponseUnsuccessful ->
                    Error.Network(statusCode = error.statusCode)
                is HttpCall.Error.CallUnsuccessful ->
                    Error.Network(error.message)
            }
        })
    }

    class DoLoginOut(
        val session: SessionEntity,
        val loginHeaders: Headers,
    )

    private fun Result<Error, DoLoginOut>.getSanitizedCookieWeb()
        : Result<Error, SessionEntity> = flatMap { out ->
        val sanitizedCookieWeb = out.loginHeaders.getSetCookieValue()?.sanitizeCookie()

        if (sanitizedCookieWeb != null)
            Result.Success(out.session.copy(cookieWeb = sanitizedCookieWeb))
        else Result.Failure(Error.CookieWebDoesNotExist)

    }

    private fun Headers.getSetCookieValue(): String? = this[SET_COOKIE_HEADER]

    private fun String.sanitizeCookie(): String = substringBefore(";")

    private fun Result<Error, SessionEntity>.getHtmlDocumentFromIndexBody()
        : Result<Error, GetHtmlDocumentFromIndexBodyOut> =
        flatMap { session ->
            webpageRepository.getIndexPage(session)
                .doOnResult({ httpResponse ->
                    val indexHtmlDocument = httpResponse.responseBody.convertToDocument(INDEX_URL)
                    webpageRepository.closeConnection(httpResponse.responseBody)
                    GetHtmlDocumentFromIndexBodyOut(
                        session = session,
                        indexHtmlDocument = indexHtmlDocument
                    )
                }, { error ->
                    when (error) {
                        is HttpCall.Error.ResponseUnsuccessful ->
                            Error.Network(statusCode = error.statusCode)
                        is HttpCall.Error.CallUnsuccessful ->
                            Error.Network(error.message)
                    }
                })
        }

    class GetHtmlDocumentFromIndexBodyOut(
        val session: SessionEntity,
        val indexHtmlDocument: Document,
    )

    private fun Result<Error, GetHtmlDocumentFromIndexBodyOut>.getSateValuesFromIndexHtml()
        : Result<Error, SessionEntity> =
        flatMap { out ->
            getStateValuesTask.execute(out.indexHtmlDocument)
                .mapError(Error.CouldNotParseSessionValuesFromIndextHtml) { stateValuesOut ->
                    out.session.copy(
                        viewState = stateValuesOut.viewState,
                        viewStateGenerator = stateValuesOut.viewStateGenerator,
                        eventValidation = stateValuesOut.eventValidation,
                    )
                }
        }

    private fun Result<Error, SessionEntity>.saveSession(): Result<Error, SessionEntity> =
        map(sessionRepository::saveOrUpdate)

    private fun ResponseBody.convertToDocument(url: URL) = Jsoup.parse(
        byteStream(),
        Charsets.UTF_8.name(),
        url.toString()
    )

    sealed class Error {
        data class Network(val message: String = "", val statusCode: Int = -1) : Error()
        object CookieDoesNotExist : Error()
        object CouldNotParseResponseBody : Error()
        object CouldNotParseSessionValuesFromInitialHtml : Error()
        object CookieWebDoesNotExist : Error()
        object CouldNotParseSessionValuesFromIndextHtml : Error()
    }
}