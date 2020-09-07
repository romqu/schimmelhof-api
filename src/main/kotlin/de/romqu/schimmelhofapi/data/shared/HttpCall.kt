package de.romqu.schimmelhofapi.data.shared

import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.data.shared.constant.*
import de.romqu.schimmelhofapi.shared.Result
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URL

interface HttpCall {

    fun makeCall(request: Request): Result<Error, Response>

    fun <F : Error, S : Any> makeCall(
        request: Request,
        withResponse: (
            response: okhttp3.Response, responseBody: ResponseBody,
        ) -> Result<F, S>
    ): Result<Error, S>

    fun makeNoBodyCall(request: Request): Result<Error, Headers>

    fun createPostRequest(
        url: URL,
        addToRequestBody: String,
        httpCallRequestData: HttpCallRequestData
    ): Request

    fun createGetRequest(url: URL, httpCallRequestData: HttpCallRequestData): Request

    sealed class Error {
        class ResponseUnsuccessful(val statusCode: Int, statusMessage: String) : Error()
        class CallUnsuccessful(val message: String) : Error()
    }

    class Response(
        headers: Headers,
        responseBody: ResponseBody
    )
}

@Component
class HttpCallDelegate(private val httpClient: OkHttpClient) : HttpCall {

    override fun makeCall(request: Request): Result<HttpCall.Error, HttpCall.Response> {
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body

            if (response.isSuccessful && responseBody != null) {
                Result.Success(HttpCall.Response(response.headers, responseBody))
            } else {
                Result.Failure(HttpCall.Error.ResponseUnsuccessful(response.code, response.message))
            }

        } catch (ex: IOException) {
            Result.Failure(HttpCall.Error.CallUnsuccessful(ex.toString()))
        }
    }

    override fun <F : HttpCall.Error, S : Any> makeCall(
        request: Request,
        withResponse: (response: Response, responseBody: ResponseBody) -> Result<F, S>
    ): Result<HttpCall.Error, S> = try {
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body
        if (responseBody != null) {
            withResponse(response, responseBody)
        } else Result.Failure(HttpCall.Error.ResponseUnsuccessful(
            response.code,
            response.message
        ))
    } catch (ex: IOException) {
        Result.Failure(HttpCall.Error.CallUnsuccessful(ex.toString()))
    }

    override fun makeNoBodyCall(request: Request): Result<HttpCall.Error, Headers> {
        return try {
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                Result.Success(response.headers)
            } else {
                Result.Failure(HttpCall.Error.ResponseUnsuccessful(response.code, response.message))
            }

        } catch (ex: IOException) {
            Result.Failure(HttpCall.Error.CallUnsuccessful(ex.toString()))
        }
    }

    override fun createPostRequest(
        url: URL,
        addToRequestBody: String,
        httpCallRequestData: HttpCallRequestData
    ): Request = with(httpCallRequestData) {

        Request.Builder()
            .url(url)
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .addHeader(HEADER_CONTENT_TYPE, MIME_X_WWW_FORM_URLENCODED)
            .post(
                (REQUEST_EVENT_TARGET_KEY +
                    "${REQUEST_EVENT_ARGUMENT_KEY}$eventArgument" +
                    REQUEST_LAST_FOCUS_KEY +
                    "${REQUEST_VIEW_STATE_KEY}$viewState" +
                    "${REQUEST_VIEW_STATE_GENERATOR_KEY}$viewStateGenerator" +
                    "${REQUEST_EVENT_VALIDATION_KEY}$eventValidationEncoded" +
                    addToRequestBody)
                    .toRequestBody(MIME_X_WWW_FORM_URLENCODED.toMediaType())
            ).build()
    }

    override fun createGetRequest(url: URL, httpCallRequestData: HttpCallRequestData): Request =
        with(httpCallRequestData) {
            val builder = Request.Builder()
            val builderHeaderStep =
                if (cookie.isNotEmpty()) {
                    if (cookieWeb.isNotEmpty()) {
                        builder.addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
                    } else {
                        builder.addHeader(COOKIE_HEADER, cookie)
                    }
                } else builder

            builderHeaderStep
                .url(INDEX_URL)
                .get()
                .build()
        }
}
