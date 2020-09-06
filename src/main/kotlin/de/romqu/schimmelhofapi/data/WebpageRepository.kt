package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.RequestSessionData
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Repository

@Repository
class WebpageRepository(
    private val httpClient: OkHttpClient,
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {

    fun getHomePage(): Result<HttpCall.Error, HttpCall.Response> {
        val request = Request.Builder().url(LOGIN_URL).get().build()

        return makeCall(request)
    }

    fun getIndexPage(
        requestSessionData: RequestSessionData
    ): Result<HttpCall.Error, HttpCall.Response> {

        val request = createGetRequest(INDEX_URL, requestSessionData)

        return makeCall(request)
    }
}