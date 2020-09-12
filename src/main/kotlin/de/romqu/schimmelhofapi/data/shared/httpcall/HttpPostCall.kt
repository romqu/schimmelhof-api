package de.romqu.schimmelhofapi.data.shared.httpcall

import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.data.shared.constant.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Component
import java.net.URL

interface HttpPostCall {
    fun createPostRequest(
        url: URL,
        addToRequestBody: String,
        httpCallRequest: HttpCallRequest
    ): Request
}

@Component
class HttpPostCallDelegate : HttpPostCall {

    override fun createPostRequest(
        url: URL,
        addToRequestBody: String,
        httpCallRequest: HttpCallRequest
    ): Request = with(httpCallRequest) {

        Request.Builder()
            .url(url)
            .addHeader(COOKIE_HEADER, """$cookie; $cookieWeb""")
            .addHeader(HEADER_CONTENT_TYPE, MIME_X_WWW_FORM_URLENCODED)
            .post(
                ("$REQUEST_EVENT_TARGET_KEY$eventTarget" +
                    "$REQUEST_EVENT_ARGUMENT_KEY$eventArgument" +
                    REQUEST_LAST_FOCUS_KEY +
                    "$REQUEST_VIEW_STATE_KEY$viewState" +
                    "$REQUEST_VIEW_STATE_GENERATOR_KEY$viewStateGenerator" +
                    "$REQUEST_EVENT_VALIDATION_KEY$eventValidationEncoded" +
                    addToRequestBody)
                    .toRequestBody(MIME_X_WWW_FORM_URLENCODED.toMediaType())
            ).build()
    }

}