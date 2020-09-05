package de.romqu.schimmelhofapi.data.shared

import de.romqu.schimmelhofapi.core.Result
import okhttp3.*
import org.springframework.stereotype.Component
import java.io.IOException

interface HttpCall {

    fun makeCall(request: Request): Result<Error, Response>

    fun makeCallNullableBody(request: Request): Result<Error, Headers>

    fun <F : Error, S : Any> makeCall(
        request: Request,
        withResponse: (response: okhttp3.Response) -> Result<F, S>
    ): Result<Error, S>

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

    override fun makeCallNullableBody(request: Request): Result<HttpCall.Error, Headers> {
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

    override fun <F : HttpCall.Error, S : Any> makeCall(
        request: Request,
        withResponse: (response: Response) -> Result<F, S>
    ): Result<HttpCall.Error, S> = try {
        val response = httpClient.newCall(request).execute()
        withResponse(response)
    } catch (ex: IOException) {
        Result.Failure(HttpCall.Error.CallUnsuccessful(ex.toString()))
    }
}