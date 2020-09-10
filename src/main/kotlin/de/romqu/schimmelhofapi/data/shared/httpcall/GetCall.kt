package de.romqu.schimmelhofapi.data.shared.httpcall

import de.romqu.schimmelhofapi.COOKIE_HEADER
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import okhttp3.Request
import org.springframework.stereotype.Component
import java.net.URL

interface GetCall {
    fun createGetRequest(url: URL, httpCallRequestData: HttpCallRequestData): Request
}

@Component
class GetCallDelegate : GetCall {

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