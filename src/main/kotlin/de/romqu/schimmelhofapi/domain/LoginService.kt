package de.romqu.schimmelhofapi.domain


import de.romqu.schimmelhofapi.INITIAL_URL
import de.romqu.schimmelhofapi.SET_COOKIE_HEADER
import de.romqu.schimmelhofapi.data.UserRepository
import de.romqu.schimmelhofapi.data.WebpageRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpCall
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapError
import okhttp3.Headers
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*


@Service
class LoginService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val getStateValuesFromHtmlDocumentTask: GetStateValuesFromHtmlDocumentTask,
    private val webpageRepository: WebpageRepository,
    private val getRidingLessonsTask: GetRidingLessonsTask,
) {

    class Out(
        val ridingLessonMap: Map<GetRidingLessonsTask.Weekday, List<GetRidingLessonsTask.RidingLessonTableEntry>>,
        val sessionEntity: SessionEntity
    )

    fun execute(username: String, passwordPlain: String) =
        webpageRepository.getHomePage()
            .getInitialCookie()
            .sanitizeCookie()
            .getHtmlDocumentFromBody()
            .getStateValuesFromInitialHtml()
            .doLogin(username, passwordPlain)
            .getCookieWebFromLoginHeaders()
            .sanitizeCookieWeb()
            .getHtmlDocumentFromIndexBody()
            .getSateValuesFromIndexHtml()
            .saveSession()
            .getRidingLessons()

    private fun Result<HttpCall.Error, HttpCall.Response>.getInitialCookie()
        : Result<Error, GetInitialCookieOut> =
        doOn({ response ->
            val setCookieHeaderValue = response.headers.getSetCookieValue()

            if (setCookieHeaderValue != null)
                Result.Success(
                    GetInitialCookieOut(
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

    class GetInitialCookieOut(val initialCookie: String, val responseBody: ResponseBody)

    private fun Result<Error, GetInitialCookieOut>.sanitizeCookie(): Result<Error, SanitizeCookieOut> =
        flatMap { getInitialCookieOut ->
            val sanitizedCookie = getInitialCookieOut.initialCookie.sanitizeCookie()

            if (sanitizedCookie != getInitialCookieOut.initialCookie)
                Result.Success(
                    SanitizeCookieOut(
                        sanitizedCookie,
                        getInitialCookieOut.responseBody
                    )
                )
            else Result.Failure(Error.CookieCouldNotBeSanitized)
        }

    class SanitizeCookieOut(val sanitizedCookie: String, val responseBody: ResponseBody)

    private fun Result<Error, SanitizeCookieOut>.getHtmlDocumentFromBody(): Result<Error, GetHtmlDocumentFromBodyOut> =
        flatMap { out ->
            try {
                val htmlDocument = out.responseBody.convertToDocument(INITIAL_URL)

                // TODO: improve
                out.responseBody.close()

                Result.Success(GetHtmlDocumentFromBodyOut(out.sanitizedCookie, htmlDocument))

            } catch (ex: IOException) {
                Result.Failure(Error.CouldNotParseResponseBody)
            }
        }

    class GetHtmlDocumentFromBodyOut(val sanitizedCookie: String, val htmlDocument: Document)

    private fun Result<Error, GetHtmlDocumentFromBodyOut>.getStateValuesFromInitialHtml()
        : Result<Error, GetSessionValuesFromHtmlOut> =
        flatMap { out ->
            getStateValuesFromHtmlDocumentTask.execute(out.htmlDocument)
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
        passwordPlain: String
    ): Result<Error, DoLoginOut> = flatMap { out ->
        userRepository.login(
            username = userName,
            password = passwordPlain,
            viewState = out.viewState,
            viewStateGenerator = out.viewStateGenerator,
            eventValidation = out.eventValidation,
            cookie = out.sanitizedCookie
        ).mapError(Error.Network) { loginHeaders ->
            DoLoginOut(
                sanitizedCookie = out.sanitizedCookie,
                viewState = out.viewState,
                viewStateGenerator = out.viewStateGenerator,
                eventValidation = out.eventValidation,
                loginHeaders = loginHeaders
            )
        }
    }

    class DoLoginOut(
        val sanitizedCookie: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
        val loginHeaders: Headers
    )


    private fun Result<Error, DoLoginOut>.getCookieWebFromLoginHeaders()
        : Result<Error, GetCookieWebFromLoginHeadersOut> = flatMap { out ->
        val setCookieWebHeaderValue = out.loginHeaders.getSetCookieValue()

        if (setCookieWebHeaderValue != null)
            Result.Success(
                GetCookieWebFromLoginHeadersOut(
                    sanitizedCookie = out.sanitizedCookie,
                    setCookieWebHeaderValue = setCookieWebHeaderValue,
                    viewState = out.viewState,
                    viewStateGenerator = out.viewStateGenerator,
                    eventValidation = out.eventValidation,
                )

            )
        else Result.Failure(Error.CookieWebDoesNotExist)

    }

    class GetCookieWebFromLoginHeadersOut(
        val sanitizedCookie: String,
        val setCookieWebHeaderValue: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
    )

    private fun Result<Error, GetCookieWebFromLoginHeadersOut>.sanitizeCookieWeb()
        : Result<Error, SanitizeCookieWebOut> =
        flatMap { out ->
            val sanitizedCookieWeb = out.setCookieWebHeaderValue.sanitizeCookie()

            if (sanitizedCookieWeb != out.setCookieWebHeaderValue)
                Result.Success(
                    SanitizeCookieWebOut(
                        sanitizedCookie = out.sanitizedCookie,
                        sanitizedCookieWeb = sanitizedCookieWeb,
                        viewState = out.viewState,
                        viewStateGenerator = out.viewStateGenerator,
                        eventValidation = out.eventValidation,
                    )
                )
            else Result.Failure(Error.CookieWebCouldNotBeSanitized)
        }

    class SanitizeCookieWebOut(
        val sanitizedCookie: String,
        val sanitizedCookieWeb: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
    )

    private fun Headers.getSetCookieValue(): String? = this[SET_COOKIE_HEADER]

    private fun String.sanitizeCookie(): String = substringBefore(";")

    private fun Result<Error, SanitizeCookieWebOut>.getHtmlDocumentFromIndexBody()
        : Result<Error, GetHtmlDocumentFromIndexBodyOut> =
        flatMap { out ->
            webpageRepository.getIndexPage(
                cookie = out.sanitizedCookie,
                cookieWeb = out.sanitizedCookieWeb
            ).mapError(Error.CouldNotParseIndexResponseBody) { body ->
                val indexHtmlDocument = body.convertToDocument(INDEX_URL)

                GetHtmlDocumentFromIndexBodyOut(
                    sanitizedCookie = out.sanitizedCookie,
                    sanitizedCookieWeb = out.sanitizedCookieWeb,
                    viewState = out.viewState,
                    viewStateGenerator = out.viewStateGenerator,
                    eventValidation = out.eventValidation,
                    indexHtmlDocument = indexHtmlDocument
                )
            }
        }

    class GetHtmlDocumentFromIndexBodyOut(
        val sanitizedCookie: String,
        val sanitizedCookieWeb: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
        val indexHtmlDocument: Document
    )

    private fun Result<Error, GetHtmlDocumentFromIndexBodyOut>.getSateValuesFromIndexHtml()
        : Result<Error, GetSateValuesFromIndexHtmlOut> =
        flatMap { out ->
            getStateValuesFromHtmlDocumentTask.execute(out.indexHtmlDocument)
                .mapError(Error.CouldNotParseSessionValuesFromIndextHtml) { stateValuesOut ->
                    GetSateValuesFromIndexHtmlOut(
                        sanitizedCookie = out.sanitizedCookie,
                        sanitizedCookieWeb = out.sanitizedCookieWeb,
                        viewState = stateValuesOut.viewState,
                        viewStateGenerator = stateValuesOut.viewStateGenerator,
                        eventValidation = stateValuesOut.eventValidation,
                    )
                }
        }

    class GetSateValuesFromIndexHtmlOut(
        val sanitizedCookie: String,
        val sanitizedCookieWeb: String,
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
    )

    private fun Result<Error, GetSateValuesFromIndexHtmlOut>.saveSession(): Result<Error, SessionEntity> =
        map { out ->
            val session = SessionEntity(
                uuid = UUID.randomUUID(),
                cookie = out.sanitizedCookie,
                cookieWeb = out.sanitizedCookieWeb,
                viewState = out.viewState,
                viewStateGenerator = out.viewStateGenerator,
                eventValidation = out.eventValidation,
            )

            sessionRepository.saveOrUpdate(session)
        }

    private fun Result<Error, SessionEntity>.getRidingLessons() = flatMap { session ->
        getRidingLessonsTask.execute(session.uuid)
            .mapError(Error.CouldNotGetRidingLessons) {
                Out(it, session)
            }
    }

    private fun ResponseBody.convertToDocument(url: String) = Jsoup.parse(
        byteStream(),
        Charsets.UTF_8.name(),
        url
    )

    sealed class Error {
        class Network(val message: String = "", val statusCode: Int = -1) : Error()
        object CookieDoesNotExist : Error()
        object CookieCouldNotBeSanitized : Error()
        object CouldNotParseResponseBody : Error()
        object CouldNotParseSessionValuesFromInitialHtml : Error()
        object CookieWebDoesNotExist : Error()
        object CookieWebCouldNotBeSanitized : Error()
        object CouldNotParseIndexResponseBody : Error()
        object CouldNotParseSessionValuesFromIndextHtml : Error()
        object CouldNotGetRidingLessons : Error()
    }

}