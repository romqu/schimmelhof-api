package de.romqu.schimmelhofapi.data.shared.httpcall

import de.romqu.schimmelhofapi.shared.Result
import okhttp3.*
import org.springframework.stereotype.Component
import java.io.IOException

interface HttpCall {

    fun makeCall(request: Request): Result<Error, Response>

    fun <F : Error, S : Any> makeCall(
        request: Request,
        withResponse: (
            response: okhttp3.Response, responseBody: ResponseBody,
        ) -> Result<F, S>
    ): Result<Error, S>

    fun makeNoBodyCall(request: Request): Result<Error, Headers>

    sealed class Error {
        class ResponseUnsuccessful(val statusCode: Int, statusMessage: String) : Error()
        class CallUnsuccessful(val message: String) : Error()
    }

    class Response(
        val headers: Headers,
        val responseBody: ResponseBody
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
}
