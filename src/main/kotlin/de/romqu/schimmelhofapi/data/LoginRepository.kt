package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.Constant.HEADER_CONTENT_TYPE
import de.romqu.schimmelhofapi.data.shared.Constant.MIME_X_WWW_FORM_URLENCODED
import de.romqu.schimmelhofapi.data.shared.Constant.REQUEST_EVENT_ARGUMENT_KEY
import de.romqu.schimmelhofapi.data.shared.Constant.REQUEST_EVENT_TARGET_KEY
import de.romqu.schimmelhofapi.data.shared.Constant.REQUEST_EVENT_VALIDATION_KEY
import de.romqu.schimmelhofapi.data.shared.Constant.REQUEST_VIEW_STATE_GENERATOR_KEY
import de.romqu.schimmelhofapi.data.shared.Constant.REQUEST_VIEW_STATE_KEY
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.LOGIN_URL
import de.romqu.schimmelhofapi.data.shared.RequestSessionValue
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Repository
import java.net.URL

@Repository
class LoginRepository(
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {


    fun getInitialResponse(): Result<HttpCall.Error, HttpCall.Response> {
        val request = Request.Builder().url(LOGIN_URL).get().build()

        return makeCall(request) { response ->
            if (response.code == 302) {
                Result.Success(HttpCall.Response(response.headers, response.body))
            } else {
                Res
            }
        }
    }

    fun getLoginHeadersResponse(
        username: String,
        password: String,
        requestSessionValue: RequestSessionValue
    ): Result<HttpCall.Error, Headers> {

        val request = with(requestSessionValue) {
            createPostRequest(
                url = LOGIN_URL,
                addToRequestBody = "",
                viewStateValue = viewState,
                viewStateGeneratorValue = viewStateGenerator,
                eventValidationValue = eventValidationEncoded,
                cookieValue = cookie
            )
        }

        return makeCallNullableBody(request)
    }


    fun createPostRequest(
        url: URL,
        addToRequestBody: String,
        viewStateValue: String,
        viewStateGeneratorValue: String,
        eventValidationValue: String,
        cookieValue: String
    ): Request = Request.Builder()
        .url(url)
        .addHeader(COOKIE_HEADER, cookieValue)
        .addHeader(HEADER_CONTENT_TYPE, MIME_X_WWW_FORM_URLENCODED)
        .post(
            (REQUEST_EVENT_TARGET_KEY +
                REQUEST_EVENT_ARGUMENT_KEY +
                "$REQUEST_VIEW_STATE_KEY$viewStateValue" +
                "$REQUEST_VIEW_STATE_GENERATOR_KEY$viewStateGeneratorValue" +
                "$REQUEST_EVENT_VALIDATION_KEY$eventValidationValue" +
                addToRequestBody)
                .toRequestBody(MIME_X_WWW_FORM_URLENCODED.toMediaType())
        ).build()
}


