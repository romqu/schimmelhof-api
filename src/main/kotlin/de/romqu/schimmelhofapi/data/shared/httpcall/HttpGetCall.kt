package de.romqu.schimmelhofapi.data.shared.httpcall

import de.romqu.schimmelhofapi.COOKIE_HEADER
import okhttp3.Request
import org.springframework.stereotype.Component
import java.net.URL

interface HttpGetCall : HttpCall {
    fun createGetRequest(url: URL, httpCallRequest: HttpCallRequest): Request
}

@Component
class HttpGetCallDelegate(
    private val httpCallDelegate: HttpCallDelegate,
) : HttpGetCall, HttpCall by httpCallDelegate {

    override fun createGetRequest(url: URL, httpCallRequest: HttpCallRequest): Request =
        with(httpCallRequest) {
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
                .url(url)
                .get()
                .build()
        }

}