package de.romqu.schimmelhofapi.data.config

import de.romqu.schimmelhofapi.data.shared.AddDefaultHeadersInterceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Configuration
class OkhttpConfig {

    @Bean
    fun getHttpClient(): OkHttpClient = getUnsafeOkHttpClient()

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            //.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(8081)))
            .followRedirects(false)
            .followSslRedirects(false)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(AddDefaultHeadersInterceptor())
            .build()
    }
}