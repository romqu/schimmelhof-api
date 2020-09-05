package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.INDEX_URL
import de.romqu.schimmelhofapi.core.Result
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.springframework.stereotype.Repository
import java.io.IOException

@Repository
class WebpageRepository(
    private val httpClient: OkHttpClient
) {
    fun getBody(
        cookie: String,
        cookieWeb: String,
    ): Result<Error, ResponseBody> {

        val request = Request.Builder()
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .url(INDEX_URL)
            .get()
            .build()

        return try {
            val response = httpClient.newCall(request).execute()

            val body = response.body

            if (response.isSuccessful && body != null) {
                Result.Success(body)
            } else {
                response.close()
                Result.Failure(Error.CallUnsuccessfulOrBodyIsNull)
            }
        } catch (ex: IOException) {
            Result.Failure(Error.Network)
        }
    }

    sealed class Error {
        object CallUnsuccessfulOrBodyIsNull : Error()
        object Network : Error()
    }
}