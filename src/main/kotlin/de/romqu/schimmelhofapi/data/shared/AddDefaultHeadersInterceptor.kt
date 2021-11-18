package de.romqu.schimmelhofapi.data.shared

import okhttp3.Interceptor
import okhttp3.Response


class AddDefaultHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:96.0) Gecko/20100101 Firefox/96.0")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
            .header("Accept-Encoding", "gzip, deflate")
            .header("Dnt", "1")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Sec-Gpc", "1")
            .header("Te", "trailers")
            .build()

        return chain.proceed(newRequest)
    }
}