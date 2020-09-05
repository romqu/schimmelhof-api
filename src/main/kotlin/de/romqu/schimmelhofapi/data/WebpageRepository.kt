package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.RequestSessionData
import okhttp3.OkHttpClient
import org.springframework.stereotype.Repository

@Repository
class WebpageRepository(
    private val httpClient: OkHttpClient,
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {
    fun getBody(
        requestSessionData: RequestSessionData
    ): Result<HttpCall.Error, HttpCall.Response> {

        val request = createGetRequest(requestSessionData)

        return makeCall(request)
    }
}