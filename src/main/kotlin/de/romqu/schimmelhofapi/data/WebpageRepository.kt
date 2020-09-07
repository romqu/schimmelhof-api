package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.HttpCallRequestData
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import de.romqu.schimmelhofapi.shared.Result
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
        sessionEntity: SessionEntity
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequestData(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation
            )
        }

        val request = createGetRequest(INDEX_URL, requestData)

        return makeCall(request)
    }
}